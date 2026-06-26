package com.example.data.repository

import com.example.BuildConfig
import com.example.data.database.ResumeDao
import com.example.data.api.RetrofitClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResumeRepository(private val resumeDao: ResumeDao) {

    // --- DB Queries ---
    val allResumes: Flow<List<ResumeEntity>> = resumeDao.getAllResumes()

    fun getResumeWithDetails(resumeId: Int): Flow<ResumeWithDetails?> {
        val resumeFlow = resumeDao.getResumeById(resumeId)
        val expFlow = resumeDao.getExperiencesForResume(resumeId)
        val eduFlow = resumeDao.getEducationsForResume(resumeId)
        val skillFlow = resumeDao.getSkillsForResume(resumeId)
        val projFlow = resumeDao.getProjectsForResume(resumeId)
        val clFlow = resumeDao.getCoverLettersForResume(resumeId)

        return combine(resumeFlow, expFlow, eduFlow, skillFlow, projFlow, clFlow) { array ->
            val resume = array[0] as ResumeEntity?
            val exp = array[1] as List<ExperienceEntity>
            val edu = array[2] as List<EducationEntity>
            val skill = array[3] as List<SkillEntity>
            val proj = array[4] as List<ProjectEntity>
            val cl = array[5] as List<CoverLetterEntity>

            if (resume == null) null
            else ResumeWithDetails(
                resume = resume,
                experiences = exp,
                educations = edu,
                skills = skill,
                projects = proj,
                coverLetters = cl
            )
        }
    }

    suspend fun saveResume(resume: ResumeEntity): Int = withContext(Dispatchers.IO) {
        resumeDao.insertResume(resume).toInt()
    }

    suspend fun saveExperience(experience: ExperienceEntity) = withContext(Dispatchers.IO) {
        resumeDao.insertExperience(experience)
    }

    suspend fun deleteExperience(id: Int) = withContext(Dispatchers.IO) {
        resumeDao.deleteExperienceById(id)
    }

    suspend fun saveEducation(education: EducationEntity) = withContext(Dispatchers.IO) {
        resumeDao.insertEducation(education)
    }

    suspend fun deleteEducation(id: Int) = withContext(Dispatchers.IO) {
        resumeDao.deleteEducationById(id)
    }

    suspend fun saveSkill(skill: SkillEntity) = withContext(Dispatchers.IO) {
        resumeDao.insertSkill(skill)
    }

    suspend fun deleteSkill(id: Int) = withContext(Dispatchers.IO) {
        resumeDao.deleteSkillById(id)
    }

    suspend fun saveProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        resumeDao.insertProject(project)
    }

    suspend fun deleteProject(id: Int) = withContext(Dispatchers.IO) {
        resumeDao.deleteProjectById(id)
    }

    suspend fun saveCoverLetter(coverLetter: CoverLetterEntity) = withContext(Dispatchers.IO) {
        resumeDao.insertCoverLetter(coverLetter)
    }

    suspend fun deleteCoverLetter(id: Int) = withContext(Dispatchers.IO) {
        resumeDao.deleteCoverLetterById(id)
    }

    suspend fun deleteResume(resumeId: Int) = withContext(Dispatchers.IO) {
        resumeDao.deleteResumeById(resumeId)
    }

    // --- AI REST calls via Retrofit & Gemini ---
    
    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun generateSummary(
        jobTitle: String,
        skills: String,
        experienceText: String,
        isFresher: Boolean
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val prompt = if (isFresher) {
            "Generate a professional, inspiring, and industry-specific 3-sentence resume summary for a fresher entering the field of '$jobTitle'. " +
                    "They have the following key skills: '$skills'. Focus on their academic achievements, quick learning ability, " +
                    "passion, and how they can bring value to a team. Avoid buzzwords like 'synergy' or 'go-getter'."
        } else {
            "Generate a highly professional, high-impact 3-sentence resume summary for a professional in the role of '$jobTitle'. " +
                    "They have these skills: '$skills' and experience: '$experienceText'. Focus on achievements, domain expertise, " +
                    "and career value. Avoid standard AI clichés."
        }

        val systemInstruction = "You are an elite executive resume writer and career coach. Your summaries are concise, compelling, and perfectly tailored for ATS tracking systems."

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Failed to generate summary content."
        } catch (e: Exception) {
            "Error generating summary: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun improveDescription(role: String, originalDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val prompt = "Rewrite the following resume work/project bullet point for the role of '$role' to make it extremely strong, " +
                "professional, and impactful. Start with a powerful action verb (e.g., 'Engineered', 'Pioneered', 'Optimized', 'Architected'). " +
                "Focus on the action taken and the business result. Keep it as 1 to 2 highly polished professional bullet points.\n\n" +
                "Original bullet point:\n\"$originalDescription\""

        val systemInstruction = "You are a senior hiring manager. You write resume experience statements that clearly highlight quantified impact, responsibility, and engineering/operational excellence."

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                generationConfig = GenerationConfig(temperature = 0.6f)
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Failed to generate real-time suggestions."
        } catch (e: Exception) {
            "Error refining description: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun generateCoverLetter(
        resume: ResumeWithDetails,
        recipientName: String,
        companyName: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val skillsList = resume.skills.joinToString { it.name }
        val expList = resume.experiences.joinToString("\n") { "- ${it.jobTitle} at ${it.company}: ${it.description}" }
        val projList = resume.projects.joinToString("\n") { "- ${it.name}: ${it.description}" }

        val prompt = "Draft a highly professional, engaging, and personalized cover letter addressed to '$recipientName' at '$companyName' " +
                "on behalf of '${resume.resume.fullName}' applying for the role of '${resume.resume.targetJobTitle}'.\n\n" +
                "Use the applicant's background details:\n" +
                "- Full Name: ${resume.resume.fullName}\n" +
                "- Contact: ${resume.resume.email} | ${resume.resume.phone} | ${resume.resume.location}\n" +
                "- Target Role: ${resume.resume.targetJobTitle}\n" +
                "- Key Skills: $skillsList\n" +
                "- Relevant Experiences:\n$expList\n" +
                "- Notable Projects:\n$projList\n\n" +
                "The cover letter should have 3 key sections:\n" +
                "1. Introduction: State interest in the role, mention the company name, and hook the reader's interest.\n" +
                "2. Body: Bridge their experiences/projects with how they directly solve problems or fit the role at the target company.\n" +
                "3. Conclusion: Restate enthusiasm, suggest a follow-up or interview call, and sign off gracefully.\n\n" +
                "Tone: Confident, respectful, professional, and tailored. Keep the letter to about 300 words. Format with clean line breaks."

        val systemInstruction = "You are a professional copywriter specializing in career correspondence. You draft cover letters that stand out by focusing on direct value, culture alignment, and immediate impact."

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Failed to generate cover letter."
        } catch (e: Exception) {
            "Error drafting cover letter: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun suggestMissingSkills(
        targetRole: String,
        existingSkills: List<String>
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext emptyList()
        }

        val prompt = "Based on the target role of '$targetRole' and these current skills: '${existingSkills.joinToString()}', " +
                "recommend 5 additional highly relevant technical or soft skills that would make this resume significantly more competitive. " +
                "Provide them ONLY as a plain, comma-separated list of skill names. Do not include introductory text or bullet points."

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.5f)
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
            if (text.isNotEmpty()) {
                text.split(",")
                    .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    .filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
