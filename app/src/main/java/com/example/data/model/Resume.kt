package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val website: String,
    val location: String,
    val summary: String,
    val targetJobTitle: String,
    val templateStyle: String = "MODERN", // MODERN, CREATIVE, CLASSIC, TECH
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "experiences",
    foreignKeys = [ForeignKey(
        entity = ResumeEntity::class,
        parentColumns = ["id"],
        childColumns = ["resumeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["resumeId"])]
)
data class ExperienceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeId: Int,
    val jobTitle: String,
    val company: String,
    val startDate: String,
    val endDate: String,
    val description: String
)

@Entity(
    tableName = "educations",
    foreignKeys = [ForeignKey(
        entity = ResumeEntity::class,
        parentColumns = ["id"],
        childColumns = ["resumeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["resumeId"])]
)
data class EducationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeId: Int,
    val school: String,
    val degree: String,
    val startDate: String,
    val endDate: String,
    val gpa: String
)

@Entity(
    tableName = "skills",
    foreignKeys = [ForeignKey(
        entity = ResumeEntity::class,
        parentColumns = ["id"],
        childColumns = ["resumeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["resumeId"])]
)
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeId: Int,
    val name: String,
    val level: String = "" // e.g. "Beginner", "Intermediate", "Expert", or blank
)

@Entity(
    tableName = "projects",
    foreignKeys = [ForeignKey(
        entity = ResumeEntity::class,
        parentColumns = ["id"],
        childColumns = ["resumeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["resumeId"])]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeId: Int,
    val name: String,
    val technologies: String,
    val description: String,
    val url: String
)

@Entity(
    tableName = "cover_letters",
    foreignKeys = [ForeignKey(
        entity = ResumeEntity::class,
        parentColumns = ["id"],
        childColumns = ["resumeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["resumeId"])]
)
data class CoverLetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeId: Int,
    val recipient: String,
    val company: String,
    val subject: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ResumeWithDetails(
    val resume: ResumeEntity,
    val experiences: List<ExperienceEntity> = emptyList(),
    val educations: List<EducationEntity> = emptyList(),
    val skills: List<SkillEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val coverLetters: List<CoverLetterEntity> = emptyList()
)
