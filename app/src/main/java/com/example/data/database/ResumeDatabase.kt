package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY createdAt DESC")
    fun getAllResumes(): Flow<List<ResumeEntity>>

    @Query("SELECT * FROM resumes WHERE id = :id")
    fun getResumeById(id: Int): Flow<ResumeEntity?>

    @Query("SELECT * FROM experiences WHERE resumeId = :resumeId")
    fun getExperiencesForResume(resumeId: Int): Flow<List<ExperienceEntity>>

    @Query("SELECT * FROM educations WHERE resumeId = :resumeId")
    fun getEducationsForResume(resumeId: Int): Flow<List<EducationEntity>>

    @Query("SELECT * FROM skills WHERE resumeId = :resumeId")
    fun getSkillsForResume(resumeId: Int): Flow<List<SkillEntity>>

    @Query("SELECT * FROM projects WHERE resumeId = :resumeId")
    fun getProjectsForResume(resumeId: Int): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM cover_letters WHERE resumeId = :resumeId ORDER BY createdAt DESC")
    fun getCoverLettersForResume(resumeId: Int): Flow<List<CoverLetterEntity>>

    // Insert methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(experience: ExperienceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducation(education: EducationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverLetter(coverLetter: CoverLetterEntity): Long

    // Delete methods
    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResumeById(id: Int)

    @Query("DELETE FROM experiences WHERE id = :id")
    suspend fun deleteExperienceById(id: Int)

    @Query("DELETE FROM educations WHERE id = :id")
    suspend fun deleteEducationById(id: Int)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: Int)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    @Query("DELETE FROM cover_letters WHERE id = :id")
    suspend fun deleteCoverLetterById(id: Int)

    // Clear details
    @Query("DELETE FROM experiences WHERE resumeId = :resumeId")
    suspend fun clearExperiencesForResume(resumeId: Int)

    @Query("DELETE FROM educations WHERE resumeId = :resumeId")
    suspend fun clearEducationsForResume(resumeId: Int)

    @Query("DELETE FROM skills WHERE resumeId = :resumeId")
    suspend fun clearSkillsForResume(resumeId: Int)

    @Query("DELETE FROM projects WHERE resumeId = :resumeId")
    suspend fun clearProjectsForResume(resumeId: Int)
}

@Database(
    entities = [
        ResumeEntity::class,
        ExperienceEntity::class,
        EducationEntity::class,
        SkillEntity::class,
        ProjectEntity::class,
        CoverLetterEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ResumeDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao

    companion object {
        @Volatile
        private var INSTANCE: ResumeDatabase? = null

        fun getDatabase(context: Context): ResumeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ResumeDatabase::class.java,
                    "resume_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
