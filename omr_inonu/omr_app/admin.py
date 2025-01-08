from django.contrib import admin
from .models import Course, TestGroup, ColumnMapping, AnswerKey, Student, StudentAnswer


admin.site.register(Course)
admin.site.register(TestGroup)
admin.site.register(ColumnMapping)
admin.site.register(AnswerKey)
admin.site.register(Student)
admin.site.register(StudentAnswer)

