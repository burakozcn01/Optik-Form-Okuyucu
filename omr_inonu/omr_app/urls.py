from django.urls import path
from rest_framework.routers import DefaultRouter
from .views import (
    OMRProcessingView, CourseViewSet, TestGroupViewSet, 
    ColumnMappingViewSet, AnswerKeyViewSet, StudentViewSet, 
    StudentAnswerViewSet, ExportStudentGradesView, OMRAnswerKeyProcessingView,
    home_view,
    course_list_view, course_detail_view,
    testgroup_list_view, testgroup_detail_view,
    columnmapping_list_view, columnmapping_detail_view,
    answerkey_list_view, answerkey_detail_view,
    student_list_view, student_detail_view,
    studentanswer_list_view, studentanswer_detail_view,
    course_create_view, course_delete_view,
    testgroup_create_view, testgroup_delete_view,
    columnmapping_create_view, columnmapping_delete_view,
    answerkey_create_view, answerkey_delete_view,
    student_create_view, student_delete_view,
    studentanswer_create_view, studentanswer_delete_view,
)

router = DefaultRouter()
router.register(r'courses', CourseViewSet)
router.register(r'testgroups', TestGroupViewSet)
router.register(r'columnmappings', ColumnMappingViewSet)
router.register(r'answerkeys', AnswerKeyViewSet)
router.register(r'students', StudentViewSet)
router.register(r'studentanswers', StudentAnswerViewSet)

urlpatterns = [
    path('', home_view, name='home-html'),

    path('courses-html/', course_list_view, name='course-list-html'),
    path('courses-html/<int:pk>/', course_detail_view, name='course-detail-html'),
    path('courses-html/create/', course_create_view, name='course-create-html'),
    path('courses-html/<int:pk>/delete/', course_delete_view, name='course-delete-html'),

    path('testgroups-html/', testgroup_list_view, name='testgroup-list-html'),
    path('testgroups-html/<int:pk>/', testgroup_detail_view, name='testgroup-detail-html'),
    path('testgroups-html/create/', testgroup_create_view, name='testgroup-create-html'),
    path('testgroups-html/<int:pk>/delete/', testgroup_delete_view, name='testgroup-delete-html'),

    path('columnmappings-html/', columnmapping_list_view, name='columnmapping-list-html'),
    path('columnmappings-html/<int:pk>/', columnmapping_detail_view, name='columnmapping-detail-html'),
    path('columnmappings-html/create/', columnmapping_create_view, name='columnmapping-create-html'),
    path('columnmappings-html/<int:pk>/delete/', columnmapping_delete_view, name='columnmapping-delete-html'),

    path('answerkeys-html/', answerkey_list_view, name='answerkey-list-html'),
    path('answerkeys-html/<int:pk>/', answerkey_detail_view, name='answerkey-detail-html'),
    path('answerkeys-html/create/', answerkey_create_view, name='answerkey-create-html'),
    path('answerkeys-html/<int:pk>/delete/', answerkey_delete_view, name='answerkey-delete-html'),

    path('students-html/', student_list_view, name='student-list-html'),
    path('students-html/<int:pk>/', student_detail_view, name='student-detail-html'),
    path('students-html/create/', student_create_view, name='student-create-html'),
    path('students-html/<int:pk>/delete/', student_delete_view, name='student-delete-html'),

    path('studentanswers-html/', studentanswer_list_view, name='studentanswer-list-html'),
    path('studentanswers-html/<int:pk>/', studentanswer_detail_view, name='studentanswer-detail-html'),
    path('studentanswers-html/create/', studentanswer_create_view, name='studentanswer-create-html'),
    path('studentanswers-html/<int:pk>/delete/', studentanswer_delete_view, name='studentanswer-delete-html'),

    path('process/', OMRProcessingView.as_view(), name='omr-process'),
    path('extract-answer-key/', OMRAnswerKeyProcessingView.as_view(), name='extract-answer-key-process'),
    path('export-grades/<str:export_format>/', ExportStudentGradesView.as_view(), name='export-grades'),
]

urlpatterns += router.urls
