package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.ResumeWithDetails

object PdfExporter {

    fun exportToPdf(context: Context, resume: ResumeWithDetails) {
        val html = generateHtml(resume)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "${resume.resume.fullName.replace(" ", "_")}_Resume"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun generateHtml(resume: ResumeWithDetails): String {
        val style = resume.resume.templateStyle.uppercase()
        val fullName = resume.resume.fullName
        val jobTitle = resume.resume.targetJobTitle
        val email = resume.resume.email
        val phone = resume.resume.phone
        val website = resume.resume.website
        val location = resume.resume.location
        val summary = resume.resume.summary

        val experiencesHtml = resume.experiences.joinToString("") {
            """
            <div class="item">
                <div class="item-header">
                    <span class="item-title">${it.jobTitle}</span>
                    <span class="item-date">${it.startDate} - ${it.endDate}</span>
                </div>
                <div class="item-sub">${it.company}</div>
                <div class="item-desc">${it.description.replace("\n", "<br/>")}</div>
            </div>
            """.trimIndent()
        }

        val educationHtml = resume.educations.joinToString("") {
            """
            <div class="item">
                <div class="item-header">
                    <span class="item-title">${it.degree}</span>
                    <span class="item-date">${it.startDate} - ${it.endDate}</span>
                </div>
                <div class="item-sub">${it.school}</div>
                ${if (it.gpa.isNotEmpty()) "<div class='item-gpa'>GPA: ${it.gpa}</div>" else ""}
            </div>
            """.trimIndent()
        }

        val projectsHtml = resume.projects.joinToString("") {
            """
            <div class="item">
                <div class="item-header">
                    <span class="item-title">${it.name}</span>
                    ${if (it.url.isNotEmpty()) "<span class='item-link'>${it.url}</span>" else ""}
                </div>
                <div class="item-sub">Tech Stack: ${it.technologies}</div>
                <div class="item-desc">${it.description.replace("\n", "<br/>")}</div>
            </div>
            """.trimIndent()
        }

        val skillsHtml = resume.skills.joinToString("") {
            """<span class="skill-tag">${it.name} ${if (it.level.isNotEmpty()) "(${it.level})" else ""}</span>"""
        }

        val css = when (style) {
            "CREATIVE" -> """
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    color: #2D3748;
                    margin: 0;
                    padding: 0;
                    background-color: #FFFFFF;
                }
                .container {
                    display: flex;
                    min-height: 297mm;
                }
                .sidebar {
                    width: 32%;
                    background-color: #2D3748;
                    color: #FFFFFF;
                    padding: 30px 20px;
                    box-sizing: border-box;
                }
                .main-content {
                    width: 68%;
                    padding: 30px;
                    box-sizing: border-box;
                }
                .name {
                    font-size: 26px;
                    font-weight: bold;
                    color: #FF8C00;
                    margin-bottom: 5px;
                }
                .title {
                    font-size: 16px;
                    font-weight: 500;
                    color: #E2E8F0;
                    margin-bottom: 30px;
                }
                .sidebar-section {
                    margin-bottom: 30px;
                }
                .sidebar-title {
                    font-size: 14px;
                    text-transform: uppercase;
                    border-bottom: 2px solid #FF8C00;
                    padding-bottom: 5px;
                    margin-bottom: 15px;
                    font-weight: bold;
                    letter-spacing: 1px;
                }
                .contact-item {
                    margin-bottom: 10px;
                    font-size: 12px;
                    word-break: break-all;
                }
                .section {
                    margin-bottom: 25px;
                }
                .section-title {
                    font-size: 16px;
                    text-transform: uppercase;
                    color: #2D3748;
                    border-bottom: 2px solid #FF8C00;
                    padding-bottom: 5px;
                    margin-bottom: 15px;
                    font-weight: bold;
                    letter-spacing: 1px;
                }
                .summary-text {
                    font-size: 13px;
                    line-height: 1.6;
                    margin-bottom: 20px;
                }
                .item {
                    margin-bottom: 18px;
                }
                .item-header {
                    display: flex;
                    justify-content: space-between;
                    font-weight: bold;
                    font-size: 13px;
                }
                .item-title {
                    color: #2D3748;
                }
                .item-date {
                    color: #718096;
                    font-size: 11px;
                }
                .item-sub {
                    font-style: italic;
                    color: #4A5568;
                    font-size: 12px;
                    margin-bottom: 4px;
                }
                .item-desc {
                    font-size: 12px;
                    line-height: 1.5;
                    color: #4A5568;
                }
                .sidebar-skill {
                    display: inline-block;
                    background-color: #4A5568;
                    padding: 4px 8px;
                    margin: 4px;
                    border-radius: 4px;
                    font-size: 11px;
                }
            """.trimIndent()

            "CLASSIC" -> """
                body {
                    font-family: 'Times New Roman', Georgia, serif;
                    color: #111111;
                    margin: 0;
                    padding: 40px;
                    background-color: #FFFFFF;
                }
                .header {
                    text-align: center;
                    margin-bottom: 25px;
                }
                .name {
                    font-size: 28px;
                    font-weight: normal;
                    letter-spacing: 1px;
                    margin-bottom: 5px;
                }
                .title {
                    font-size: 14px;
                    font-style: italic;
                    color: #555555;
                    margin-bottom: 15px;
                }
                .contact {
                    font-size: 11px;
                    color: #444444;
                    margin-bottom: 15px;
                }
                .section {
                    margin-bottom: 25px;
                }
                .section-title {
                    font-size: 13px;
                    text-transform: uppercase;
                    border-bottom: 1px solid #111111;
                    padding-bottom: 3px;
                    margin-bottom: 12px;
                    font-weight: bold;
                    letter-spacing: 1.5px;
                }
                .summary-text {
                    font-size: 12px;
                    line-height: 1.6;
                    text-align: justify;
                }
                .item {
                    margin-bottom: 15px;
                }
                .item-header {
                    display: flex;
                    justify-content: space-between;
                    font-weight: bold;
                    font-size: 12px;
                }
                .item-date {
                    font-weight: normal;
                }
                .item-sub {
                    font-style: italic;
                    font-size: 12px;
                    margin-bottom: 3px;
                }
                .item-desc {
                    font-size: 12px;
                    line-height: 1.5;
                    text-align: justify;
                }
                .skill-tag {
                    display: inline-block;
                    margin-right: 15px;
                    font-size: 12px;
                }
                .skill-tag::after {
                    content: " • ";
                    margin-left: 15px;
                }
                .skill-tag:last-child::after {
                    content: "";
                }
            """.trimIndent()

            "TECH" -> """
                body {
                    font-family: 'Courier New', Courier, monospace;
                    color: #1A202C;
                    margin: 0;
                    padding: 30px;
                    background-color: #FFFFFF;
                }
                .header {
                    border-bottom: 3px double #2D3748;
                    padding-bottom: 15px;
                    margin-bottom: 25px;
                }
                .name {
                    font-size: 24px;
                    font-weight: bold;
                    color: #008080;
                }
                .title {
                    font-size: 14px;
                    color: #4A5568;
                    margin-top: 5px;
                }
                .contact {
                    font-size: 11px;
                    color: #718096;
                    margin-top: 5px;
                }
                .section {
                    margin-bottom: 25px;
                }
                .section-title {
                    font-size: 14px;
                    color: #FFFFFF;
                    background-color: #008080;
                    padding: 4px 8px;
                    font-weight: bold;
                    margin-bottom: 15px;
                }
                .summary-text {
                    font-size: 12px;
                    line-height: 1.5;
                }
                .item {
                    margin-bottom: 18px;
                    border-left: 2px solid #008080;
                    padding-left: 10px;
                }
                .item-header {
                    display: flex;
                    justify-content: space-between;
                    font-weight: bold;
                    font-size: 12px;
                }
                .item-sub {
                    font-size: 11px;
                    color: #4A5568;
                    margin-bottom: 4px;
                    font-weight: bold;
                }
                .item-desc {
                    font-size: 11px;
                    line-height: 1.4;
                }
                .skill-tag {
                    display: inline-block;
                    background-color: #E6FFFA;
                    border: 1px solid #319795;
                    color: #234E52;
                    padding: 2px 6px;
                    margin: 3px;
                    border-radius: 2px;
                    font-size: 11px;
                }
            """.trimIndent()

            else -> """
                /* MODERN DEFAULT */
                body {
                    font-family: 'Segoe UI', Roboto, sans-serif;
                    color: #2D3748;
                    margin: 0;
                    padding: 35px;
                    background-color: #FFFFFF;
                }
                .header {
                    border-left: 6px solid #319795;
                    padding-left: 15px;
                    margin-bottom: 25px;
                }
                .name {
                    font-size: 26px;
                    font-weight: 800;
                    color: #1A202C;
                }
                .title {
                    font-size: 15px;
                    font-weight: 600;
                    color: #319795;
                    margin-top: 4px;
                }
                .contact {
                    font-size: 11px;
                    color: #718096;
                    margin-top: 8px;
                    line-height: 1.4;
                }
                .section {
                    margin-bottom: 25px;
                }
                .section-title {
                    font-size: 14px;
                    text-transform: uppercase;
                    color: #319795;
                    border-bottom: 2px solid #E2E8F0;
                    padding-bottom: 4px;
                    margin-bottom: 12px;
                    font-weight: 700;
                    letter-spacing: 1px;
                }
                .summary-text {
                    font-size: 12px;
                    line-height: 1.6;
                    color: #4A5568;
                }
                .item {
                    margin-bottom: 16px;
                }
                .item-header {
                    display: flex;
                    justify-content: space-between;
                    font-weight: 600;
                    font-size: 12px;
                }
                .item-title {
                    color: #1A202C;
                }
                .item-date {
                    color: #A0AEC0;
                    font-size: 11px;
                }
                .item-sub {
                    font-weight: 500;
                    color: #4A5568;
                    font-size: 12px;
                    margin-bottom: 3px;
                }
                .item-desc {
                    font-size: 11px;
                    line-height: 1.5;
                    color: #4A5568;
                }
                .skill-tag {
                    display: inline-block;
                    background-color: #EDF2F7;
                    color: #2D3748;
                    padding: 4px 10px;
                    margin: 3px;
                    border-radius: 12px;
                    font-size: 11px;
                }
            """.trimIndent()
        }

        return if (style == "CREATIVE") {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>$css</style>
            </head>
            <body>
                <div class="container">
                    <div class="sidebar">
                        <div class="name">$fullName</div>
                        <div class="title">$jobTitle</div>
                        
                        <div class="sidebar-section">
                            <div class="sidebar-title">Contact</div>
                            <div class="contact-item">📧 $email</div>
                            <div class="contact-item">📞 $phone</div>
                            <div class="contact-item">📍 $location</div>
                            ${if (website.isNotEmpty()) "<div class='contact-item'>🌐 $website</div>" else ""}
                        </div>
                        
                        <div class="sidebar-section">
                            <div class="sidebar-title">Skills</div>
                            ${resume.skills.joinToString("") { "<div class='sidebar-skill'>${it.name} ${if (it.level.isNotEmpty()) "(${it.level})" else ""}</div>" }}
                        </div>
                    </div>
                    <div class="main-content">
                        <div class="section">
                            <div class="section-title">Summary</div>
                            <div class="summary-text">$summary</div>
                        </div>
                        
                        ${if (resume.experiences.isNotEmpty()) """
                        <div class="section">
                            <div class="section-title">Experience</div>
                            $experiencesHtml
                        </div>
                        """ else ""}
                        
                        ${if (resume.educations.isNotEmpty()) """
                        <div class="section">
                            <div class="section-title">Education</div>
                            $educationHtml
                        </div>
                        """ else ""}
                        
                        ${if (resume.projects.isNotEmpty()) """
                        <div class="section">
                            <div class="section-title">Projects</div>
                            $projectsHtml
                        </div>
                        """ else ""}
                    </div>
                </div>
            </body>
            </html>
            """.trimIndent()
        } else {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>$css</style>
            </head>
            <body>
                <div class="header">
                    <div class="name">$fullName</div>
                    <div class="title">$jobTitle</div>
                    <div class="contact">
                        📍 $location &nbsp;|&nbsp; 📧 $email &nbsp;|&nbsp; 📞 $phone 
                        ${if (website.isNotEmpty()) "&nbsp;|&nbsp; 🌐 $website" else ""}
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">Professional Summary</div>
                    <div class="summary-text">$summary</div>
                </div>
                
                ${if (resume.experiences.isNotEmpty()) """
                <div class="section">
                    <div class="section-title">Work Experience</div>
                    $experiencesHtml
                </div>
                """ else ""}
                
                ${if (resume.educations.isNotEmpty()) """
                <div class="section">
                    <div class="section-title">Education</div>
                    $educationHtml
                </div>
                """ else ""}
                
                ${if (resume.projects.isNotEmpty()) """
                <div class="section">
                    <div class="section-title">Projects</div>
                    $projectsHtml
                </div>
                """ else ""}
                
                ${if (resume.skills.isNotEmpty()) """
                <div class="section">
                    <div class="section-title">Key Skills</div>
                    <div style="margin-top: 5px;">$skillsHtml</div>
                </div>
                """ else ""}
            </body>
            </html>
            """.trimIndent()
        }
    }
}
