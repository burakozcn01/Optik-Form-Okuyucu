# Görüntü Yükleme ve Ön İşleme

## 1. Görüntünün Yüklenmesi ve Kırpma

### Fonksiyon: `process_image(image_path: str, config: Dict) -> Dict`

#### Görüntü Yükleme:
```python
image = cv2.imread(image_path)
if image is None:
    logger.error(f"Görüntü yüklenemedi: {image_path}")
    return {"error": "Görüntü yüklenemedi."}
logger.info(f"Görüntü yüklendi: {image_path}")
```
- `cv2.imread` ile belirtilen yol üzerinden görüntü okunur.
- Görüntü başarıyla yüklenmezse hata loglanır ve süreç sonlandırılır.

#### Kenar Kırpma:
```python
image = crop_borders(image)
```
- `crop_borders` fonksiyonu, görüntünün kenarlarındaki boşlukları kırpar.
- Bu adım, gereksiz boşlukları kaldırarak işleme odaklanmayı sağlar.

---

## 2. Görüntü Hizalama

### Şablon Yükleme:
```python
template = load_template(config['template_matching']['template_path'])
if template is None:
    logger.error("Şablon görüntü yüklenemedi.")
    return {"error": "Şablon görüntü yüklenemedi."}
```
- `load_template` fonksiyonu, hizalama için kullanılacak şablon görüntüyü yükler.
- Şablon başarılı bir şekilde yüklenmezse süreç sonlandırılır.

### Hizalama İşlemi:
```python
aligned_image = align_image_with_feature_matching(image, template, config)
if aligned_image is None:
    aligned_image = alternative_alignment_method(image, template, config)
    if aligned_image is None:
        logger.error("Hizalama başarısız oldu.")
        return {"error": "Hizalama başarısız oldu."}
```
- `align_image_with_feature_matching` fonksiyonu, ORB algoritması ve feature matching kullanarak görüntüyü şablona hizalar.
- Eğer birinci yöntem başarısız olursa, `alternative_alignment_method` ile alternatif bir hizalama denenir.
- Her iki yöntem de başarısız olursa süreç sonlandırılır.

---

## 3. Görüntü Ön İşleme

### Deskew (Eğim Düzeltme) ve Eşikleme:
```python
deskewed_image, thresh = preprocess_image(aligned_image, config)
if deskewed_image is None or thresh is None:
    logger.error("Ön işleme başarısız.")
    return {"error": "Ön işleme başarısız."}
```
- `preprocess_image` fonksiyonu, görüntüyü düzeltir (deskew), kontrastını artırır, gürültüyü azaltır ve ikili (binary) hale getirir.
- Bu adım, OCR işlemi için görüntüyü optimize eder.

---

## 4. Metin Tanıma (OCR)

### OCR İşlemi:
```python
ocr_result = perform_ocr_space(thresh, config)
```
- `perform_ocr_space` fonksiyonu, OCR.space API kullanarak eşiklenmiş görüntüden metin çıkarır.
- API'den alınan sonuçlar, tam metin ve detaylı metin bilgilerini içerir.

---

## 5. Bölge İlgi Alanlarının (ROI) Tespiti ve Çıkarılması

### a. Başlık Metni ile Koordinat Bulma
#### Cevap Alanı Koordinatlarını Bulma:
```python
answer_coords = find_heading_coordinates(
    aligned_image,
    [answer_heading_text],
    config,
    answer_area_config
)
if answer_coords is None:
    logger.error("Cevap alanı koordinatları bulunamadı.")
    return {"error": "Cevap alanı bulunamadı."}
```
- `find_heading_coordinates` fonksiyonu, OCR sonucu metinler arasında "CEVAPLAR" başlığını arar.
- Eşik değeri ile benzerlik kontrolü yaparak doğru ROI koordinatlarını belirler.

### b. ROI Çıkarma
#### Cevap Alanını Çıkarma:
```python
answers = extract_answers(
    thresh,
    answer_coords,
    config
)
if not answers:
    logger.error("Cevaplar çıkarılamadı.")
    return {"error": "Cevaplar çıkarılamadı."}
```
- `extract_answers` fonksiyonu, belirlenen cevap alanı koordinatlarından cevapları tespit eder.
- Bu fonksiyon, sütun, soru ve seçenek bazında görüntüyü analiz eder ve hangi seçeneklerin işaretlendiğini belirler.

