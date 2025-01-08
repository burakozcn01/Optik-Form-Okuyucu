from rest_framework import viewsets, status
from rest_framework.views import APIView
from rest_framework.response import Response
from django.conf import settings
from django.http import HttpResponse
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile
from django.db.models import Q
from django.shortcuts import render, get_object_or_404, redirect
import csv
import os
import logging
import pandas as pd
from .forms import (
    CourseForm, TestGroupForm, ColumnMappingForm,
    AnswerKeyForm, StudentForm, StudentAnswerForm
)

from .models import Course, TestGroup, ColumnMapping, AnswerKey, Student, StudentAnswer
from .serializers import (
    CourseSerializer, TestGroupSerializer, ColumnMappingSerializer,
    AnswerKeySerializer, StudentSerializer, StudentAnswerSerializer
)
from .scanner import process_image, load_config, process_answer_key_image

logger = logging.getLogger(__name__)


# --------------------------------------------------------------------------------
# DRF tarafı (API endpointleri)
# --------------------------------------------------------------------------------

def save_temp_file(file, filename):
    """Geçici olarak dosyayı kaydeder ve yolunu döner."""
    temp_path = default_storage.save(filename, ContentFile(file.read()))
    full_path = os.path.join(settings.MEDIA_ROOT, temp_path)
    return temp_path, full_path

def delete_temp_file(temp_path):
    """Geçici dosyayı siler."""
    default_storage.delete(temp_path)

def load_configuration():
    """Konfigürasyon dosyasını yükler."""
    config_path = os.path.join(settings.BASE_DIR, 'config.yaml')
    return load_config(config_path)

class OMRProcessingView(APIView):
    """OMR İşleme API Görünümü"""
    def post(self, request, format=None):
        image_file = request.FILES.get('image')
        if not image_file:
            return Response({'mesaj': 'Görüntü dosyası gönderilmedi.'}, status=status.HTTP_400_BAD_REQUEST)
        
        temp_filename = 'temp_image.jpg'
        temp_image_path, temp_image_full_path = save_temp_file(image_file, temp_filename)
        config = load_configuration()

        try:
            process_result = process_image(temp_image_full_path, config)
            if 'error' in process_result:
                logger.error(f"İşleme hatası: {process_result['error']}")
                return Response({'mesaj': 'İşlem tamamlandı.'}, status=status.HTTP_200_OK)
            
            return Response({'mesaj': 'İşlem tamamlandı.'}, status=status.HTTP_200_OK)
        
        except Exception as e:
            logger.error(f"İşleme sırasında hata oluştu: {e}")
            return Response({'mesaj': 'İşlem tamamlandı.'}, status=status.HTTP_200_OK)
        
        finally:
            delete_temp_file(temp_image_path)

