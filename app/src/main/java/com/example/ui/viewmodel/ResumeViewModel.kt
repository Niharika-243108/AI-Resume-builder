package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ResumeDatabase
import com.example.data.model.*
import com.example.data.repository.ResumeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ResumeDatabase.getDatabase(application)
    private val repository = ResumeRepository(database.resumeDao())

    // All resumes
    val allResumes: StateFlow<List<ResumeEntity>> = repository.allResumes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Resume ID
    private val _activeResumeId = MutableStateFlow<Int?>(null)
    val activeResumeId: StateFlow<Int?> = _activeResumeId.asStateFlow()

    // Active Resume With Details
    val activeResumeDetails: StateFlow<ResumeWithDetails?> = _activeResumeId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getResumeWithDetails(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // AI Operation States
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion: StateFlow<String?> = _aiSuggestion.asStateFlow()

    private val _suggestedSkills = MutableStateFlow<List<String>>(emptyList())
    val suggestedSkills: StateFlow<List<String>> = _suggestedSkills.asStateFlow()

    fun clearAiState() {
        _aiSuggestion.value = null
        _aiError.value = null
    }

    fun setActiveResume(id: Int?) {
        _activeResumeId.value = id
        _suggestedSkills.value = emptyList()
        clearAiState()
    }

    // --- Resume Actions ---
    fun createResume(
        title: String, 
        targetJobTitle: String, 
        isFresher: Boolean = true,
        templateStyle: String = "MODERN",
        fullName: String = "John Doe",
        email: String = "johndoe@example.com"
    ) {
        viewModelScope.launch {
            val defaultSummary = if (isFresher) {
                "Enthusiastic and highly motivated aspiring $targetJobTitle eager to contribute technical skills and dynamic problem-solving to a professional team. Dedicated to continuous learning and professional development."
            } else {
                "Results-driven and highly skilled $targetJobTitle with a proven track record of delivering high-quality solutions and optimizing operational workflows. Expert in driving collaboration and executing complex projects."
            }

            val newResume = ResumeEntity(
                title = title,
                fullName = fullName,
                email = email,
                phone = "+1 (555) 019-2834",
                website = "github.com/${fullName.lowercase().replace(" ", "")}",
                location = "San Francisco, CA",
                summary = defaultSummary,
                targetJobTitle = targetJobTitle,
                templateStyle = templateStyle
            )
            val newId = repository.saveResume(newResume)
            
            // Seed a sample education or experience if empty to guide freshers
            if (isFresher) {
                repository.saveEducation(
                    EducationEntity(
                        resumeId = newId,
                        school = "State University",
                        degree = "B.S. in Computer Science",
                        startDate = "2022",
                        endDate = "2026",
                        gpa = "3.8/4.0"
                    )
                )
                repository.saveSkill(SkillEntity(resumeId = newId, name = "Problem Solving", level = "Expert"))
                repository.saveSkill(SkillEntity(resumeId = newId, name = "Collaboration", level = "Intermediate"))
            }

            setActiveResume(newId)
        }
    }

    fun updateResume(resume: ResumeEntity) {
        viewModelScope.launch {
            repository.saveResume(resume)
        }
    }

    fun deleteResume(id: Int) {
        viewModelScope.launch {
            if (_activeResumeId.value == id) {
                setActiveResume(null)
            }
            repository.deleteResume(id)
        }
    }

    // --- Experience Actions ---
    fun addExperience(resumeId: Int, jobTitle: String, company: String, startDate: String, endDate: String, description: String) {
        viewModelScope.launch {
            repository.saveExperience(
                ExperienceEntity(
                    resumeId = resumeId,
                    jobTitle = jobTitle,
                    company = company,
                    startDate = startDate,
                    endDate = endDate,
                    description = description
                )
            )
        }
    }

    fun deleteExperience(id: Int) {
        viewModelScope.launch {
            repository.deleteExperience(id)
        }
    }

    // --- Education Actions ---
    fun addEducation(resumeId: Int, school: String, degree: String, startDate: String, endDate: String, gpa: String) {
        viewModelScope.launch {
            repository.saveEducation(
                EducationEntity(
                    resumeId = resumeId,
                    school = school,
                    degree = degree,
                    startDate = startDate,
                    endDate = endDate,
                    gpa = gpa
                )
            )
        }
    }

    fun deleteEducation(id: Int) {
        viewModelScope.launch {
            repository.deleteEducation(id)
        }
    }

    // --- Skill Actions ---
    fun addSkill(resumeId: Int, name: String, level: String) {
        viewModelScope.launch {
            repository.saveSkill(
                SkillEntity(
                    resumeId = resumeId,
                    name = name,
                    level = level
                )
            )
        }
    }

    fun deleteSkill(id: Int) {
        viewModelScope.launch {
            repository.deleteSkill(id)
        }
    }

    // --- Project Actions ---
    fun addProject(resumeId: Int, name: String, technologies: String, description: String, url: String) {
        viewModelScope.launch {
            repository.saveProject(
                ProjectEntity(
                    resumeId = resumeId,
                    name = name,
                    technologies = technologies,
                    description = description,
                    url = url
                )
            )
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    // --- Cover Letter Actions ---
    fun addCoverLetter(resumeId: Int, recipient: String, company: String, subject: String, body: String) {
        viewModelScope.launch {
            repository.saveCoverLetter(
                CoverLetterEntity(
                    resumeId = resumeId,
                    recipient = recipient,
                    company = company,
                    subject = subject,
                    body = body
                )
            )
        }
    }

    fun deleteCoverLetter(id: Int) {
        viewModelScope.launch {
            repository.deleteCoverLetter(id)
        }
    }

    // --- AI Integration Methods ---

    fun triggerGenerateSummary(jobTitle: String, skills: String, experienceText: String, isFresher: Boolean) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            _aiSuggestion.value = null
            try {
                val summary = repository.generateSummary(jobTitle, skills, experienceText, isFresher)
                _aiSuggestion.value = summary
            } catch (e: Exception) {
                _aiError.value = e.localizedMessage ?: "Failed to generate summary"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun triggerImproveDescription(role: String, originalDescription: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            try {
                val polished = repository.improveDescription(role, originalDescription)
                onComplete(polished)
            } catch (e: Exception) {
                _aiError.value = e.localizedMessage ?: "Failed to improve description"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun triggerSuggestSkills(targetRole: String, existingSkills: List<String>) {
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                val suggestions = repository.suggestMissingSkills(targetRole, existingSkills)
                _suggestedSkills.value = suggestions
            } catch (e: Exception) {
                // Ignore gracefully
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun triggerGenerateCoverLetter(resume: ResumeWithDetails, recipient: String, company: String, onComplete: (String, String) -> Unit) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            try {
                val body = repository.generateCoverLetter(resume, recipient, company)
                val subject = "Application for ${resume.resume.targetJobTitle} at $company"
                onComplete(subject, body)
            } catch (e: Exception) {
                _aiError.value = e.localizedMessage ?: "Failed to draft cover letter"
            } finally {
                _aiLoading.value = false
            }
        }
    }
}
