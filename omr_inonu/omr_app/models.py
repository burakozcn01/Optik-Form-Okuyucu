from django.db import models
from django.core.validators import MinValueValidator, MaxValueValidator
from django.db import transaction
from django.db.models import Count, Q

class Course(models.Model):
    name = models.CharField(max_length=100, unique=True, null=False)
    code = models.CharField(max_length=10, unique=True, null=False)
    description = models.TextField(null=True, blank=True)
    total_questions = models.PositiveIntegerField(default=1, validators=[MinValueValidator(1)])

    def __str__(self):
        return f"{self.code} - {self.name}"


class TestGroup(models.Model):
    name = models.CharField(max_length=50, unique=True, null=False)
    total_questions = models.PositiveIntegerField(default=1, validators=[MinValueValidator(1)])

    def __str__(self):
        return f"{self.name}"


class ColumnMapping(models.Model):
    test_group = models.ForeignKey(TestGroup, on_delete=models.CASCADE, related_name='test_group_columns')
    column_number = models.PositiveIntegerField(null=False, validators=[MinValueValidator(1), MaxValueValidator(100)])
    course = models.ForeignKey(Course, on_delete=models.CASCADE, related_name='course_columns')

    def __str__(self):
        return f"Group:{self.test_group.name} Column:{self.column_number} Course:{self.course.name}"


class AnswerKey(models.Model):
    test_group = models.ForeignKey(TestGroup, on_delete=models.CASCADE, related_name='answer_keys')
    course = models.ForeignKey(Course, on_delete=models.CASCADE, related_name='answer_keys')
    question_id = models.IntegerField(null=False)
    correct_answer = models.CharField(
        max_length=1,
        null=False,
        choices=[('A', 'A'), ('B', 'B'), ('C', 'C'), ('D', 'D'), ('E', 'E')]
    )

    class Meta:
        unique_together = ('test_group', 'course', 'question_id')

    def __str__(self):
        return f"Group:{self.test_group.name} Course:{self.course.name} Q:{self.question_id} A:{self.correct_answer}"


class Student(models.Model):
    student_number = models.CharField(max_length=20, unique=True, null=False)
    results = models.JSONField(default=dict)  

    def __str__(self):
        return self.student_number

    @transaction.atomic
    def calculate_results(self):
        """
        Öğrencinin sonuçlarını hesaplar ve günceller.
        """
        results = {}
        courses = Course.objects.prefetch_related('course_columns__test_group')

        for course in courses:
            course_results = {}

            test_groups = TestGroup.objects.filter(test_group_columns__course=course).distinct()
            total_questions = sum(group.total_questions for group in test_groups)
            if total_questions == 0:
                continue

            answers_agg = StudentAnswer.objects.filter(
                student=self,
                course=course
            ).aggregate(
                correct=Count('id', filter=Q(is_correct=True)),
                incorrect=Count('id', filter=Q(is_correct=False))
            )

            correct_answers = answers_agg['correct']
            incorrect_answers = answers_agg['incorrect']

            if correct_answers > 0 or incorrect_answers > 0:
                course_results['overall'] = {
                    'score': round((correct_answers / total_questions) * 100, 2),
                    'correct': correct_answers,
                    'incorrect': incorrect_answers
                }

            if course_results:
                results[course.code] = course_results

        self.results = results
        self.save()


class StudentAnswer(models.Model):
    student = models.ForeignKey(Student, on_delete=models.CASCADE, related_name='student_answers')
    test_group = models.ForeignKey(TestGroup, on_delete=models.CASCADE, related_name='test_group_answers')
    course = models.ForeignKey(Course, on_delete=models.CASCADE, related_name='course_answers')
    question_id = models.IntegerField(null=False)
    selected_answer = models.CharField(
        max_length=1,
        null=False,
        choices=[('A', 'A'), ('B', 'B'), ('C', 'C'), ('D', 'D'), ('E', 'E')]
    )
    is_correct = models.BooleanField(null=True, editable=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = ('student', 'test_group', 'course', 'question_id')

    def save(self, *args, **kwargs):
        answer_key = AnswerKey.objects.filter(
            test_group=self.test_group,
            course=self.course,
            question_id=self.question_id
        ).first()

        self.is_correct = self.selected_answer == answer_key.correct_answer if answer_key else None
        super().save(*args, **kwargs)

        if answer_key:
            self.student.calculate_results()

    def __str__(self):
        return f"Student:{self.student.student_number} Course:{self.course.name} Q:{self.question_id} A:{self.selected_answer} Correct:{self.is_correct}"
