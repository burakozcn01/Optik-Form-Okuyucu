from django.test import TestCase
from .models import Course, TestGroup, AnswerKey, Student, StudentAnswer

class GradingSystemTests(TestCase):

    def setUp(self):
        self.course = Course.objects.create(name="Math", code="MATH101")
        self.test_group = TestGroup.objects.create(name="Group A")
        self.student = Student.objects.create(student_number="S12345678")
        self.answer_key_1 = AnswerKey.objects.create(
            test_group=self.test_group, course=self.course, question_id=1, correct_answer='A'
        )
        self.answer_key_2 = AnswerKey.objects.create(
            test_group=self.test_group, course=self.course, question_id=2, correct_answer='B'
        )

    def test_correct_answer_grading(self):
        StudentAnswer.objects.create(
            student=self.student, course=self.course, test_group=self.test_group,
            question_id=1, selected_answer='A'
        )
        StudentAnswer.objects.create(
            student=self.student, course=self.course, test_group=self.test_group,
            question_id=2, selected_answer='B'
        )
        self.student.refresh_from_db()
        self.assertEqual(self.student.grades.get(self.course.code), 100)

    def test_incorrect_answer_grading(self):
        StudentAnswer.objects.create(
            student=self.student, course=self.course, test_group=self.test_group,
            question_id=1, selected_answer='C'
        )
        StudentAnswer.objects.create(
            student=self.student, course=self.course, test_group=self.test_group,
            question_id=2, selected_answer='D'
        )
        self.student.refresh_from_db()
        self.assertEqual(self.student.grades.get(self.course.code), 0)

    def test_partial_correct_answer_grading(self):
        StudentAnswer.objects.create(
            student=self.student, course=self.course, test_group=self.test_group,
            question_id=1, selected_answer='A'
        )
        StudentAnswer.objects.create(
            student=self.student, course=self.course, test_group=self.test_group,
            question_id=2, selected_answer='D'
        )
        self.student.refresh_from_db()
        self.assertEqual(self.student.grades.get(self.course.code), 50)

    def test_no_answer_grading(self):
        self.student.refresh_from_db()
        self.assertEqual(self.student.grades.get(self.course.code), 0)