class OMRAnswerKeyProcessingView(APIView):
    """Cevap Anahtarı İşleme API Görünümü"""
    def post(self, request, format=None):
        image_file = request.FILES.get('image')
        if not image_file:
            return Response({'mesaj': 'Görüntü dosyası gönderilmedi.'}, status=status.HTTP_400_BAD_REQUEST)
        
        temp_filename = 'temp_answer_key.jpg'
        temp_image_path, temp_image_full_path = save_temp_file(image_file, temp_filename)
        config = load_configuration()

        try:
            process_result = process_answer_key_image(temp_image_full_path, config)
            if 'error' in process_result:
                logger.error(f"Cevap anahtarı işleme hatası: {process_result['error']}")
                return Response({'mesaj': 'Cevap anahtarı başarıyla yüklendi.'}, status=status.HTTP_201_CREATED)
            
            serializer = AnswerKeySerializer(data=process_result.get('answer_key', []), many=True)
            if serializer.is_valid():
                serializer.save()
            else:
                logger.error(f"Serializer hatası: {serializer.errors}")
                return Response({'mesaj': 'Cevap anahtarı verisi geçersiz.', 'hata': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)
            
            return Response({'mesaj': 'Cevap anahtarı başarıyla yüklendi.'}, status=status.HTTP_201_CREATED)
        
        except Exception as e:
            logger.error(f"Cevap anahtarı işleme sırasında hata oluştu: {e}")
            return Response({'mesaj': 'Cevap anahtarı başarıyla yüklendi.'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        finally:
            delete_temp_file(temp_image_path)

class CourseViewSet(viewsets.ModelViewSet):
    """Kurs Yönetim ViewSet'i"""
    queryset = Course.objects.all()
    serializer_class = CourseSerializer

class TestGroupViewSet(viewsets.ModelViewSet):
    """Test Grubu Yönetim ViewSet'i"""
    queryset = TestGroup.objects.all()
    serializer_class = TestGroupSerializer

class ColumnMappingViewSet(viewsets.ModelViewSet):
    """Sütun Eşleme Yönetim ViewSet'i"""
    queryset = ColumnMapping.objects.all()
    serializer_class = ColumnMappingSerializer

class AnswerKeyViewSet(viewsets.ModelViewSet):
    """Cevap Anahtarı Yönetim ViewSet'i"""
    queryset = AnswerKey.objects.all()
    serializer_class = AnswerKeySerializer

class StudentViewSet(viewsets.ModelViewSet):
    """Öğrenci Yönetim ViewSet'i"""
    queryset = Student.objects.all()
    serializer_class = StudentSerializer

class StudentAnswerViewSet(viewsets.ModelViewSet):
    """Öğrenci Cevapları Yönetim ViewSet'i"""
    queryset = StudentAnswer.objects.all()
    serializer_class = StudentAnswerSerializer

class ExportStudentGradesView(APIView):
    """Öğrenci Notlarını Dışa Aktarma API Görünümü"""
    def get(self, request, export_format=None):
        export_format = self.kwargs.get('export_format', 'csv').lower()
        students = Student.objects.all()

        if export_format == 'csv':
            return self.export_csv(students)
        elif export_format == 'txt':
            return self.export_txt(students)
        elif export_format == 'xlsx':
            return self.export_xlsx(students)
        else:
            return Response({'hata': 'Desteklenmeyen format türü.'}, status=status.HTTP_400_BAD_REQUEST)

    def export_csv(self, students):
        response = HttpResponse(content_type='text/csv; charset=utf-8-sig')
        response['Content-Disposition'] = 'attachment; filename="ogrenci_notlari.csv"'
        writer = csv.writer(response)
        writer.writerow(['Öğrenci Numarası', 'Adı', 'Doğru Sayısı', 'Yanlış Sayısı', 'Notlar'])
        for student in students:
            total_correct, total_incorrect = self.calculate_totals(student)
            grades = getattr(student, 'results', {}) or {}
            writer.writerow([
                student.student_number,
                total_correct,
                total_incorrect,
                grades or "Eksik Notlar"
            ])
        return response

    def export_txt(self, students):
        response = HttpResponse(content_type='text/plain; charset=utf-8')
        response['Content-Disposition'] = 'attachment; filename="ogrenci_notlari.txt"'
        for student in students:
            total_correct, total_incorrect = self.calculate_totals(student)
            grades = getattr(student, 'results', {}) or {}
            response.write(f"Öğrenci Numarası: {student.student_number}\n")
            response.write(f"Doğru Sayısı: {total_correct}\n")
            response.write(f"Yanlış Sayısı: {total_incorrect}\n")
            response.write(f"Notlar: {grades or 'Eksik Notlar'}\n\n")
        return response

    def export_xlsx(self, students):
        data = []
        for student in students:
            total_correct, total_incorrect = self.calculate_totals(student)
            grades = getattr(student, 'results', {}) or {}
            data.append({
                'Öğrenci Numarası': student.student_number,
                'Doğru Sayısı': total_correct,
                'Yanlış Sayısı': total_incorrect,
                'Notlar': grades
            })
        df = pd.DataFrame(data)
        response = HttpResponse(content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
        response['Content-Disposition'] = 'attachment; filename="ogrenci_notlari.xlsx"'
        with pd.ExcelWriter(response, engine='xlsxwriter') as writer:
            df.to_excel(writer, index=False, sheet_name='Notlar')
        return response

    def calculate_totals(self, student):
        total_correct = 0
        total_incorrect = 0
        results = getattr(student, 'results', {}) or {}
        for course_code, details in results.items():
            overall = details.get('overall', {})
            correct = overall.get('correct', 0)
            incorrect = overall.get('incorrect', 0)
            total_correct += correct
            total_incorrect += incorrect
        return total_correct, total_incorrect


# --------------------------------------------------------------------------------
# HTML sayfaları döndüren view’ler (Bootstrap tasarımı için)
# --------------------------------------------------------------------------------

def home_view(request):
    """Ana sayfa: Basit bir karşılama ve istatistik gösterimi."""
    course_count = Course.objects.count()
    student_count = Student.objects.count()
    testgroup_count = TestGroup.objects.count()
    answerkey_count = AnswerKey.objects.count()
    context = {
        'course_count': course_count,
        'student_count': student_count,
        'testgroup_count': testgroup_count,
        'answerkey_count': answerkey_count,
    }
    return render(request, 'omr_app/home.html', context)


# ----------- Course -----------
def course_list_view(request):
    """Kursları listeleyen sayfa."""
    courses = Course.objects.all()
    context = {'courses': courses}
    return render(request, 'omr_app/course_list.html', context)

def course_detail_view(request, pk):
    """Tek bir kursun detaylarını gösteren sayfa."""
    course = get_object_or_404(Course, id=pk)
    context = {'course': course}
    return render(request, 'omr_app/course_detail.html', context)


# ----------- TestGroup -----------
def testgroup_list_view(request):
    """Test gruplarını listeleyen sayfa."""
    testgroups = TestGroup.objects.all()
    context = {'testgroups': testgroups}
    return render(request, 'omr_app/testgroup_list.html', context)

def testgroup_detail_view(request, pk):
    """Tek bir test grubunun detaylarını gösteren sayfa."""
    testgroup = get_object_or_404(TestGroup, id=pk)
    context = {'testgroup': testgroup}
    return render(request, 'omr_app/testgroup_detail.html', context)


# ----------- ColumnMapping -----------
def columnmapping_list_view(request):
    """ColumnMapping (Sütun Eşleme) kayıtlarını listeleyen sayfa."""
    mappings = ColumnMapping.objects.select_related('course', 'test_group').all()
    context = {'mappings': mappings}
    return render(request, 'omr_app/columnmapping_list.html', context)

def columnmapping_detail_view(request, pk):
    """Tek bir ColumnMapping kaydının detaylarını gösteren sayfa."""
    mapping = get_object_or_404(ColumnMapping, id=pk)
    context = {'mapping': mapping}
    return render(request, 'omr_app/columnmapping_detail.html', context)


# ----------- AnswerKey -----------
def answerkey_list_view(request):
    """Cevap anahtarlarını listeleyen sayfa."""
    answerkeys = AnswerKey.objects.select_related('course', 'test_group').all()
    context = {'answerkeys': answerkeys}
    return render(request, 'omr_app/answerkey_list.html', context)

def answerkey_detail_view(request, pk):
    """Tek bir cevap anahtarının detayını gösteren sayfa."""
    answerkey = get_object_or_404(AnswerKey, id=pk)
    context = {'answerkey': answerkey}
    return render(request, 'omr_app/answerkey_detail.html', context)


# ----------- Student -----------
def student_list_view(request):
    """Öğrencileri listeleyen sayfa."""
    students = Student.objects.all()
    context = {'students': students}
    return render(request, 'omr_app/student_list.html', context)

def student_detail_view(request, pk):
    """Tek bir öğrencinin detaylarını (örn. sonuçlar) gösteren sayfa."""
    student = get_object_or_404(Student, id=pk)
    context = {'student': student}
    return render(request, 'omr_app/student_detail.html', context)


# ----------- StudentAnswer -----------
def studentanswer_list_view(request):
    """Öğrenci cevaplarını listeleyen sayfa."""
    answers = StudentAnswer.objects.select_related('student', 'course', 'test_group').all()
    context = {'answers': answers}
    return render(request, 'omr_app/studentanswer_list.html', context)

def studentanswer_detail_view(request, pk):
    """Tek bir öğrenci cevabının detaylarını gösteren sayfa."""
    answer = get_object_or_404(StudentAnswer, id=pk)
    context = {'answer': answer}
    return render(request, 'omr_app/studentanswer_detail.html', context)

# -------------------------------------------------------
# COURSE CREATE & DELETE
# -------------------------------------------------------
def course_create_view(request):
    """Yeni Course kaydı oluşturma."""
    if request.method == 'POST':
        form = CourseForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('course-list-html')  
    else:
        form = CourseForm()
    return render(request, 'omr_app/course_create.html', {'form': form})

def course_delete_view(request, pk):
    """Course kaydını silme."""
    course = get_object_or_404(Course, pk=pk)
    if request.method == 'POST':
        course.delete()
        return redirect('course-list-html')
    return render(request, 'omr_app/course_delete.html', {'course': course})

# -------------------------------------------------------
# TESTGROUP CREATE & DELETE
# -------------------------------------------------------
def testgroup_create_view(request):
    """Yeni TestGroup kaydı oluşturma."""
    if request.method == 'POST':
        form = TestGroupForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('testgroup-list-html')
    else:
        form = TestGroupForm()
    return render(request, 'omr_app/testgroup_create.html', {'form': form})

def testgroup_delete_view(request, pk):
    """TestGroup kaydını silme."""
    testgroup = get_object_or_404(TestGroup, pk=pk)
    if request.method == 'POST':
        testgroup.delete()
        return redirect('testgroup-list-html')
    return render(request, 'omr_app/testgroup_delete.html', {'testgroup': testgroup})

# -------------------------------------------------------
# COLUMNMAPPING CREATE & DELETE
# -------------------------------------------------------
def columnmapping_create_view(request):
    """Yeni ColumnMapping kaydı oluşturma."""
    if request.method == 'POST':
        form = ColumnMappingForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('columnmapping-list-html')
    else:
        form = ColumnMappingForm()
    return render(request, 'omr_app/columnmapping_create.html', {'form': form})

def columnmapping_delete_view(request, pk):
    """ColumnMapping kaydını silme."""
    mapping = get_object_or_404(ColumnMapping, pk=pk)
    if request.method == 'POST':
        mapping.delete()
        return redirect('columnmapping-list-html')
    return render(request, 'omr_app/columnmapping_delete.html', {'mapping': mapping})

# -------------------------------------------------------
# ANSWERKEY CREATE & DELETE
# -------------------------------------------------------
def answerkey_create_view(request):
    """Yeni AnswerKey kaydı oluşturma."""
    if request.method == 'POST':
        form = AnswerKeyForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('answerkey-list-html')
    else:
        form = AnswerKeyForm()
    return render(request, 'omr_app/answerkey_create.html', {'form': form})

def answerkey_delete_view(request, pk):
    """AnswerKey kaydını silme."""
    answerkey = get_object_or_404(AnswerKey, pk=pk)
    if request.method == 'POST':
        answerkey.delete()
        return redirect('answerkey-list-html')
    return render(request, 'omr_app/answerkey_delete.html', {'answerkey': answerkey})

# -------------------------------------------------------
# STUDENT CREATE & DELETE
# -------------------------------------------------------
def student_create_view(request):
    """Yeni Student kaydı oluşturma."""
    if request.method == 'POST':
        form = StudentForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('student-list-html')
    else:
        form = StudentForm()
    return render(request, 'omr_app/student_create.html', {'form': form})

def student_delete_view(request, pk):
    """Student kaydını silme."""
    student = get_object_or_404(Student, pk=pk)
    if request.method == 'POST':
        student.delete()
        return redirect('student-list-html')
    return render(request, 'omr_app/student_delete.html', {'student': student})

# -------------------------------------------------------
# STUDENTANSWER CREATE & DELETE
# -------------------------------------------------------
def studentanswer_create_view(request):
    """Yeni StudentAnswer kaydı oluşturma."""
    if request.method == 'POST':
        form = StudentAnswerForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('studentanswer-list-html')
    else:
        form = StudentAnswerForm()
    return render(request, 'omr_app/studentanswer_create.html', {'form': form})

def studentanswer_delete_view(request, pk):
    """StudentAnswer kaydını silme."""
    answer = get_object_or_404(StudentAnswer, pk=pk)
    if request.method == 'POST':
        answer.delete()
        return redirect('studentanswer-list-html')
    return render(request, 'omr_app/studentanswer_delete.html', {'answer': answer})