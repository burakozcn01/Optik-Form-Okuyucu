from rest_framework import serializers
from django.db.models import Q
from .models import (
    Course, TestGroup, ColumnMapping,
    AnswerKey, Student, StudentAnswer
)

class CourseSerializer(serializers.ModelSerializer):
    column_number = serializers.SerializerMethodField(read_only=True)
    test_group = serializers.PrimaryKeyRelatedField(
        queryset=TestGroup.objects.all(),
        write_only=True,
        required=False
    )

    class Meta:
        model = Course
        fields = ['id', 'name', 'code', 'description', 'column_number', 'test_group', 'total_questions']

    def get_column_number(self, obj):
        mapping = obj.course_columns.first()
        return mapping.column_number if mapping else None

    def create(self, validated_data):
        test_group = validated_data.pop('test_group', None)
        course = Course.objects.create(**validated_data)

        if test_group:
            ColumnMapping.objects.create(course=course, column_number=1, test_group=test_group)

        return course

    def update(self, instance, validated_data):
        test_group = validated_data.pop('test_group', None)
        instance.name = validated_data.get('name', instance.name)
        instance.code = validated_data.get('code', instance.code)
        instance.description = validated_data.get('description', instance.description)
        instance.total_questions = validated_data.get('total_questions', instance.total_questions)
        instance.save()

        if test_group:
            ColumnMapping.objects.update_or_create(
                course=instance,
                test_group=test_group,
                defaults={'column_number': 1}
            )

        return instance


class TestGroupSerializer(serializers.ModelSerializer):
    class Meta:
        model = TestGroup
        fields = ['id', 'name', 'total_questions']

    def validate_total_questions(self, value):
        if value < 1:
            raise serializers.ValidationError("Toplam soru sayısı en az 1 olmalıdır.")
        return value


class ColumnMappingSerializer(serializers.ModelSerializer):
    # DİKKAT: Aynı alan adları hem write_only hem de read_only olarak tekrar kullanılmış.
    course = serializers.PrimaryKeyRelatedField(
        queryset=Course.objects.all(),
        write_only=True
    )
    test_group = serializers.PrimaryKeyRelatedField(
        queryset=TestGroup.objects.all(),
        write_only=True
    )
    course = serializers.CharField(source='course.name', read_only=True)
    test_group = serializers.CharField(source='test_group.name', read_only=True)

    class Meta:
        model = ColumnMapping
        fields = [
            'id',
            'course',
            'test_group',
            'column_number'
        ]

    def validate(self, attrs):
        course = attrs.get('course')
        test_group = attrs.get('test_group')
        column_number = attrs.get('column_number')

        if ColumnMapping.objects.filter(
            course=course,
            test_group=test_group,
            column_number=column_number
        ).exists():
            raise serializers.ValidationError(
                "Bu kurs, test grubu ve sütun numarası kombinasyonu zaten mevcut."
            )

        return attrs


class AnswerKeySerializer(serializers.ModelSerializer):
    course = serializers.PrimaryKeyRelatedField(
        queryset=Course.objects.all(),
        write_only=True
    )
    test_group = serializers.PrimaryKeyRelatedField(
        queryset=TestGroup.objects.all(),
        write_only=True
    )
    course_name = serializers.CharField(source='course.name', read_only=True)
    test_group_name = serializers.CharField(source='test_group.name', read_only=True)

    class Meta:
        model = AnswerKey
        fields = [
            'id',
            'course',
            'test_group',
            'question_id',
            'correct_answer',
            'course_name',
            'test_group_name',
        ]

    def create(self, validated_data):
        test_group = validated_data.pop('test_group')
        course = validated_data.pop('course')
        question_id = validated_data.pop('question_id')
        correct_answer = validated_data.pop('correct_answer')

        answer_key, created = AnswerKey.objects.update_or_create(
            test_group=test_group,
            course=course,
            question_id=question_id,
            defaults={'correct_answer': correct_answer}
        )
        return answer_key


class StudentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Student
        fields = ['id', 'student_number', 'results'] 

    def validate_student_number(self, value):
        if not value:
            raise serializers.ValidationError("Öğrenci numarası boş olamaz.")
        return value


class StudentAnswerSerializer(serializers.ModelSerializer):
    course = serializers.PrimaryKeyRelatedField(
        queryset=Course.objects.all(),
        write_only=True
    )
    test_group = serializers.PrimaryKeyRelatedField(
        queryset=TestGroup.objects.all(),
        write_only=True
    )
    student = serializers.PrimaryKeyRelatedField(
        queryset=Student.objects.all(),
        write_only=True
    )

    course = serializers.CharField(source='course.name', read_only=True)
    test_group = serializers.CharField(source='test_group.name', read_only=True)
    student = serializers.CharField(source='student.student_number', read_only=True)

    class Meta:
        model = StudentAnswer
        fields = [
            'id',
            'student',
            'test_group',
            'course',
            'question_id',
            'selected_answer',
            'is_correct',
            'created_at',
            'updated_at',
            'course',
            'test_group',
            'student',
        ]
        read_only_fields = ['is_correct', 'created_at', 'updated_at']

    def validate_selected_answer(self, value):
        if value not in ['A', 'B', 'C', 'D', 'E']:
            raise serializers.ValidationError("Geçersiz cevap seçeneği.")
        return value

    def validate(self, attrs):
        course = attrs.get('course')
        test_group = attrs.get('test_group')
        question_id = attrs.get('question_id')

        if not AnswerKey.objects.filter(
            course=course,
            test_group=test_group,
            question_id=question_id
        ).exists():
            raise serializers.ValidationError("Bu soru için bir cevap anahtarı bulunamadı.")
        return attrs
