package com.quillforge.api.blog.controller;

import com.quillforge.api.blog.dto.BlogCategoryDto;
import com.quillforge.api.blog.dto.BlogRequest;
import com.quillforge.api.blog.dto.BlogResponse;
import com.quillforge.api.blog.service.BlogService;
import com.quillforge.api.blog.service.BlogImportService;
import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin/blogs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Blogs Management", description = "Endpoints for administrators to manage blog posts and categories")
@RequiredArgsConstructor
public class AdminBlogController {

    private final BlogService blogService;
    private final BlogImportService blogImportService;

    @PostMapping("/import")
    @Operation(summary = "Import blog post from .docx file")
    public ResponseEntity<ApiResponse<BlogResponse>> importBlog(@RequestParam("file") MultipartFile file) {
        BlogResponse response = blogImportService.importDocx(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog post imported successfully", response));
    }

    @GetMapping("/import/template")
    @Operation(summary = "Download MS Word docx writing guide template")
    public ResponseEntity<org.springframework.core.io.Resource> downloadTemplate() {
        try {
            org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();

            // Metadata Headers
            addTemplateLine(doc, "Meta Title: Comprehensive Guide to MS Word Blog Imports");
            addTemplateLine(doc, "Meta Description: Learn how to author structured blog articles using Microsoft Word documents.");
            addTemplateLine(doc, "URL: guide-to-word-blog-imports");
            addTemplateLine(doc, "Cover Image Asset ID: 00000000-0000-0000-0000-000000000000");
            addTemplateLine(doc, "Excerpt: A step-by-step tutorial on structuring headings, media blocks, embeds, code snips, and CTA boxes directly in MS Word.");
            addTemplateLine(doc, "Featured: true");
            addTemplateLine(doc, "Publish: draft");
            addTemplateParagraph(doc, ""); // empty line spacing

            // Article Title
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setStyle("Heading 1");
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("Mastering Word-to-Blog Importer");
            titleRun.setBold(true);
            titleRun.setFontSize(22);

            // Intro Section
            addTemplateParagraph(doc, "Section: Introduction");
            addTemplateParagraph(doc, "Writing blog articles inside MS Word is highly convenient. This guide walks you through formatting sections, call-out CTA highlights, embed frames, and summary content containers.");

            // Subsection
            addTemplateParagraph(doc, "Sub-Section: Getting Started");
            addTemplateParagraph(doc, "To begin, always ensure the top of your document contains the metadata keys delimited with a colon. Next, use the 'Section:' keyword prefix to divide your publication areas.");

            // CTA Block
            addTemplateParagraph(doc, "CTA: Try QuillForge Today");
            addTemplateLine(doc, "cta title: Get Started with QuillForge");
            addTemplateLine(doc, "cta desc: Experience premium headless content management with native S3 uploads, cache invalidators, and MS Word imports.");
            addTemplateLine(doc, "cta button text: Start Free Trial");
            addTemplateLine(doc, "cta button type: primary");
            addTemplateLine(doc, "cta text color: #ffffff");
            addTemplateParagraph(doc, "");

            // Code Snip Block
            addTemplateParagraph(doc, "Code Block: Sample Java Rest Controller");
            addTemplateLine(doc, "code language: java");
            addTemplateLine(doc, "code block: @GetMapping(\"/hello\")\npublic String hello() {\n    return \"Hello World\";\n}");
            addTemplateParagraph(doc, "");

            // Summary Block
            addTemplateParagraph(doc, "Summary: Wrap-Up");
            addTemplateLine(doc, "summary button text: Back to Dashboard");
            addTemplateLine(doc, "summary button type: secondary");
            addTemplateParagraph(doc, "By conforming to these metadata keys and structural tags, your Word documents will compile seamlessly into the database.");

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            doc.write(out);
            doc.close();

            byte[] bytes = out.toByteArray();
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(bytes);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=blog_writing_template.docx")
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .contentLength(bytes.length)
                    .body(resource);
        } catch (Exception e) {
            throw new BadRequestException("Could not generate writing guide template: " + e.getMessage());
        }
    }

    private void addTemplateLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily("Calibri");
        r.setFontSize(11);
    }

    private void addTemplateParagraph(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily("Calibri");
        r.setFontSize(11);
    }

    @PostMapping
    @Operation(summary = "Create blog post")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(@RequestBody @Valid BlogRequest request) {
        BlogResponse response = blogService.createBlog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update blog post")
    public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(@PathVariable UUID id, @RequestBody @Valid BlogRequest request) {
        BlogResponse response = blogService.updateBlog(id, request);
        return ResponseEntity.ok(ApiResponse.success("Blog updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blog post by ID (admin)")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogById(@PathVariable UUID id) {
        BlogResponse response = blogService.getBlogById(id);
        return ResponseEntity.ok(ApiResponse.success("Blog retrieved successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete blog post")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable UUID id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok(ApiResponse.success("Blog soft-deleted successfully"));
    }

    // Categories (Admin)
    @PostMapping("/categories")
    @Operation(summary = "Create blog category")
    public ResponseEntity<ApiResponse<BlogCategoryDto>> createCategory(@RequestBody @Valid BlogCategoryDto dto) {
        BlogCategoryDto response = blogService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog category created successfully", response));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update blog category")
    public ResponseEntity<ApiResponse<BlogCategoryDto>> updateCategory(@PathVariable UUID id, @RequestBody @Valid BlogCategoryDto dto) {
        BlogCategoryDto response = blogService.updateCategory(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Blog category updated successfully", response));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete blog category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        blogService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Blog category soft-deleted successfully"));
    }
}
