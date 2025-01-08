import os
import re
import logging
from typing import Tuple, List, Dict, Optional

import cv2
import numpy as np
import requests
import yaml
from rapidfuzz import fuzz
from django.conf import settings

logger = logging.getLogger(__name__)

# Türk alfabesindeki 29 harf (Cevap seçeneklerinde ve diğer alanlarda kullanılıyor)
TURKISH_LETTERS = [
    'A', 'B', 'C', 'Ç', 'D', 'E', 'F', 'G', 'Ğ', 'H',
    'I', 'İ', 'J', 'K', 'L', 'M', 'N', 'O', 'Ö', 'P',
    'R', 'S', 'Ş', 'T', 'U', 'Ü', 'V', 'Y', 'Z'
]


def load_config(config_path: str) -> Dict:
    """
    YAML formatındaki konfigürasyon dosyasını yükler.
    """
    try:
        with open(config_path, 'r', encoding='utf-8') as file:
            config = yaml.safe_load(file)
        logger.debug(f"Konfigürasyon dosyası yüklendi: {config_path}")
        return config
    except FileNotFoundError:
        logger.error(f"Konfigürasyon dosyası bulunamadı: {config_path}")
        raise
    except yaml.YAMLError as e:
        logger.error(f"YAML dosyası okunurken hata oluştu: {e}")
        raise


def setup_logging(config: Dict):
    """
    Logging yapılandırmasını ayarlar.
    """
    try:
        log_level = getattr(logging, config['logging']['level'].upper(), logging.INFO)
        log_format = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')

        logger = logging.getLogger()
        logger.setLevel(log_level)

        # Mevcut handler'ları temizle
        if logger.hasHandlers():
            logger.handlers.clear()

        # Dosya handler'ı ekle
        log_file_path = os.path.join(settings.BASE_DIR, config['logging']['log_file'])
        os.makedirs(os.path.dirname(log_file_path), exist_ok=True)
        file_handler = logging.FileHandler(log_file_path)
        file_handler.setFormatter(log_format)
        logger.addHandler(file_handler)

        # Konsol handler'ı ekle
        stream_handler = logging.StreamHandler()
        stream_handler.setFormatter(log_format)
        logger.addHandler(stream_handler)

        logger.debug("Logging yapılandırması başarıyla ayarlandı.")
    except KeyError as e:
        logger.error(f"Logging yapılandırmasında eksik anahtar: {e}")
        raise
    except Exception as e:
        logger.error(f"Logging yapılandırılırken hata oluştu: {e}")
        raise


def resize_image(image: np.ndarray, max_width: int, max_height: int) -> np.ndarray:
    height, width = image.shape[:2]
    scaling_factor = min(max_width / width, max_height / height, 1.0)
    if scaling_factor < 1.0:
        new_size = (int(width * scaling_factor), int(height * scaling_factor))
        resized_image = cv2.resize(image, new_size, interpolation=cv2.INTER_AREA)
        logger.debug(f"Görüntü yeniden boyutlandırıldı: {new_size}")
    else:
        resized_image = image
        logger.debug("Görüntü yeniden boyutlandırılmadı (maksimum boyutlara uygun).")
    return resized_image


