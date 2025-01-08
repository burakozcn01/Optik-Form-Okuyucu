from django import forms
from .models import Course, TestGroup, ColumnMapping, AnswerKey, Student, StudentAnswer

class CourseForm(forms.ModelForm):
    class Meta:
        model = Course
        fields = '__all__'

class TestGroupForm(forms.ModelForm):
    class Meta:
        model = TestGroup
        fields = '__all__'

class ColumnMappingForm(forms.ModelForm):
    class Meta:
        model = ColumnMapping
        fields = '__all__'

class AnswerKeyForm(forms.ModelForm):
    class Meta:
        model = AnswerKey
        fields = '__all__'

class StudentForm(forms.ModelForm):
    class Meta:
        model = Student
        fields = '__all__'

class StudentAnswerForm(forms.ModelForm):
    class Meta:
        model = StudentAnswer
        fields = '__all__'
