package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.*
import com.example.ui.viewmodel.ResumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeApp(viewModel: ResumeViewModel) {
    val activeId by viewModel.activeResumeId.collectAsStateWithLifecycle()
    val allResumes by viewModel.allResumes.collectAsStateWithLifecycle()
    val activeDetails by viewModel.activeResumeDetails.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("resume_app_prefs", android.content.Context.MODE_PRIVATE) }
    
    var isLoggedIn by remember { mutableStateOf(sharedPrefs.getBoolean("is_logged_in", false)) }
    var currentUserEmail by remember { mutableStateOf(sharedPrefs.getString("current_user_email", "") ?: "") }
    var currentUsername by remember { mutableStateOf(sharedPrefs.getString("current_username", "") ?: "") }

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = {
            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
        },
        label = "auth_session_transition"
    ) { authenticated ->
        if (!authenticated) {
            AuthScreen(
                onAuthSuccess = { email, name ->
                    sharedPrefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("current_user_email", email)
                        .putString("current_username", name)
                        .apply()
                    currentUserEmail = email
                    currentUsername = name
                    isLoggedIn = true
                }
            )
        } else {
            AnimatedContent(
                targetState = activeId,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "screen_transition"
            ) { id ->
                if (id == null) {
                    HomeScreen(
                        resumes = allResumes,
                        userName = currentUsername,
                        onLogout = {
                            sharedPrefs.edit()
                                .putBoolean("is_logged_in", false)
                                .putString("current_user_email", "")
                                .putString("current_username", "")
                                .apply()
                            isLoggedIn = false
                            currentUserEmail = ""
                            currentUsername = ""
                        },
                        onCreateResume = { title, job, isFresher, style ->
                            viewModel.createResume(title, job, isFresher, templateStyle = style, fullName = currentUsername, email = currentUserEmail)
                        },
                        onSelectResume = { viewModel.setActiveResume(it) },
                        onDeleteResume = { viewModel.deleteResume(it) }
                    )
                } else {
                    activeDetails?.let { details ->
                        ResumeBuilderScreen(
                            resumeDetails = details,
                            viewModel = viewModel,
                            onBack = { viewModel.setActiveResume(null) }
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

fun getInitials(fullName: String): String {
    if (fullName.isBlank()) return "AI"
    val parts = fullName.trim().split("\\s+".toRegex())
    if (parts.isEmpty()) return "AI"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts[0].take(1) + parts[parts.size - 1].take(1)).uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    resumes: List<ResumeEntity>,
    userName: String,
    onLogout: () -> Unit,
    onCreateResume: (String, String, Boolean, String) -> Unit,
    onSelectResume: (Int) -> Unit,
    onDeleteResume: (Int) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showTemplateShowcase by remember { mutableStateOf(false) }
    var preselectedTemplateStyle by remember { mutableStateOf("MODERN") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ResumeAI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = "Assistant: Professional Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(44.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFFEADDFF))
                                .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                .shadow(1.dp, androidx.compose.foundation.shape.CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(userName),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFBFF)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        preselectedTemplateStyle = "MODERN"
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("create_resume_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build New Resume", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFDFBFF)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Suggestion Card (Primary Action)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD0BCFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF21005D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "AI Content Boost",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEADDFF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "STRENGTH: 82%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                            }
                        }
                        
                        Text(
                            text = "\"I've noticed your summary is a bit brief. Would you like me to expand it based on your experience and key skills?\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF21005D),
                            lineHeight = 20.sp
                        )
                        
                        Button(
                            onClick = {
                                showCreateDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF21005D),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Auto-Generate Summary",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Quick Tool Selection (Grid / Row)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tool 1: Templates
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showTemplateShowcase = true
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8DEF8)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TEMPLATES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Text(
                                text = "Browse 42 Styles",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }

                    // Tool 2: Cover Letter
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showCreateDialog = true
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF2B8B5)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "COVER LETTER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF601410)
                            )
                            Text(
                                text = "Generate Now",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF601410)
                            )
                        }
                    }
                }
            }

            // Quick Fresher Tip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8DEF8).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF6750A4)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Are you a fresher?",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Select 'Fresher' when building to generate a highly compelling summary focused on potential and key academic skills.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Your Resumes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            }

            if (resumes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No Resumes Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the 'Build New Resume' button below to craft your first professional resume.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                items(resumes, key = { it.id }) { resume ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectResume(resume.id) }
                            .testTag("resume_card_${resume.id}"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resume.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = resume.targetJobTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Template: ${resume.templateStyle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteResume(resume.id) },
                                modifier = Modifier.testTag("delete_resume_button_${resume.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var targetJob by remember { mutableStateOf("") }
        var isFresher by remember { mutableStateOf(true) }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Build New Resume",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Resume Title (e.g. My Tech Resume)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_resume_title"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetJob,
                        onValueChange = { targetJob = it },
                        label = { Text("Target Job Role (e.g. Software Engineer)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_resume_job_title"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Are you a fresher?",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isFresher,
                            onCheckedChange = { isFresher = it },
                            modifier = Modifier.testTag("new_resume_fresher_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank() && targetJob.isNotBlank()) {
                                    onCreateResume(title, targetJob, isFresher, preselectedTemplateStyle)
                                    showCreateDialog = false
                                } else {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("new_resume_submit")
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    if (showTemplateShowcase) {
        TemplateShowcaseDialog(
            onDismiss = { showTemplateShowcase = false },
            onSelectTemplate = { style ->
                preselectedTemplateStyle = style
                showTemplateShowcase = false
                showCreateDialog = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderScreen(
    resumeDetails: ResumeWithDetails,
    viewModel: ResumeViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Contact", "Experience", "Education", "Projects", "Skills", "AI Tools", "Preview")
    val context = LocalContext.current

    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = resumeDetails.resume.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resumeDetails.resume.targetJobTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("builder_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            PdfExporter.exportToPdf(context, resumeDetails)
                        },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Horizontal Tab Row styled as bottom navigation of Vibrant Palette
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = Color(0xFFF3EDF7),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF6750A4)
                    )
                },
                divider = {
                    HorizontalDivider(color = Color(0xFFCAC4D0))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F)
                            )
                        },
                        modifier = Modifier.testTag("tab_$title")
                    )
                }
            }

            if (aiLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (aiError != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = aiError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearAiState() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> ContactTab(resume = resumeDetails.resume, onUpdate = { viewModel.updateResume(it) })
                    1 -> ExperienceTab(
                        resumeId = resumeDetails.resume.id,
                        targetRole = resumeDetails.resume.targetJobTitle,
                        experiences = resumeDetails.experiences,
                        onAdd = { title, comp, s, e, desc ->
                            viewModel.addExperience(resumeDetails.resume.id, title, comp, s, e, desc)
                        },
                        onDelete = { viewModel.deleteExperience(it) },
                        viewModel = viewModel
                    )
                    2 -> EducationTab(
                        resumeId = resumeDetails.resume.id,
                        educations = resumeDetails.educations,
                        onAdd = { school, deg, s, e, gpa ->
                            viewModel.addEducation(resumeDetails.resume.id, school, deg, s, e, gpa)
                        },
                        onDelete = { viewModel.deleteEducation(it) }
                    )
                    3 -> ProjectsTab(
                        resumeId = resumeDetails.resume.id,
                        targetRole = resumeDetails.resume.targetJobTitle,
                        projects = resumeDetails.projects,
                        onAdd = { name, tech, desc, url ->
                            viewModel.addProject(resumeDetails.resume.id, name, tech, desc, url)
                        },
                        onDelete = { viewModel.deleteProject(it) },
                        viewModel = viewModel
                    )
                    4 -> SkillsTab(
                        resumeDetails = resumeDetails,
                        viewModel = viewModel
                    )
                    5 -> AiToolsTab(
                        resumeDetails = resumeDetails,
                        viewModel = viewModel
                    )
                    6 -> PreviewTab(
                        resumeDetails = resumeDetails,
                        onStyleSelected = { style ->
                            viewModel.updateResume(resumeDetails.resume.copy(templateStyle = style))
                        }
                    )
                }
            }

            // Bottom Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EDF7))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTab > 0) {
                    OutlinedButton(
                        onClick = { selectedTab-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4)),
                        border = BorderStroke(1.dp, Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (selectedTab < 6) {
                    val nextTabTitle = tabs.getOrNull(selectedTab + 1) ?: "Next"
                    Button(
                        onClick = { selectedTab++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next: $nextTabTitle")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = { 
                            PdfExporter.exportToPdf(context, resumeDetails)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Download Resume")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Share, contentDescription = "Download", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// --- TAB 0: CONTACT ---
@Composable
fun ContactTab(resume: ResumeEntity, onUpdate: (ResumeEntity) -> Unit) {
    var fullName by remember(resume) { mutableStateOf(resume.fullName) }
    var email by remember(resume) { mutableStateOf(resume.email) }
    var phone by remember(resume) { mutableStateOf(resume.phone) }
    var location by remember(resume) { mutableStateOf(resume.location) }
    var website by remember(resume) { mutableStateOf(resume.website) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Personal & Contact Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                onUpdate(resume.copy(fullName = it))
            },
            label = { Text("Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_fullname"),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                onUpdate(resume.copy(email = it))
            },
            label = { Text("Email Address") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_email"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                onUpdate(resume.copy(phone = it))
            },
            label = { Text("Phone Number") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_phone"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        OutlinedTextField(
            value = location,
            onValueChange = {
                location = it
                onUpdate(resume.copy(location = it))
            },
            label = { Text("Location (e.g. San Francisco, CA)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_location"),
            singleLine = true
        )

        OutlinedTextField(
            value = website,
            onValueChange = {
                website = it
                onUpdate(resume.copy(website = it))
            },
            label = { Text("Portfolio / LinkedIn (e.g. github.com/username)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_website"),
            singleLine = true
        )
    }
}

// --- TAB 1: EXPERIENCE ---
@Composable
fun ExperienceTab(
    resumeId: Int,
    targetRole: String,
    experiences: List<ExperienceEntity>,
    onAdd: (String, String, String, String, String) -> Unit,
    onDelete: (Int) -> Unit,
    viewModel: ResumeViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Professional Experience",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_exp_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (experiences.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No Experience Added Yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Freshers can list internships, freelance gigs, or college project leadership here!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(experiences) { exp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exp_card_${exp.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.jobTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(exp.company, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    Text("${exp.startDate} - ${exp.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDelete(exp.id) }, modifier = Modifier.testTag("delete_exp_${exp.id}")) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(exp.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var jobTitle by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Experience", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exp_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exp_company_input"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Date") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exp_start_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("End Date") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exp_end_input"),
                            singleLine = true
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Achievements") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("exp_desc_input"),
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.triggerImproveDescription(jobTitle.ifEmpty { targetRole }, description) { polished ->
                                    description = polished
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("exp_ai_improve"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Refine", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (jobTitle.isNotBlank() && company.isNotBlank()) {
                                    onAdd(jobTitle, company, startDate, endDate, description)
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.testTag("exp_submit")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: EDUCATION ---
@Composable
fun EducationTab(
    resumeId: Int,
    educations: List<EducationEntity>,
    onAdd: (String, String, String, String, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Education History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_edu_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (educations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No Education Added Yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(educations) { edu ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_card_${edu.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(edu.degree, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(edu.school, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text("${edu.startDate} - ${edu.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (edu.gpa.isNotEmpty()) {
                                    Text("GPA/Grade: ${edu.gpa}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { onDelete(edu.id) }, modifier = Modifier.testTag("delete_edu_${edu.id}")) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var school by remember { mutableStateOf("") }
        var degree by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }
        var gpa by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Education", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = school,
                        onValueChange = { school = it },
                        label = { Text("School / University") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_school_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = degree,
                        onValueChange = { degree = it },
                        label = { Text("Degree / Certificate") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_degree_input"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Year") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edu_start_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("End Year") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edu_end_input"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = gpa,
                        onValueChange = { gpa = it },
                        label = { Text("GPA / Grade (e.g. 3.9/4.0)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_gpa_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (school.isNotBlank() && degree.isNotBlank()) {
                                    onAdd(school, degree, startDate, endDate, gpa)
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.testTag("edu_submit")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: PROJECTS ---
@Composable
fun ProjectsTab(
    resumeId: Int,
    targetRole: String,
    projects: List<ProjectEntity>,
    onAdd: (String, String, String, String) -> Unit,
    onDelete: (Int) -> Unit,
    viewModel: ResumeViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal & Academic Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_proj_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No Projects Added Yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Projects are critical for freshers to demonstrate practical technical knowledge!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(projects) { proj ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("proj_card_${proj.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(proj.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    if (proj.url.isNotEmpty()) {
                                        Text(proj.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Text("Technologies: ${proj.technologies}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { onDelete(proj.id) }, modifier = Modifier.testTag("delete_proj_${proj.id}")) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(proj.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var technologies by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("proj_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = technologies,
                        onValueChange = { technologies = it },
                        label = { Text("Technologies Used (e.g. Kotlin, Compose)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("proj_tech_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Project URL (e.g. GitHub link)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("proj_url_input"),
                        singleLine = true
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Project Description / Outcomes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("proj_desc_input"),
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.triggerImproveDescription(targetRole, description) { polished ->
                                    description = polished
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("proj_ai_improve"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Refine", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onAdd(name, technologies, description, url)
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.testTag("proj_submit")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: SKILLS ---
@Composable
fun SkillsTab(
    resumeDetails: ResumeWithDetails,
    viewModel: ResumeViewModel
) {
    var newSkill by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("Intermediate") }
    val levels = listOf("Beginner", "Intermediate", "Expert")

    val suggestedSkills by viewModel.suggestedSkills.collectAsStateWithLifecycle()

    LaunchedEffect(resumeDetails.resume.targetJobTitle) {
        viewModel.triggerSuggestSkills(
            resumeDetails.resume.targetJobTitle,
            resumeDetails.skills.map { it.name }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Skills & Core Competencies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Add Skill Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newSkill,
                    onValueChange = { newSkill = it },
                    label = { Text("Add Skill (e.g. Kotlin)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skill_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Level:", fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(levels) { lvl ->
                            FilterChip(
                                selected = level == lvl,
                                onClick = { level = lvl },
                                label = { Text(lvl) },
                                modifier = Modifier.testTag("level_chip_$lvl")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (newSkill.isNotBlank()) {
                                viewModel.addSkill(resumeDetails.resume.id, newSkill.trim(), level)
                                newSkill = ""
                            }
                        },
                        modifier = Modifier.testTag("add_skill_button")
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        // Suggested AI Skills
        if (suggestedSkills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI Suggested Skills for ${resumeDetails.resume.targetJobTitle}:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedSkills.forEach { sName ->
                        InputChip(
                            selected = false,
                            onClick = {
                                viewModel.addSkill(resumeDetails.resume.id, sName, "Intermediate")
                            },
                            label = { Text(sName) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("suggest_skill_$sName")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Existing Skills List
        Text("Your Skills:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            resumeDetails.skills.forEach { skill ->
                InputChip(
                    selected = true,
                    onClick = { viewModel.deleteSkill(skill.id) },
                    label = { Text("${skill.name} (${skill.level})") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("skill_chip_${skill.id}")
                )
            }
        }
    }
}

// Simple FlowRow helper for chip clouds
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Compose 1.5+ has built-in FlowRow, using a Row wrapped scroll for light compatibility
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

// --- TAB 5: AI TOOLS (Summary & Cover Letter) ---
@Composable
fun AiToolsTab(
    resumeDetails: ResumeWithDetails,
    viewModel: ResumeViewModel
) {
    var recipient by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    val aiSuggestion by viewModel.aiSuggestion.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION A: AI Summary Generator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("AI Summary Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Our AI analyzes your experience, target job title, and skills to write a perfect executive resume summary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        val skillsStr = resumeDetails.skills.joinToString { it.name }
                        val expStr = resumeDetails.experiences.joinToString { "${it.jobTitle} at ${it.company}" }
                        viewModel.triggerGenerateSummary(
                            jobTitle = resumeDetails.resume.targetJobTitle,
                            skills = skillsStr.ifEmpty { "quick learner, motivated" },
                            experienceText = expStr.ifEmpty { "fresher" },
                            isFresher = resumeDetails.experiences.isEmpty()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_generate_summary_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Generate Summary")
                }

                // Show generated summary or current
                Text("Current Professional Summary:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = resumeDetails.resume.summary,
                    onValueChange = { viewModel.updateResume(resumeDetails.resume.copy(summary = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("summary_text_field"),
                    maxLines = 6
                )

                if (aiSuggestion != null) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AI Generated Proposal:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            Text(aiSuggestion ?: "", style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.align(Alignment.End)) {
                                TextButton(onClick = { viewModel.clearAiState() }) {
                                    Text("Discard")
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateResume(resumeDetails.resume.copy(summary = aiSuggestion ?: ""))
                                        viewModel.clearAiState()
                                    },
                                    modifier = Modifier.testTag("apply_ai_summary")
                                ) {
                                    Text("Apply & Use")
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION B: AI Cover Letter Creator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("AI Cover Letter Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Generates a polished, persuasive cover letter matching your target role and resume credentials perfectly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Hiring Manager Name (e.g. Recruiter)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cl_recipient"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Target Company Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cl_company"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (recipient.isNotBlank() && company.isNotBlank()) {
                            viewModel.triggerGenerateCoverLetter(resumeDetails, recipient, company) { subj, body ->
                                viewModel.addCoverLetter(resumeDetails.resume.id, recipient, company, subj, body)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cl_generate_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Draft Cover Letter")
                }

                // Saved Cover Letters
                if (resumeDetails.coverLetters.isNotEmpty()) {
                    Text("Generated Cover Letters:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    resumeDetails.coverLetters.forEach { cl ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cl_card_${cl.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("To: ${cl.recipient} @ ${cl.company}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { viewModel.deleteCoverLetter(cl.id) }, modifier = Modifier.testTag("cl_delete_${cl.id}")) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(cl.subject, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                HorizontalDivider()
                                Text(cl.body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 6: PREVIEW & STYLE ---
@Composable
fun PreviewTab(
    resumeDetails: ResumeWithDetails,
    onStyleSelected: (String) -> Unit
) {
    val styles = listOf("MODERN", "CREATIVE", "CLASSIC", "TECH")
    var selectedStyle by remember(resumeDetails.resume.templateStyle) { mutableStateOf(resumeDetails.resume.templateStyle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Choose Visual Style Template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Template Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEach { style ->
                val isSelected = style == selectedStyle
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedStyle = style
                            onStyleSelected(style)
                        }
                        .testTag("style_card_$style"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (style) {
                                "MODERN" -> Icons.Default.Create
                                "CREATIVE" -> Icons.Default.Star
                                "CLASSIC" -> Icons.Default.Person
                                else -> Icons.Default.Build
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = style,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text("Document Preview (A4 Formatted Scroll)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        // Mock paper preview in Jetpack Compose matching selected style!
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                when (selectedStyle) {
                    "CREATIVE" -> CreativeComposePreview(resumeDetails)
                    "CLASSIC" -> ClassicComposePreview(resumeDetails)
                    "TECH" -> TechComposePreview(resumeDetails)
                    else -> ModernComposePreview(resumeDetails)
                }
            }
        }
    }
}

// --- COMPOSE LIVE DOCUMENT RENDERING STYLES ---

@Composable
fun ModernComposePreview(details: ResumeWithDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(details.resume.fullName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(details.resume.targetJobTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
        }
        Text(
            text = "${details.resume.location}  |  ${details.resume.email}  |  ${details.resume.phone}  |  ${details.resume.website}",
            fontSize = 9.sp,
            color = Color.Black
        )

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        // Summary
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PROFESSIONAL SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(details.resume.summary, fontSize = 10.sp, lineHeight = 14.sp, color = Color.Black)
        }

        // Experiences
        if (details.experiences.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WORK EXPERIENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                details.experiences.forEach { exp ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(exp.jobTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("${exp.startDate} - ${exp.endDate}", fontSize = 9.sp, color = Color.Black)
                        }
                        Text(exp.company, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.Black, fontStyle = FontStyle.Italic)
                        Text(exp.description, fontSize = 9.sp, lineHeight = 13.sp, color = Color.Black)
                    }
                }
            }
        }

        // Education
        if (details.educations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("EDUCATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                details.educations.forEach { edu ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(edu.degree, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("${edu.startDate} - ${edu.endDate}", fontSize = 9.sp, color = Color.Black)
                        }
                        Text(edu.school, fontSize = 9.sp, color = Color.Black)
                        if (edu.gpa.isNotEmpty()) {
                            Text("GPA: ${edu.gpa}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Skills
        if (details.skills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KEY SKILLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    details.skills.forEach { skill ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(skill.name, fontSize = 8.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreativeComposePreview(details: ResumeWithDetails) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sidebar (35%)
        Column(
            modifier = Modifier
                .weight(0.35f)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(details.resume.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(details.resume.targetJobTitle, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CONTACT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(details.resume.email, fontSize = 8.sp, color = Color.Black)
                Text(details.resume.phone, fontSize = 8.sp, color = Color.Black)
                Text(details.resume.location, fontSize = 8.sp, color = Color.Black)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SKILLS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                details.skills.forEach { skill ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(skill.name, fontSize = 7.sp, color = Color.Black)
                    }
                }
            }
        }

        // Main (65%)
        Column(
            modifier = Modifier.weight(0.65f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(details.resume.summary, fontSize = 9.sp, lineHeight = 13.sp, color = Color.Black)
            }

            if (details.experiences.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EXPERIENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    details.experiences.forEach { exp ->
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(exp.jobTitle, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("${exp.startDate}-${exp.endDate}", fontSize = 7.sp, color = Color.Black)
                            }
                            Text(exp.company, fontSize = 8.sp, fontStyle = FontStyle.Italic, color = Color.Black)
                            Text(exp.description, fontSize = 8.sp, lineHeight = 11.sp, color = Color.Black)
                        }
                    }
                }
            }

            if (details.educations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EDUCATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    details.educations.forEach { edu ->
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(edu.degree, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(edu.startDate, fontSize = 7.sp, color = Color.Black)
                            }
                            Text(edu.school, fontSize = 8.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassicComposePreview(details: ResumeWithDetails) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(details.resume.fullName.uppercase(), fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, color = Color.Black)
        Text(details.resume.targetJobTitle, fontSize = 12.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = Color.Black)
        Text(
            text = "${details.resume.location} • ${details.resume.email} • ${details.resume.phone}",
            fontSize = 9.sp,
            fontFamily = FontFamily.Serif,
            color = Color.Black
        )

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PROFESSIONAL SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Color.Black)
            Text(details.resume.summary, fontSize = 9.sp, fontFamily = FontFamily.Serif, lineHeight = 13.sp, textAlign = TextAlign.Justify, color = Color.Black)
        }

        if (details.experiences.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WORK EXPERIENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Color.Black)
                details.experiences.forEach { exp ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exp.jobTitle} - ${exp.company}", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Color.Black)
                            Text("${exp.startDate} - ${exp.endDate}", fontSize = 8.sp, fontFamily = FontFamily.Serif, color = Color.Black)
                        }
                        Text(exp.description, fontSize = 8.sp, fontFamily = FontFamily.Serif, lineHeight = 11.sp, color = Color.Black)
                    }
                }
            }
        }

        if (details.educations.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("EDUCATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Color.Black)
                details.educations.forEach { edu ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${edu.degree} - ${edu.school}", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Color.Black)
                            Text(edu.startDate, fontSize = 8.sp, fontFamily = FontFamily.Serif, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechComposePreview(details: ResumeWithDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black)
                .padding(8.dp)
        ) {
            Text("> ${details.resume.fullName.uppercase()}", fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Role: ${details.resume.targetJobTitle}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
            Text("Contact: ${details.resume.email} / ${details.resume.phone}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("[SUMMARY]", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(details.resume.summary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, lineHeight = 12.sp, color = Color.Black)
        }

        if (details.experiences.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("[EXPERIENCE]", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                details.experiences.forEach { exp ->
                    Column {
                        Text("* ${exp.jobTitle} @ ${exp.company} (${exp.startDate} - ${exp.endDate})", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(exp.description, fontSize = 8.sp, fontFamily = FontFamily.Monospace, lineHeight = 11.sp, color = Color.Black)
                    }
                }
            }
        }

        if (details.skills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("[SKILLS_STACK]", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(
                    text = details.skills.joinToString(" | ") { it.name.uppercase() },
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (String, String) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("resume_app_prefs", android.content.Context.MODE_PRIVATE) }
    
    var isSignUp by remember { mutableStateOf(false) }
    
    // Login states
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    
    // Sign Up states
    var signUpName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero / Brand Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Large Document/Resume Icon as Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFEADDFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Logo",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Text(
                    text = "ResumeAI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                
                Text(
                    text = "Professional Resume Builder Assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center
                )
            }
            
            // Tab-like Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFE8DEF8))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { isSignUp = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSignUp) Color(0xFF21005D) else Color.Transparent,
                        contentColor = if (!isSignUp) Color.White else Color(0xFF49454F)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Login", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { isSignUp = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignUp) Color(0xFF21005D) else Color.Transparent,
                        contentColor = if (isSignUp) Color.White else Color(0xFF49454F)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Sign Up", fontWeight = FontWeight.Bold)
                }
            }
            
            // Animated Form Container
            AnimatedContent(
                targetState = isSignUp,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "auth_form"
            ) { signUpActive ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (signUpActive) {
                            // SIGN UP FORM
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            
                            OutlinedTextField(
                                value = signUpName,
                                onValueChange = { signUpName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                            )
                            
                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = { signUpEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
                            )
                            
                            OutlinedTextField(
                                value = signUpPassword,
                                onValueChange = { signUpPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Text(if (passwordVisible) "HIDE" else "SHOW", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("signup_password_input")
                            )
                            
                            OutlinedTextField(
                                value = signUpConfirmPassword,
                                onValueChange = { signUpConfirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Text(if (confirmPasswordVisible) "HIDE" else "SHOW", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password_input")
                            )
                            
                            Button(
                                onClick = {
                                    if (signUpName.isBlank() || signUpEmail.isBlank() || signUpPassword.isBlank()) {
                                        Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(signUpEmail).matches()) {
                                        Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signUpPassword.length < 6) {
                                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signUpPassword != signUpConfirmPassword) {
                                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    // Save to shared preferences
                                    sharedPrefs.edit()
                                        .putString("email_${signUpEmail}", signUpEmail)
                                        .putString("pass_${signUpEmail}", signUpPassword)
                                        .putString("name_${signUpEmail}", signUpName)
                                        .apply()
                                        
                                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess(signUpEmail, signUpName)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("signup_submit_button")
                            ) {
                                Text("Create Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // LOGIN FORM
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            
                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("login_email_input")
                            )
                            
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Text(if (passwordVisible) "HIDE" else "SHOW", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                            )
                            
                            Button(
                                onClick = {
                                    if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    // Verify from SharedPrefs
                                    val registeredPass = sharedPrefs.getString("pass_${loginEmail}", null)
                                    val registeredName = sharedPrefs.getString("name_${loginEmail}", null)
                                    
                                    // Provide fallback testing credentials if nothing registered yet
                                    if (registeredPass == null && loginEmail == "test@example.com" && loginPassword == "password") {
                                        onAuthSuccess("test@example.com", "Test User")
                                        Toast.makeText(context, "Logged in as test user", Toast.LENGTH_SHORT).show()
                                    } else if (registeredPass == loginPassword && registeredName != null) {
                                        onAuthSuccess(loginEmail, registeredName)
                                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_submit_button")
                            ) {
                                Text("Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateShowcaseDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Premium Templates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Text(
                    text = "Pick a high-performing layout tailored to your target industry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Template List
                val templates = listOf(
                    Triple("MODERN", "Teal Minimalist style. Ideal for corporate, finance, and management roles.", Color(0xFF0D9488)),
                    Triple("CREATIVE", "Modern split column layout with rich slate-blue sidebar. Ideal for designers, media, and startups.", Color(0xFFFB923C)),
                    Triple("CLASSIC", "Elegant serif layout with traditional academic alignment. Ideal for teaching, law, and research.", Color(0xFF1E293B)),
                    Triple("TECH", "Developer layout with monospace sub-headers and clean lists. Ideal for engineers and IT professionals.", Color(0xFF6750A4))
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(templates) { (style, description, colorAccent) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTemplate(style)
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, colorAccent.copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colorAccent)
                                        )
                                        Text(
                                            text = style,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D1B20)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colorAccent.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "PREVIEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colorAccent
                                        )
                                    }
                                }
                                
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF49454F)
                                )
                                
                                Button(
                                    onClick = { onSelectTemplate(style) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorAccent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Use $style Template", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