def crop_borders(image: np.ndarray) -> np.ndarray:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    _, thresh = cv2.threshold(gray, 1, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if contours:
        largest_contour = max(contours, key=cv2.contourArea)
        x, y, w, h = cv2.boundingRect(largest_contour)
        cropped_image = image[y:y + h, x:x + w]
        logger.debug(f"Görüntü kenarları kırpıldı: x={x}, y={y}, w={w}, h={h}")
        return cropped_image
    logger.debug("Kırpılacak kontur bulunamadı. Görüntü değiştirilmedi.")
    return image


def order_points(pts: np.ndarray) -> np.ndarray:
    rect = np.zeros((4, 2), dtype="float32")
    s = pts.sum(axis=1)
    diff = np.diff(pts, axis=1)
    rect[0] = pts[np.argmin(s)]  # Sol üst
    rect[2] = pts[np.argmax(s)]  # Sağ alt
    rect[1] = pts[np.argmin(diff)]  # Sağ üst
    rect[3] = pts[np.argmax(diff)]  # Sol alt
    logger.debug(f"Köşe noktaları sıralandı: {rect}")
    return rect


def four_point_transform(image: np.ndarray, pts: np.ndarray) -> np.ndarray:
    rect = order_points(pts)
    (tl, tr, br, bl) = rect

    width_a = np.linalg.norm(br - bl)
    width_b = np.linalg.norm(tr - tl)
    max_width = max(int(width_a), int(width_b))

    height_a = np.linalg.norm(tr - br)
    height_b = np.linalg.norm(tl - bl)
    max_height = max(int(height_a), int(height_b))

    dst = np.array([
        [0, 0],
        [max_width - 1, 0],
        [max_width - 1, max_height - 1],
        [0, max_height - 1]
    ], dtype="float32")

    M = cv2.getPerspectiveTransform(rect, dst)
    warped = cv2.warpPerspective(image, M, (max_width, max_height))
    logger.debug("Perspektif dönüşümü uygulandı.")
    return warped


def find_document_contour(image: np.ndarray) -> Optional[np.ndarray]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edged = cv2.Canny(blurred, 50, 150)
    contours, _ = cv2.findContours(edged, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)

    for contour in contours:
        peri = cv2.arcLength(contour, True)
        approx = cv2.approxPolyDP(contour, 0.02 * peri, True)
        if len(approx) == 4:
            logger.debug("Belge konturu bulundu.")
            return approx
    logger.warning("Belge konturu bulunamadı.")
    return None


def deskew_image(image: np.ndarray, config: Dict) -> np.ndarray:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, tuple(config['deskew']['gaussian_blur_kernel']), 0)
    edges = cv2.Canny(blurred, config['deskew']['canny_threshold1'], config['deskew']['canny_threshold2'])
    lines = cv2.HoughLines(edges, 1, np.pi / 180, config['deskew']['hough_threshold'])

    angles = []
    if lines is not None:
        for line in lines:
            rho, theta = line[0]
            angle = (theta * 180 / np.pi) - 90
            if config['deskew']['median_angle_range'][0] < angle < config['deskew']['median_angle_range'][1]:
                angles.append(angle)
        logger.debug(f"Bulunan açılar: {angles}")

    if angles:
        median_angle = np.median(angles)
        if abs(median_angle) > config['deskew']['min_angle_threshold']:
            (h, w) = image.shape[:2]
            center = (w // 2, h // 2)
            M = cv2.getRotationMatrix2D(center, median_angle, 1.0)
            deskewed = cv2.warpAffine(image, M, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
            logger.info(f"Görüntü {median_angle} derece döndürüldü.")
            return deskewed

    logger.info("Deskew işlemi uygulanmadı.")
    return image


def preprocess_image(image: np.ndarray, config: Dict) -> Tuple[Optional[np.ndarray], Optional[np.ndarray]]:
    deskewed = deskew_image(image, config)
    if deskewed is None:
        logger.error("Görüntü deskew edilemedi.")
        return None, None

    gray = cv2.cvtColor(deskewed, cv2.COLOR_BGR2GRAY)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced_gray = clahe.apply(gray)

    blurred = cv2.medianBlur(enhanced_gray, 3)
    kernel = np.ones(tuple(config['preprocess']['morph_kernel_size']), np.uint8)
    morphed = cv2.morphologyEx(blurred, cv2.MORPH_OPEN, kernel, iterations=1)

    adaptive_thresh = cv2.adaptiveThreshold(
        morphed,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY_INV,
        config['preprocess']['adaptive_thresh_block_size'],
        config['preprocess']['adaptive_thresh_C']
    )

    logger.debug("Ön işleme adımları tamamlandı.")
    if config['output']['save_debug_images']:
        debug_dir = os.path.join(config['output']['debug_images_directory'])
        os.makedirs(debug_dir, exist_ok=True)
        cv2.imwrite(os.path.join(debug_dir, 'thresholded_image.jpg'), adaptive_thresh)

    return deskewed, adaptive_thresh


def perform_ocr_space(image: np.ndarray, config: Dict) -> Dict:
    """
    OCR.space API kullanarak görüntüden metin çıkarır ve tüm çıktıları loglar.
    """
    try:
        api_key = getattr(settings, 'OCR_SPACE_API_KEY', None)
        if not api_key:
            raise ValueError("OCR_SPACE_API_KEY ayarı tanımlanmamış.")

        language = config['ocr']['language']
        detect_orientation = config['ocr']['detect_orientation']
        url = 'https://apipro1.ocr.space/parse/image'

        _, img_encoded = cv2.imencode('.jpg', image)
        files = {'file': ('image.jpg', img_encoded.tobytes(), 'image/jpeg')}
        payload = {
            'isOverlayRequired': True,
            'apikey': api_key,
            'language': language,
            'detectOrientation': detect_orientation
        }

        response = requests.post(url, files=files, data=payload)
        result = response.json()

        if result.get('IsErroredOnProcessing'):
            error_message = result.get('ErrorMessage', ['Unknown error'])[0]
            raise Exception(f"OCR.space API Hatası: {error_message}")

        parsed_results = result.get('ParsedResults', [])
        if not parsed_results:
            return {"full_text": "", "detailed_texts": []}

        parsed_text = parsed_results[0].get('ParsedText', "")
        text_overlay = parsed_results[0].get('TextOverlay', {})
        lines = text_overlay.get('Lines', [])
        detailed_texts = []

        for line in lines:
            line_text = line.get('LineText', '').strip()
            words = line.get('Words', [])
            word_details = []
            for word in words:
                word_text = word.get('WordText', '').strip()
                left = word.get('Left', 0)
                top = word.get('Top', 0)
                height = word.get('Height', 0)
                width = word.get('Width', 0)
                word_details.append({
                    "WordText": word_text,
                    "Left": left,
                    "Top": top,
                    "Height": height,
                    "Width": width
                })
            if word_details:
                bounding_box = []
                for wd in word_details:
                    bounding_box.append((wd['Left'], wd['Top']))
                    bounding_box.append((wd['Left'] + wd['Width'], wd['Top'] + wd['Height']))
                bounding_box = sorted(list(set(bounding_box)), key=lambda x: (x[1], x[0]))
                detailed_texts.append({
                    "description": line_text,
                    "bounding_box": bounding_box
                })

        logger.debug("OCR işlemi başarılı.")
        return {
            "full_text": parsed_text,
            "detailed_texts": detailed_texts
        }
    except Exception as e:
        logger.error(f"OCR işlemi sırasında hata: {e}")
        return {"full_text": "", "detailed_texts": []}


def normalize_text(text: str) -> str:
    """
    Metni normalize eder: büyük harfe çevirir ve sadece alfanümerik karakterleri bırakır.
    """
    text = text.upper()
    text = re.sub(r'[^A-ZÇĞİÖŞÜ0-9\s\-]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()
    logger.debug(f"Normalize edilmiş metin: {text}")
    return text


def find_heading_coordinates(
    image: np.ndarray,
    heading_texts: List[str],
    config: Dict,
    area_config: Dict
) -> Optional[List[List[int]]]:
    """
    Belirli bir başlık metninin koordinatlarını bulur. Birden fazla başlık metni destekler.
    """
    for heading_text in heading_texts:
        heading_text_normalized = normalize_text(heading_text)
        min_text_length = area_config.get('min_text_length', 3)
        max_text_length = area_config.get('max_text_length', 100)
        ocr_result = perform_ocr_space(image, config)
        texts = ocr_result.get('detailed_texts', [])
        highest_similarity = 0
        best_match_coords = None
        best_match_text = ''

        for text_item in texts:
            text = text_item['description'].strip()
            if not text:
                continue
            text_length = len(text)
            if text_length < min_text_length or text_length > max_text_length:
                continue
            text_normalized = normalize_text(text)
            similarity = fuzz.ratio(heading_text_normalized, text_normalized)
            if similarity > highest_similarity:
                highest_similarity = similarity
                bounding_box = text_item['bounding_box']
                x_coords = [point[0] for point in bounding_box]
                y_coords = [point[1] for point in bounding_box]
                x_start = min(x_coords)
                y_start = min(y_coords)
                x_end = max(x_coords)
                y_end = max(y_coords)
                best_match_coords = [[x_start, y_start], [x_end, y_end]]
                best_match_text = text
                logger.debug(f"Metin benzerliği: '{text}' (%{similarity})")

            if similarity >= config['ocr']['similarity_threshold']:
                extra_width = area_config.get('extra_width', 0)
                extra_height = area_config.get('extra_height', 0)
                offset_x = area_config.get('offset_x', 0)
                offset_y = area_config.get('offset_y', 0)
                width = area_config.get('width', x_end - x_start)
                height = area_config.get('height', y_end - y_start)
                x_start = x_start + offset_x
                x_end = x_start + width + extra_width
                y_start = y_start + offset_y
                y_end = y_start + height + extra_height
                x_start = int(round(max(0, x_start)))
                x_end = int(round(min(image.shape[1], x_end)))
                y_start = int(round(max(0, y_start)))
                y_end = int(round(min(image.shape[0], y_end)))
                logger.debug(f"ROI Koordinatları: [{x_start}, {y_start}], [{x_end}, {y_end}]")
                return [[x_start, y_start], [x_end, y_end]]

        if best_match_coords is not None:
            logger.warning(
                f"En yüksek benzerlik ({highest_similarity}%) ile bulunan metin '{best_match_text}', ancak eşik değerin altında."
            )
        else:
            logger.error(f"Başlık '{heading_text}' bulunamadı.")

    return None


def extract_roi(
    thresh: np.ndarray,
    coordinates: List[List[int]],
    name: str,
    config: Dict
) -> Optional[np.ndarray]:
    """
    Belirli koordinatlarda ROI (Bölge) çıkarır.
    """
    try:
        (x_start, y_start), (x_end, y_end) = coordinates
        h, w = thresh.shape[:2]
        x_start = int(round(max(0, x_start)))
        y_start = int(round(max(0, y_start)))
        x_end = int(round(min(w, x_end)))
        y_end = int(round(min(h, y_end)))

        if x_end <= x_start or y_end <= y_start:
            logger.error(f"Geçersiz koordinatlar: {coordinates}")
            return None

        roi = thresh[y_start:y_end, x_start:x_end]
        if len(roi.shape) == 3:
            roi = cv2.cvtColor(roi, cv2.COLOR_BGR2GRAY)

        if config['output']['save_rois']:
            rois_dir = os.path.join(settings.BASE_DIR, config['output']['rois_directory'])
            os.makedirs(rois_dir, exist_ok=True)
            roi_path = os.path.join(rois_dir, f"{name}.jpg")
            cv2.imwrite(roi_path, roi)
            logger.debug(f"ROI kaydedildi: {roi_path}")

        logger.debug(f"ROI çıkarıldı: {name}")
        return roi
    except Exception as e:
        logger.error(f"ROI çıkarılırken hata: {e}")
        return None


def detect_filled_option(area: np.ndarray, num_choices: int, threshold: float) -> int:
    """
    Bir alanda hangi seçeneğin işaretlendiğini tespit eder.
    """
    h, w = area.shape[:2]
    if h == 0 or w == 0:
        logger.warning("Boş alan tespit edildi.")
        return -1

    choice_width = w // num_choices
    padding = int(choice_width * 0.05)

    choice_fill_ratios = []
    filled_option = -1
    max_fill_ratio = 0

    for i in range(num_choices):
        x_start = max(0, i * choice_width + padding)
        x_end = min(w, (i + 1) * choice_width - padding)
        choice_area = area[:, x_start:x_end]

        choice_area = cv2.GaussianBlur(choice_area, (5, 5), 0)
        _, choice_thresh = cv2.threshold(choice_area, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        choice_thresh = cv2.morphologyEx(choice_thresh, cv2.MORPH_CLOSE, kernel)

        filled_pixels = cv2.countNonZero(choice_thresh)
        total_pixels = choice_thresh.shape[0] * choice_thresh.shape[1]
        fill_ratio = filled_pixels / total_pixels if total_pixels > 0 else 0
        choice_fill_ratios.append(fill_ratio)
        logger.debug(f"Seçenek {i + 1}: Doluluk oranı = {fill_ratio:.3f}")

        if fill_ratio > max_fill_ratio:
            max_fill_ratio = fill_ratio
            filled_option = i

    logger.debug(f"Seçenek doluluk oranları: {choice_fill_ratios}")
    logger.debug(f"En yüksek doluluk oranı: {max_fill_ratio:.3f}")

    # Eşik değerini geçmezse -1 döndür
    if max_fill_ratio >= threshold:
        logger.debug(f"İşaretlenen seçenek: {filled_option + 1} (Doluluk oranı: {max_fill_ratio:.3f})")
        return filled_option
    else:
        logger.debug(f"Hiçbir seçenek yeterince dolu değil (Eşik: {threshold}, Maksimum: {max_fill_ratio:.3f})")
        return -1


def extract_answers(
    thresh: np.ndarray,
    answer_coords: List[List[int]],
    config: Dict
) -> Dict[str, Dict[str, Optional[str]]]:
    """
    Cevap alanını işleyerek cevapları çıkarır.
    """
    answer_area = extract_roi(thresh, answer_coords, "answer_area", config)
    if answer_area is None:
        logger.error("Cevap alanı çıkarılamadı.")
        return {}

    answers = {}
    num_columns = config['extract_answers']['num_columns']
    num_questions = config['extract_answers']['num_questions']
    num_choices = config['extract_answers']['num_choices']
    threshold = config['extract_answers']['threshold']

    h, w = answer_area.shape[:2]
    question_height = h // num_questions
    column_width = w // num_columns

    for col in range(num_columns):
        column_number = str(col + 1)
        answers[column_number] = {}

        x_start = col * column_width
        x_end = (col + 1) * column_width

        column_area = answer_area[:, x_start:x_end]

        for q in range(num_questions):
            question_number = str(q + 1)
            y_start = q * question_height
            y_end = (q + 1) * question_height

            question_area = column_area[y_start:y_end, :]

            filled_choice = detect_filled_option(question_area, num_choices, threshold)

            if filled_choice != -1:
                answers[column_number][question_number] = chr(65 + filled_choice)  # 'A' ASCII 65
                logger.debug(f"Cevap: Sütun {column_number}, Soru {question_number} - Seçenek {chr(65 + filled_choice)}")
            else:
                answers[column_number][question_number] = None
                logger.debug(f"Cevap: Sütun {column_number}, Soru {question_number} - Belirsiz")

    logger.info("Cevaplar başarıyla çıkarıldı.")
    return answers


def extract_student_number(image: np.ndarray, config: Dict) -> str:
    """
    Öğrenci numarası alanından işaretlenen rakamları çıkarır.
    """
    try:
        if len(image.shape) == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

        h, w = image.shape[:2]

        num_digits = config['extract_student_number']['num_digits']
        num_options = config['extract_student_number']['num_options']
        threshold = config['extract_student_number']['threshold']
        student_number = []

        digit_width = w // num_digits

        for i in range(num_digits):
            x_start = i * digit_width
            x_end = (i + 1) * digit_width if i < num_digits - 1 else w
            digit_area = image[:, x_start:x_end]
            option_height = digit_area.shape[0] // num_options
            max_fill_ratio = 0
            detected_option = None

            for j in range(num_options):
                y_start = j * option_height
                y_end = (j + 1) * option_height if j < num_options - 1 else digit_area.shape[0]
                option_area = digit_area[y_start:y_end, :]

                # Daha hassas bir doluluk tespiti için ek ön işleme
                option_area = cv2.GaussianBlur(option_area, (5, 5), 0)
                option_area = cv2.threshold(option_area, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]

                filled_pixels = cv2.countNonZero(option_area)
                total_pixels = option_area.shape[0] * option_area.shape[1]
                fill_ratio = filled_pixels / total_pixels if total_pixels > 0 else 0
                if fill_ratio > max_fill_ratio:
                    max_fill_ratio = fill_ratio
                    detected_option = j
                logger.debug(f"Öğrenci Numarası Sütun {i + 1}, Seçenek {j}: Doluluk = {fill_ratio:.2f}")

            if max_fill_ratio >= threshold and detected_option is not None:
                student_number.append(str(detected_option))
                logger.debug(f"Öğrenci Numarası Sütun {i + 1}: Rakam {detected_option} algılandı.")
            else:
                student_number.append("-")
                logger.warning(f"Öğrenci Numarası Sütun {i + 1}: Doluluk oranı {max_fill_ratio:.2f}, eşik altında.")

        student_number_str = ''.join(student_number)
        logger.info(f"Çıkarılan Öğrenci Numarası: {student_number_str}")
        return student_number_str
    except Exception as e:
        logger.error(f"Öğrenci numarası çıkarılırken hata: {e}")
        return "Unknown"


def extract_test_group(
    thresh: np.ndarray,
    test_group_coords: List[List[int]],
    config: Dict
) -> Optional[str]:
    """
    Test grubu alanından işaretlenen grup harfini çıkarır.
    """
    try:
        logger.info("Test grubu çıkarma işlemi başlatılıyor.")
        test_group_area = extract_roi(thresh, test_group_coords, "test_group_area", config)
        if test_group_area is None:
            logger.error("Test grubu alanı çıkarılamadı.")
            return None

        test_group_area = cv2.bitwise_not(test_group_area)
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (2, 2))
        test_group_area = cv2.morphologyEx(test_group_area, cv2.MORPH_ERODE, kernel)

        groups = config['extract_test_group']['groups']
        if not groups:
            logger.error("Konfigürasyonda gruplar tanımlanmamış.")
            return None

        h, w = test_group_area.shape[:2]
        num_choices = len(groups)
        choice_width = w // num_choices
        min_contour_area = config['extract_test_group'].get('min_contour_area', 50)
        similarity_threshold = config['extract_test_group'].get('threshold', 0.2)
        dominance_threshold = config['extract_test_group'].get('dominance_threshold', 0.05)
        filled_ratios = []

        for i in range(num_choices):
            x_start = i * choice_width
            x_end = (i + 1) * choice_width if i < num_choices - 1 else w
            choice_area = test_group_area[:, x_start:x_end]
            contours, _ = cv2.findContours(choice_area, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            max_contour_area = 0

            for contour in contours:
                contour_area = cv2.contourArea(contour)
                if contour_area >= min_contour_area:
                    max_contour_area = max(max_contour_area, contour_area)
                    x, y, width, height = cv2.boundingRect(contour)
                    logger.debug(
                        f"Seçenek {groups[i]}: Kontur Alanı = {contour_area}, "
                        f"Pozisyon = ({x}, {y}, {width}, {height})"
                    )

            filled_ratio = max_contour_area / (choice_area.shape[0] * choice_area.shape[1])
            filled_ratios.append((groups[i], filled_ratio))
            logger.debug(f"Seçenek {groups[i]}: Doluluk Oranı = {filled_ratio:.2f}")

        filled_ratios.sort(key=lambda x: x[1], reverse=True)
        logger.debug(f"Sıralanmış Doluluk Oranları: {filled_ratios}")

        if len(filled_ratios) >= 2:
            top_choice, top_ratio = filled_ratios[0]
            second_choice, second_ratio = filled_ratios[1]
            if top_ratio - second_ratio > dominance_threshold and top_ratio >= similarity_threshold:
                logger.info(f"Test grubu için en iyi seçenek: {top_choice}")
                return top_choice
            else:
                logger.warning(
                    f"Dominance threshold karşılanmadı veya {top_choice} ile {second_choice} "
                    f"arasında belirsizlik tespit edildi."
                )
                return "Belirsiz"
        elif len(filled_ratios) == 1:
            top_choice, top_ratio = filled_ratios[0]
            if top_ratio >= similarity_threshold:
                logger.info(f"Test grubu için en iyi seçenek: {top_choice}")
                return top_choice
            else:
                logger.warning("Tek seçenek bulundu ancak benzerlik eşiğinin altında.")
                return None
        else:
            logger.error("Dolgu yapılmış seçenek bulunamadı.")
            return None
    except Exception as e:
        logger.error(f"Test grubu çıkarılırken hata: {e}")
        return None


def save_results(
    answers: Dict[str, Dict[str, Optional[str]]],
    student_number: str,
    test_group: Optional[str],
    config: Dict
) -> Dict:
    """
    Çıkarılan sonuçları kaydeder ve gerekirse JSON formatında saklar.
    """
    results = {
        "student_number": student_number,
        "test_group": test_group,
        "answers": answers
    }

    try:
        if config['output']['save_results_json']:
            results_dir = os.path.join(settings.BASE_DIR, os.path.dirname(config['output']['results_json_path']))
            os.makedirs(results_dir, exist_ok=True)
            results_path = os.path.join(settings.BASE_DIR, config['output']['results_json_path'])
            with open(results_path, 'w', encoding='utf-8') as f:
                yaml.dump(results, f, allow_unicode=True)
            logger.debug(f"Sonuçlar JSON formatında kaydedildi: {results_path}")

        logger.info("Sonuçlar başarıyla kaydedildi.")
        return results
    except Exception as e:
        logger.error(f"Sonuçlar kaydedilirken hata oluştu: {e}")
        return {"error": "Sonuçlar kaydedilirken bir hata oluştu."}


def visualize_results(
    image: np.ndarray,
    rois: List[Tuple[str, List[List[int]]]],
    config: Dict
):
    """
    Sonuçları görselleştirir ve kaydeder.
    """
    try:
        for name, coords in rois:
            if coords is not None:
                (x_start, y_start), (x_end, y_end) = coords
                cv2.rectangle(image, (x_start, y_start), (x_end, y_end), (0, 255, 0), 2)
                cv2.putText(
                    image,
                    name,
                    (x_start, y_start - 10),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.9,
                    (0, 255, 0),
                    2
                )
                logger.debug(f"Görüntüye ROI eklendi: {name}")

        if config['output']['save_visualization']:
            visualization_dir = os.path.join(settings.BASE_DIR, config['output']['visualization_directory'])
            os.makedirs(visualization_dir, exist_ok=True)
            visualization_path = os.path.join(visualization_dir, 'visualization.jpg')
            cv2.imwrite(visualization_path, image)
            logger.debug(f"Görselleştirilmiş görüntü kaydedildi: {visualization_path}")
    except Exception as e:
        logger.error(f"Görselleştirme sırasında hata oluştu: {e}")


def save_results_to_db(results: Dict):
    """
    Çıkarılan sonuçları veritabanına kaydeder.
    """
    try:
        from .models import Student, StudentAnswer, TestGroup, ColumnMapping, AnswerKey

        student_number = results.get('student_number', 'Unknown')
        test_group_name = results.get('test_group', 'Unknown')
        answers = results.get('answers', {})

        student, created = Student.objects.get_or_create(student_number=student_number)
        student.save()  # name alanı artık olmadığı için sadece kaydediyoruz
        logger.debug(f"Öğrenci kaydı güncellendi: {student_number}")

        test_group, _ = TestGroup.objects.get_or_create(name=test_group_name)
        logger.debug(f"Test grubu kaydı oluşturuldu: {test_group_name}")

        column_mappings = ColumnMapping.objects.filter(test_group=test_group)

        for column_number_str, questions in answers.items():
            try:
                column_number = int(column_number_str)
            except ValueError:
                logger.warning(f"Geçersiz sütun numarası: {column_number_str}")
                continue

            column_mapping = column_mappings.filter(column_number=column_number).first()
            if not column_mapping:
                logger.warning(f"Sütun numarası {column_number} için eşleşme bulunamadı.")
                continue

            course = column_mapping.course

            for question_id_str, selected_answer in questions.items():
                try:
                    question_id = int(question_id_str)
                except ValueError:
                    logger.warning(f"Geçersiz soru numarası: {question_id_str}")
                    continue

                valid_choices = TURKISH_LETTERS
                if selected_answer not in valid_choices and selected_answer is not None:
                    logger.warning(f"Geçersiz cevap seçeneği: {selected_answer}")
                    continue

                if not AnswerKey.objects.filter(course=course, test_group=test_group, question_id=question_id).exists():
                    logger.warning(
                        f"AnswerKey bulunamadı: Kurs={course}, Test Grubu={test_group}, Soru={question_id}"
                    )
                    continue

                StudentAnswer.objects.update_or_create(
                    student=student,
                    test_group=test_group,
                    course=course,
                    question_id=question_id,
                    defaults={
                        'selected_answer': selected_answer
                    }
                )
                logger.debug(
                    f"Cevap kaydedildi: {student_number}, Soru {question_id}, Seçenek {selected_answer}"
                )

        logger.info("Sonuçlar veritabanına başarıyla kaydedildi.")
    except Exception as e:
        logger.error(f"Veritabanına kaydetme sırasında hata oluştu: {e}")


def save_answer_key_results(
    answers: Dict[str, Dict[str, Optional[str]]],
    test_group: str,
    config: Dict
) -> Dict:
    """
    Çıkarılan cevap anahtarını AnswerKey modeline kaydeder.
    """
    try:
        from .models import AnswerKey, TestGroup, ColumnMapping, Course

        test_group_obj, created = TestGroup.objects.get_or_create(name=test_group)
        if created:
            logger.debug(f"Yeni test grubu oluşturuldu: {test_group}")

        for column_number_str, questions in answers.items():
            try:
                column_number = int(column_number_str)
            except ValueError:
                logger.warning(f"Geçersiz sütun numarası: {column_number_str}")
                continue

            column_mapping = ColumnMapping.objects.filter(
                test_group=test_group_obj, column_number=column_number
            ).first()
            if not column_mapping:
                logger.warning(f"Sütun numarası {column_number} için ColumnMapping bulunamadı.")
                continue

            course = column_mapping.course
            total_questions = course.total_questions

            for question_id_str, correct_answer in questions.items():
                try:
                    question_id = int(question_id_str)
                except ValueError:
                    logger.warning(f"Geçersiz soru numarası: {question_id_str}")
                    continue

                if question_id > total_questions:
                    logger.warning(
                        f"Soru numarası {question_id} kursun toplam soru sayısını "
                        f"({total_questions}) aşıyor. Skipping."
                    )
                    continue

                if correct_answer is None:
                    logger.warning(
                        f"Geçersiz cevap seçeneği: Sütun {column_number_str}, Soru {question_id_str}"
                    )
                    continue

                AnswerKey.objects.update_or_create(
                    test_group=test_group_obj,
                    course=course,
                    question_id=question_id,
                    defaults={'correct_answer': correct_answer}
                )
                logger.debug(
                    f"AnswerKey kaydı güncellendi veya oluşturuldu: Test Grubu={test_group}, "
                    f"Kurs={course.name}, Soru={question_id}, Cevap={correct_answer}"
                )

        logger.info("Cevap anahtarı veritabanına başarıyla kaydedildi.")
        return {"status": "success", "message": "Cevap anahtarı başarıyla kaydedildi."}

    except Exception as e:
        logger.error(f"Cevap anahtarı veritabanına kaydedilirken hata: {e}")
        return {"error": "Cevap anahtarı veritabanına kaydedilirken bir hata oluştu."}


def load_template(template_path: str) -> Optional[np.ndarray]:
    """
    Şablon görüntüyü yükler.
    """
    try:
        if not os.path.isabs(template_path):
            template_path = os.path.join(settings.BASE_DIR, template_path.lstrip('/'))
        template = cv2.imread(template_path, cv2.IMREAD_GRAYSCALE)
        if template is None:
            logger.error(f"Şablon görüntü yüklenemedi: {template_path}")
            return None
        logger.debug(f"Şablon görüntü yüklendi: {template_path}")
        return template
    except Exception as e:
        logger.error(f"Şablon yüklenirken hata oluştu: {e}")
        return None


def align_image_with_feature_matching(
    image: np.ndarray,
    template: np.ndarray,
    config: Dict
) -> Optional[np.ndarray]:
    """
    Feature matching kullanarak görüntüyü şablona hizalar.
    """
    try:
        orb = cv2.ORB_create(nfeatures=5000)
        kp1, des1 = orb.detectAndCompute(template, None)
        scales = np.linspace(0.5, 1.5, num=11)
        min_matches = config['feature_matching']['min_matches']
        bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)

        for scale in scales:
            resized_image = cv2.resize(image, None, fx=scale, fy=scale, interpolation=cv2.INTER_LINEAR)
            kp2, des2 = orb.detectAndCompute(resized_image, None)
            if des2 is None:
                continue
            matches = bf.match(des1, des2)
            matches = sorted(matches, key=lambda x: x.distance)
            if len(matches) < min_matches:
                continue

            src_pts = np.float32([kp1[m.queryIdx].pt for m in matches]).reshape(-1, 1, 2)
            dst_pts = np.float32([kp2[m.trainIdx].pt for m in matches]).reshape(-1, 1, 2)
            M, mask = cv2.findHomography(dst_pts, src_pts, cv2.RANSAC, 5.0)

            if M is not None:
                h, w = template.shape
                aligned_image = cv2.warpPerspective(resized_image, M, (w, h))
                if config['output']['save_debug_images']:
                    debug_dir = os.path.join(settings.BASE_DIR, config['output']['debug_images_directory'])
                    os.makedirs(debug_dir, exist_ok=True)
                    aligned_image_path = os.path.join(debug_dir, f'aligned_image_scale_{scale}.jpg')
                    cv2.imwrite(aligned_image_path, aligned_image)
                    logger.debug(f"Hizalanmış görüntü kaydedildi: {aligned_image_path}")
                logger.info(f"Görüntü {scale} ölçeğinde hizalandı.")
                return aligned_image

        logger.error("Görüntü herhangi bir ölçekte hizalanamadı.")
        return None
    except Exception as e:
        logger.error(f"Görüntü hizalaması sırasında hata: {e}")
        return None


def alternative_alignment_method(
    image: np.ndarray,
    template: np.ndarray,
    config: Dict
) -> Optional[np.ndarray]:
    """
    Alternatif hizalama yöntemini kullanır. (İsteğe bağlı)
    """
    logger.info("Alternatif hizalama yöntemi kullanılıyor.")
    # Buraya alternatif hizalama algoritmanız eklenebilir.
    return None


def process_image(image_path: str, config: Dict) -> Dict:
    """
    Görüntüyü işleyerek gerekli alanları çıkarır ve sonuçları döner.
    """
    try:
        setup_logging(config)
        template = load_template(config['template_matching']['template_path'])
        if template is None:
            logger.error("Şablon görüntü yüklenemedi.")
            return {"error": "Şablon görüntü yüklenemedi."}

        image = cv2.imread(image_path)
        if image is None:
            logger.error(f"Görüntü yüklenemedi: {image_path}")
            return {"error": "Görüntü yüklenemedi."}

        logger.info(f"Görüntü yüklendi: {image_path}")

        # Görüntüyü kırpma
        image = crop_borders(image)

        # Görüntüyü hizalama
        aligned_image = align_image_with_feature_matching(image, template, config)
        if aligned_image is None:
            aligned_image = alternative_alignment_method(image, template, config)
            if aligned_image is None:
                logger.error("Hizalama başarısız oldu.")
                return {"error": "Hizalama başarısız oldu."}

        # Ön işleme
        deskewed_image, thresh = preprocess_image(aligned_image, config)
        if deskewed_image is None or thresh is None:
            logger.error("Ön işleme başarısız.")
            return {"error": "Ön işleme başarısız."}

        # Cevap alanını bulma
        answer_heading_text = config['dynamic_roi']['answer_heading_text']
        answer_area_config = {
            'offset_x': config['dynamic_roi']['answer_area'].get('offset_x', 0),
            'offset_y': config['dynamic_roi']['answer_area'].get('offset_y', 0),
            'width': config['dynamic_roi']['answer_area']['width'],
            'height': config['dynamic_roi']['answer_area']['height'],
            'extra_width': config['dynamic_roi']['answer_area'].get('extra_width', 0),
            'extra_height': config['dynamic_roi']['answer_area'].get('extra_height', 0),
            'min_text_length': config['dynamic_roi'].get('answer_heading_min_text_length', 6),
            'max_text_length': config['dynamic_roi'].get('answer_heading_max_text_length', 15)
        }
        answer_coords = find_heading_coordinates(
            aligned_image,
            [answer_heading_text],
            config,
            answer_area_config
        )
        if answer_coords is None:
            logger.error("Cevap alanı koordinatları bulunamadı.")
            return {"error": "Cevap alanı bulunamadı."}

        # Öğrenci numarası alanını bulma
        student_heading_text = config['dynamic_roi']['student_number_heading_text']
        student_area_config = {
            'offset_x': config['dynamic_roi']['student_number_area'].get('offset_x', 0),
            'offset_y': config['dynamic_roi']['student_number_area'].get('offset_y', 0),
            'width': config['dynamic_roi']['student_number_area']['width'],
            'height': config['dynamic_roi']['student_number_area']['height'],
            'extra_width': config['dynamic_roi']['student_number_area'].get('extra_width', 0),
            'extra_height': config['dynamic_roi']['student_number_area'].get('extra_height', 0),
            'min_text_length': config['dynamic_roi'].get('student_number_heading_min_text_length', 10),
            'max_text_length': config['dynamic_roi'].get('student_number_heading_max_text_length', 20)
        }
        student_number_coords = find_heading_coordinates(
            aligned_image,
            [student_heading_text],
            config,
            student_area_config
        )
        if student_number_coords is None:
            logger.error("Öğrenci numarası alanı koordinatları bulunamadı.")
            student_number = "Unknown"
        else:
            student_number_area = extract_roi(
                thresh,
                student_number_coords,
                "student_number_area",
                config
            )
            if student_number_area is None:
                logger.error("Öğrenci numarası alanı çıkarılamadı.")
                student_number = "Unknown"
            else:
                student_number = extract_student_number(student_number_area, config)

        # Test grubu alanını bulma
        test_group_heading_text = config['dynamic_roi']['test_group_heading_text']
        test_group_area_config = {
            'offset_x': config['dynamic_roi']['test_group_area'].get('offset_x', 0),
            'offset_y': config['dynamic_roi']['test_group_area'].get('offset_y', 0),
            'width': config['dynamic_roi']['test_group_area']['width'],
            'height': config['dynamic_roi']['test_group_area']['height'],
            'extra_width': config['dynamic_roi']['test_group_area'].get('extra_width', 0),
            'extra_height': config['dynamic_roi']['test_group_area'].get('extra_height', 0),
            'min_text_length': config['dynamic_roi'].get('test_group_heading_min_text_length', 8),
            'max_text_length': config['dynamic_roi'].get('test_group_heading_max_text_length', 10)
        }
        test_group_coords = find_heading_coordinates(
            aligned_image,
            [test_group_heading_text],
            config,
            test_group_area_config
        )
        if test_group_coords is None:
            logger.error("Test grubu alanı koordinatları bulunamadı.")
            test_group = None
        else:
            test_group = extract_test_group(
                thresh,
                test_group_coords,
                config
            )

        # Cevapları çıkarma
        answers = extract_answers(
            thresh,
            answer_coords,
            config
        )
        if not answers:
            logger.error("Cevaplar çıkarılamadı.")
            return {"error": "Cevaplar çıkarılamadı."}

        # Sonuçları kaydetme
        results = save_results(answers, student_number, test_group, config)

        # Görselleştirme
        rois = [
            ("Answer Area", answer_coords),
            ("Student Number Area", student_number_coords),
            ("Test Group Area", test_group_coords)
        ]
        visualize_results(aligned_image.copy(), rois, config)

        # Veritabanına kaydetme
        save_results_to_db(results)
        logger.info("Tüm işlemler başarıyla tamamlandı.")
        return results
    except Exception as e:
        logger.error(f"İşlem sırasında hata oluştu: {e}")
        return {"error": "İşlem sırasında bir hata oluştu."}


def process_answer_key_image(image_path: str, config: Dict) -> Dict:
    """
    Cevap anahtarı görüntüsünü işleyerek test grubu ve cevap anahtarını çıkarır.
    """
    try:
        setup_logging(config)

        # Şablon görüntüyü yükleme
        template = load_template(config['template_matching']['template_path'])
        if template is None:
            logger.error("Şablon görüntü yüklenemedi.")
            return {"error": "Şablon görüntü yüklenemedi."}

        # Görüntüyü yükleme
        image = cv2.imread(image_path)
        if image is None:
            logger.error(f"Cevap anahtarı görüntüsü yüklenemedi: {image_path}")
            return {"error": "Cevap anahtarı görüntüsü yüklenemedi."}

        logger.info(f"Cevap anahtarı görüntüsü yüklendi: {image_path}")

        # Görüntüyü kırpma
        image = crop_borders(image)

        # Görüntüyü hizalama
        aligned_image = align_image_with_feature_matching(image, template, config)
        if aligned_image is None:
            aligned_image = alternative_alignment_method(image, template, config)
            if aligned_image is None:
                logger.error("Hizalama başarısız oldu.")
                return {"error": "Hizalama başarısız oldu."}

        # Ön işleme
        deskewed_image, thresh = preprocess_image(aligned_image, config)
        if deskewed_image is None or thresh is None:
            logger.error("Ön işleme başarısız.")
            return {"error": "Ön işleme başarısız."}

        # Cevap alanını bulma
        answer_heading_text = config['dynamic_roi']['answer_key_heading_text']
        answer_area_config = {
            'offset_x': config['dynamic_roi']['answer_key_area'].get('offset_x', 0),
            'offset_y': config['dynamic_roi']['answer_key_area'].get('offset_y', 0),
            'width': config['dynamic_roi']['answer_key_area']['width'],
            'height': config['dynamic_roi']['answer_key_area']['height'],
            'extra_width': config['dynamic_roi']['answer_key_area'].get('extra_width', 0),
            'extra_height': config['dynamic_roi']['answer_key_area'].get('extra_height', 0),
            'min_text_length': config['dynamic_roi'].get('answer_key_heading_min_text_length', 6),
            'max_text_length': config['dynamic_roi'].get('answer_key_heading_max_text_length', 15)
        }
        answer_coords = find_heading_coordinates(
            aligned_image,
            [answer_heading_text],
            config,
            answer_area_config
        )
        if answer_coords is None:
            logger.error("Cevap alanı koordinatları bulunamadı.")
            return {"error": "Cevap alanı bulunamadı."}

        # Test grubu alanını bulma
        test_group_heading_text = config['dynamic_roi']['test_group_heading_text']
        test_group_area_config = {
            'offset_x': config['dynamic_roi']['test_group_area'].get('offset_x', 0),
            'offset_y': config['dynamic_roi']['test_group_area'].get('offset_y', 0),
            'width': config['dynamic_roi']['test_group_area']['width'],
            'height': config['dynamic_roi']['test_group_area']['height'],
            'extra_width': config['dynamic_roi']['test_group_area'].get('extra_width', 0),
            'extra_height': config['dynamic_roi']['test_group_area'].get('extra_height', 0),
            'min_text_length': config['dynamic_roi'].get('test_group_heading_min_text_length', 8),
            'max_text_length': config['dynamic_roi'].get('test_group_heading_max_text_length', 10)
        }
        test_group_coords = find_heading_coordinates(
            aligned_image,
            [test_group_heading_text],
            config,
            test_group_area_config
        )
        if test_group_coords is None:
            logger.error("Test grubu alanı koordinatları bulunamadı.")
            return {"error": "Test grubu alanı bulunamadı."}
        else:
            test_group = extract_test_group(
                thresh,
                test_group_coords,
                config
            )
            if test_group is None:
                logger.error("Test grubu çıkarılamadı.")
                return {"error": "Test grubu çıkarılamadı."}

        # Cevapları çıkarma
        answers = extract_answers(
            thresh,
            answer_coords,
            config
        )
        if not answers:
            logger.error("Cevaplar çıkarılamadı.")
            return {"error": "Cevaplar çıkarılamadı."}

        # Sonuçları kaydetme
        results = save_answer_key_results(answers, test_group, config)

        # Görselleştirme
        rois = [
            ("Answer Key Area", answer_coords),
            ("Test Group Area", test_group_coords)
        ]
        visualize_results(aligned_image.copy(), rois, config)

        logger.info("Cevap anahtarı işleme tamamlandı.")
        return results
    except Exception as e:
        logger.error(f"Cevap anahtarı işlenirken hata: {e}")
        return {"error": "Cevap anahtarı işlenirken bir hata oluştu."}
