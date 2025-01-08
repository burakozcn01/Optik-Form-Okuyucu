package com.reloading.optik_form.data.di

import com.reloading.optik_form.data.repository.*

/**
 * Uygulamanın bağımlılıklarını sağlayan modül.
 * Repository sınıflarını lazy initialization ile oluşturur.
 */
object AppModule {
    val answerKeysRepository by lazy { AnswerKeysRepository() }
    val columnMappingsRepository by lazy { ColumnMappingsRepository() }
    val coursesRepository by lazy { CoursesRepository() }
    val processRepository by lazy { ProcessRepository() }
    val studentAnswersRepository by lazy { StudentAnswersRepository() }
    val studentsRepository by lazy { StudentsRepository() }
}