---

## 6. Veri Çıkarma

### a. Cevapların Belirlenmesi
#### Cevapları Çıkarmak:
```python
answers = extract_answers(thresh, answer_coords, config)
```
- Cevap alanındaki her sütun ve soru için doluluk oranları hesaplanır.
- Eşik değerine göre hangi seçeneğin işaretlendiği tespit edilir.
- Örneğin, 4 sütunlu ve 25 sorulu bir sınavda her sorunun 5 seçeneği vardır.

### b. Öğrenci Numarası ve İsim Çıkarılması
#### Öğrenci Numarası:
```python
student_number = extract_student_number(student_number_area, config)
```
- `extract_student_number` fonksiyonu, öğrenci numarası alanındaki her rakam için doluluk oranını hesaplar.
- Eşik değeri ile karşılaştırarak doğru rakamları belirler.
- Örneğin, 11 haneli bir öğrenci numarası için her hane 10 seçenekten biri olabilir.

### c. Test Grubu Çıkarılması
#### Test Grubu Belirleme:
```python
test_group = extract_test_group(thresh, test_group_coords, config)
```
- `extract_test_group` fonksiyonu, test grubu alanındaki işaretlenmiş grubu tespit eder.
- Her grup için doluluk oranı hesaplanır ve en yüksek doluluk oranına sahip grup belirlenir.

---

## 7. Sonuçların Kaydedilmesi ve Görselleştirme

### a. Dosya Kaydetme
#### Sonuçları Kaydetmek:
```python
results = save_results(answers, student_number, test_group, config, name)
```
- `save_results` fonksiyonu, çıkarılan verileri yapılandırılmış bir formatta (örneğin, JSON) kaydeder.
- Konfigürasyona bağlı olarak sonuçlar dosya olarak saklanabilir.

### b. Veritabanına Kaydetme
#### Veritabanına İşlemek:
```python
save_results_to_db(results)
```
- `save_results_to_db` fonksiyonu, çıkarılan verileri Django ORM kullanarak veritabanına kaydeder.
- Öğrenci, test grubu ve cevaplar ilgili modeller aracılığıyla veritabanında depolanır.

### c. Görselleştirme
#### ROI'leri Görselleştirmek:
```python
rois = [
    ("Answer Area", answer_coords),
    ("Student Number Area", student_number_coords),
    ("Test Group Area", test_group_coords),
]
visualize_results(aligned_image.copy(), rois, config)
```
- `visualize_results` fonksiyonu, işlenen görüntü üzerinde belirlenen ROI'leri dikdörtgenlerle işaretler.
- Bu sayede hangi alanların işlendiği görsel olarak doğrulanabilir.
- Görselleştirilmiş görüntü, konfigürasyonda belirtilen klasöre kaydedilir.

---

## Özet
- **Görüntüyü Yükler ve Kırpar:** Gereksiz kenar boşluklarını kaldırır.
- **Hizalar:** Şablon görüntüye göre perspektif düzeltme yapar.
- **Ön İşleme:** Eğim düzeltme, kontrast artırma ve ikili hale getirme işlemleri gerçekleştirir.
- **OCR ile Metin Tanır:** Görüntüden metin ve metin üzerindeki koordinat bilgilerini çıkarır.
- **ROI'leri Tespit Eder ve Çıkarır:** Cevap alanları, öğrenci numarası ve test grubu gibi bölümleri belirler ve görüntüden ayıklar.
- **Verileri Çıkarır:** Cevapları, öğrenci numarasını ve test grubunu tespit eder.
- **Sonuçları Kaydeder:** Elde edilen verileri dosya ve veritabanına kaydeder.
- **Görselleştirir:** İşlenen görüntü üzerinde tespit edilen ROI'leri işaretler ve kaydeder.

Bu süreç, sınav kağıtlarından otomatik olarak veri çıkarmak için kapsamlı ve entegre bir çözüm sunar.
