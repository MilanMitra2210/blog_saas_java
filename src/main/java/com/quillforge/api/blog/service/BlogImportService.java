package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogRequest;
import com.quillforge.api.blog.dto.BlogResponse;
import com.quillforge.api.blog.dto.BlogSectionDto;
import com.quillforge.api.blog.dto.BlogSubSectionDto;
import com.quillforge.api.common.dto.SeoDto;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.blog.repository.BlogAuthorRepository;
import com.quillforge.api.blog.repository.BlogCategoryRepository;
import com.quillforge.api.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogImportService {

    private final BlogCategoryRepository blogCategoryRepository;
    private final BlogAuthorRepository blogAuthorRepository;
    private final MediaRepository mediaRepository;
    private final BlogService blogService;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
            "has", "have", "had", "do", "does", "did", "will", "would", "can",
            "could", "should", "may", "might", "this", "that", "these", "those",
            "it", "its", "your", "our", "their", "how", "what", "when", "why",
            "which", "who", "all", "any", "both", "each", "few", "more", "most",
            "other", "some", "such", "than", "too", "very"
    ));

    private static final Pattern KEYWORD_RE = Pattern.compile(
            "^(Section|Sub-Section|Summary|CTA|Media|Embed|Code Block|Quote)\\s*:",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> SPECIAL_PROP_KEYS = new HashSet<>(Arrays.asList(
            "media asset ids", "media alt", "media caption",
            "embed provider", "embed url",
            "code language",
            "quote text", "quote author", "quote designation",
            "cta title", "cta desc", "cta button text", "cta button type",
            "cta text color", "cta background image asset id", "cta list item",
            "summary button text", "summary button type"
    ));

    @org.springframework.transaction.annotation.Transactional
    public BlogResponse importDocx(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {

            // 1. Parse Metadata Header
            ParsedMetadata meta = new ParsedMetadata();
            List<XWPFParagraph> remainingParas = new ArrayList<>();
            boolean headerDone = false;

            for (XWPFParagraph para : doc.getParagraphs()) {
                String style = para.getStyle() != null ? para.getStyle().toLowerCase() : "";
                String text = para.getText().trim();

                if (style.contains("heading 1") || style.contains("title")) {
                    headerDone = true;
                    remainingParas.add(para);
                    continue;
                }

                if (headerDone) {
                    remainingParas.add(para);
                    continue;
                }

                if (text.isEmpty()) {
                    continue;
                }

                if (!text.contains(":")) {
                    headerDone = true;
                    remainingParas.add(para);
                    continue;
                }

                String[] parts = text.split(":", 2);
                String key = parts[0].trim().toLowerCase();
                String val = parts[1].trim();

                String attr = getMetadataKey(key);
                if (attr == null) {
                    headerDone = true;
                    remainingParas.add(para);
                    continue;
                }

                setMetadataValue(meta, attr, val);
            }

            // Validation
            if (meta.slug == null || meta.slug.isEmpty()) {
                throw new BadRequestException("DOCX is missing the required 'URL:' metadata field (the blog slug).");
            }
            if (meta.coverImageAssetId == null || meta.coverImageAssetId.isEmpty()) {
                throw new BadRequestException("DOCX is missing the required 'Cover Image Asset ID:' metadata field.");
            }
            if (meta.metaTitle == null || meta.metaTitle.isEmpty()) {
                throw new BadRequestException("DOCX is missing the required 'Meta Title:' metadata field.");
            }
            if (meta.metaDescription == null || meta.metaDescription.isEmpty()) {
                throw new BadRequestException("DOCX is missing the required 'Meta Description:' metadata field.");
            }

            // 2. Parse Body content
            String blogTitle = null;
            List<ParsedSection> sections = new ArrayList<>();
            ParsedSection currentSection = null;
            ParsedSubSection currentSub = null;
            List<XWPFParagraph> specialLookahead = new ArrayList<>();

            for (int i = 0; i < remainingParas.size(); i++) {
                XWPFParagraph para = remainingParas.get(i);
                String text = para.getText().trim();
                String style = para.getStyle() != null ? para.getStyle().toLowerCase() : "";

                // Check title prefix if Heading 1 not applied
                if (blogTitle == null && (style.contains("heading 1") || style.contains("title") || text.toLowerCase().startsWith("title:") || text.toLowerCase().startsWith("blog title:"))) {
                    if (text.contains(":")) {
                        blogTitle = text.substring(text.indexOf(":") + 1).trim();
                    } else {
                        blogTitle = text;
                    }
                    continue;
                }

                Matcher km = KEYWORD_RE.matcher(text);
                if (km.find()) {
                    String keyword = km.group(1).toLowerCase();
                    String value = text.substring(km.end()).trim();

                    // Flush special block lookahead properties
                    specialLookahead.clear();
                    for (int j = i + 1; j < remainingParas.size(); j++) {
                        XWPFParagraph nextPara = remainingParas.get(j);
                        String nextText = nextPara.getText().trim();
                        if (nextText.contains(":")) {
                            String possibleKey = nextText.split(":", 2)[0].trim().toLowerCase();
                            if (SPECIAL_PROP_KEYS.contains(possibleKey)) {
                                specialLookahead.add(nextPara);
                                i++; // skip processing in main loop
                                continue;
                            }
                        }
                        break;
                    }

                    if ("section".equals(keyword)) {
                        currentSection = new ParsedSection();
                        currentSection.title = value;
                        currentSection.sectionType = "content";
                        sections.add(currentSection);
                        currentSub = null;
                    } else if ("sub-section".equals(keyword)) {
                        if (currentSection == null) {
                            currentSection = new ParsedSection();
                            currentSection.sectionType = "content";
                            sections.add(currentSection);
                        }
                        currentSub = new ParsedSubSection();
                        currentSub.title = value;
                        currentSection.subSections.add(currentSub);
                    } else {
                        // Special block (media, cta, embed, code, quote, summary)
                        ParsedSection specSec = new ParsedSection();
                        specSec.sectionType = keyword.replace(" block", "");
                        specSec.title = value;
                        parseSpecialBlockProperties(specSec, specialLookahead);
                        sections.add(specSec);
                        currentSection = specSec;
                        currentSub = null;
                    }
                    continue;
                }

                if (text.isEmpty()) continue;

                // Add text block to current section or subsection
                Map<String, Object> block = new HashMap<>();
                block.put("type", "paragraph");
                
                List<Map<String, Object>> children = new ArrayList<>();
                for (XWPFRun run : para.getRuns()) {
                    String runText = run.getText(0);
                    if (runText == null) continue;
                    Map<String, Object> leaf = new HashMap<>();
                    leaf.put("type", "text");
                    leaf.put("text", runText);
                    if (run.isBold()) leaf.put("bold", true);
                    if (run.isItalic()) leaf.put("italic", true);
                    if (run.getUnderline() != UnderlinePatterns.NONE) leaf.put("underline", true);
                    children.add(leaf);
                }
                block.put("children", children);

                if (currentSub != null) {
                    currentSub.contentBlocks.add(block);
                } else {
                    if (currentSection == null) {
                        currentSection = new ParsedSection();
                        currentSection.sectionType = "content";
                        sections.add(currentSection);
                    }
                    currentSection.contentBlocks.add(block);
                }
            }

            if (blogTitle == null) {
                blogTitle = meta.metaTitle;
            }

            // Resolve dependencies
            UUID bannerImageId = resolveMediaId(meta.coverImageAssetId);
            UUID seoMetaImageId = resolveMediaId(meta.seoMetaImageAssetId != null ? meta.seoMetaImageAssetId : meta.coverImageAssetId);
            UUID categoryId = resolveCategoryId(meta.categoryId);
            UUID authorId = resolveAuthorId(meta.authorId);

            // Compute auto keywords
            String keywords = generateKeywords(blogTitle, meta.metaTitle, meta.metaDescription);

            // Setup SEO
            SeoDto seo = new SeoDto();
            seo.setMetaTitle(meta.metaTitle);
            seo.setMetaDescription(meta.metaDescription);
            seo.setMetaKeywords(keywords);
            seo.setMetaRobots("index, follow");
            seo.setMetaImageId(seoMetaImageId);

            // Estimate reading time
            int readTime = meta.readTimeMinutes != null ? meta.readTimeMinutes : estimateReadTime(sections);

            // Map Sections DTOs
            List<BlogSectionDto> sectionDtos = new ArrayList<>();
            for (int sIdx = 0; sIdx < sections.size(); sIdx++) {
                ParsedSection s = sections.get(sIdx);
                BlogSectionDto sDto = new BlogSectionDto();
                sDto.setTitle(s.title);
                sDto.setSectionType(s.sectionType);
                sDto.setDisplayOrder(sIdx);
                
                // Map special properties into section dto content body
                sDto.setContent(buildSectionContentField(s));
                
                if (s.mediaId != null) {
                    com.quillforge.api.media.dto.MediaDto mDto = new com.quillforge.api.media.dto.MediaDto();
                    mDto.setId(s.mediaId);
                    sDto.setMedia(mDto);
                }
                sDto.setCtaButtonText(s.ctaButtonText);
                sDto.setCtaButtonUrl(s.ctaButtonUrl);
                sDto.setEmbedUrl(s.embedUrl);
                sDto.setEmbedProvider(s.embedProvider);
                sDto.setCodeBlock(s.codeBlock);
                sDto.setCodeLanguage(s.codeLanguage);

                // Sub-sections
                List<BlogSubSectionDto> subDtos = new ArrayList<>();
                for (int subIdx = 0; subIdx < s.subSections.size(); subIdx++) {
                    ParsedSubSection sub = s.subSections.get(subIdx);
                    BlogSubSectionDto subDto = new BlogSubSectionDto();
                    subDto.setTitle(sub.title);
                    subDto.setContent(sub.contentBlocks);
                    subDto.setDisplayOrder(subIdx);
                    subDtos.add(subDto);
                }
                sDto.setSubSections(subDtos);
                sectionDtos.add(sDto);
            }

            // Create blog request
            BlogRequest req = new BlogRequest();
            req.setTitle(blogTitle);
            req.setSlug(meta.slug);
            req.setExcerpt(meta.excerpt);
            req.setPublishDate(Instant.now());
            req.setReadTime(readTime);
            req.setPublished("published".equalsIgnoreCase(meta.publish));
            req.setFeatured(meta.featured);
            req.setCategoryId(categoryId);
            req.setAuthorId(authorId);
            req.setBannerImageId(bannerImageId);
            req.setSeo(seo);
            req.setSections(sectionDtos);

            return blogService.createBlog(req);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Word Document blog post", e);
            throw new BadRequestException("Failed to import blog post: [" + e.getClass().getSimpleName() + "] " + e.getMessage());
        }
    }

    // Helper parsers & lookups

    private static class ParsedMetadata {
        String metaTitle;
        String metaDescription;
        String slug;
        String coverImageAssetId;
        String seoMetaImageAssetId;
        String categoryId;
        String authorId;
        String excerpt;
        Integer readTimeMinutes;
        boolean featured = false;
        String publish = "draft";
    }

    private static class ParsedSection {
        String sectionType = "content";
        String title;
        List<Object> contentBlocks = new ArrayList<>();
        List<ParsedSubSection> subSections = new ArrayList<>();

        UUID mediaId;
        String embedUrl;
        String embedProvider;
        String codeBlock;
        String codeLanguage;
        String ctaButtonText;
        String ctaButtonUrl;

        // Custom CTA elements
        String ctaTitle;
        String ctaDesc;
        String ctaTextColor;
        List<String> ctaListItems = new ArrayList<>();

        // Summary elements
        String summaryButtonText;
        String summaryButtonType;
    }

    private static class ParsedSubSection {
        String title;
        List<Object> contentBlocks = new ArrayList<>();
    }

    private String getMetadataKey(String key) {
        return switch (key) {
            case "meta title" -> "metaTitle";
            case "meta description" -> "metaDescription";
            case "url" -> "slug";
            case "cover image asset id" -> "coverImageAssetId";
            case "seo meta image asset id" -> "seoMetaImageAssetId";
            case "category id" -> "categoryId";
            case "author id" -> "authorId";
            case "excerpt" -> "excerpt";
            case "read time minutes", "read time" -> "readTimeMinutes";
            case "featured" -> "featured";
            case "publish" -> "publish";
            default -> null;
        };
    }

    private void setMetadataValue(ParsedMetadata meta, String attr, String val) {
        switch (attr) {
            case "metaTitle" -> meta.metaTitle = val;
            case "metaDescription" -> meta.metaDescription = val;
            case "slug" -> meta.slug = val;
            case "coverImageAssetId" -> meta.coverImageAssetId = val;
            case "seoMetaImageAssetId" -> meta.seoMetaImageAssetId = val;
            case "categoryId" -> meta.categoryId = val;
            case "authorId" -> meta.authorId = val;
            case "excerpt" -> meta.excerpt = val;
            case "readTimeMinutes" -> {
                try {
                    meta.readTimeMinutes = Integer.parseInt(val.replaceAll("[^0-9]", ""));
                } catch (Exception e) {
                    meta.readTimeMinutes = null;
                }
            }
            case "featured" -> meta.featured = "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val);
            case "publish" -> meta.publish = "published".equalsIgnoreCase(val) ? "published" : "draft";
        }
    }

    private UUID resolveMediaId(String rawId) {
        if (rawId == null || rawId.trim().isEmpty()) return null;
        try {
            UUID uid = UUID.fromString(rawId.trim());
            return mediaRepository.findById(uid).isPresent() ? uid : null;
        } catch (Exception e) {
            return null;
        }
    }

    private UUID resolveCategoryId(String rawId) {
        if (rawId == null || rawId.trim().isEmpty()) return null;
        try {
            UUID uid = UUID.fromString(rawId.trim());
            return blogCategoryRepository.findById(uid).isPresent() ? uid : null;
        } catch (Exception e) {
            return null;
        }
    }

    private UUID resolveAuthorId(String rawId) {
        if (rawId == null || rawId.trim().isEmpty()) return null;
        try {
            UUID uid = UUID.fromString(rawId.trim());
            return blogAuthorRepository.findById(uid).isPresent() ? uid : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void parseSpecialBlockProperties(ParsedSection sec, List<XWPFParagraph> props) {
        for (XWPFParagraph p : props) {
            String text = p.getText().trim();
            if (!text.contains(":")) continue;
            String[] parts = text.split(":", 2);
            String k = parts[0].trim().toLowerCase();
            String v = parts[1].trim();

            switch (k) {
                case "media asset ids" -> {
                    String firstId = v.split(",")[0].trim();
                    sec.mediaId = resolveMediaId(firstId);
                }
                case "embed provider" -> sec.embedProvider = v;
                case "embed url" -> sec.embedUrl = v;
                case "code language" -> sec.codeLanguage = v;
                case "quote text" -> sec.codeBlock = v;
                case "quote author" -> sec.ctaButtonText = v;
                case "quote designation" -> sec.ctaButtonUrl = v;
                case "cta title" -> sec.ctaTitle = v;
                case "cta desc" -> sec.ctaDesc = v;
                case "cta button text" -> sec.ctaButtonText = v;
                case "cta button type" -> sec.ctaButtonUrl = v;
                case "cta text color" -> sec.ctaTextColor = v;
                case "cta background image asset id" -> sec.mediaId = resolveMediaId(v);
                case "cta list item" -> sec.ctaListItems.add(v);
                case "summary button text" -> sec.summaryButtonText = v;
                case "summary button type" -> sec.summaryButtonType = v;
            }
        }
    }

    private Object buildSectionContentField(ParsedSection s) {
        Map<String, Object> content = new HashMap<>();
        switch (s.sectionType) {
            case "media" -> {
                content.put("type", "media");
                content.put("assetIds", s.mediaId != null ? List.of(s.mediaId.toString()) : List.of());
            }
            case "embed" -> {
                content.put("type", "embed");
                content.put("provider", s.embedProvider);
                content.put("url", s.embedUrl);
            }
            case "code" -> {
                content.put("type", "code");
                content.put("language", s.codeLanguage);
                content.put("code", s.codeBlock);
            }
            case "quote" -> {
                content.put("type", "quote");
                content.put("text", s.codeBlock); // repurposed field
                content.put("author", s.ctaButtonText); // repurposed field
                content.put("designation", s.ctaButtonUrl); // repurposed field
            }
            case "cta" -> {
                content.put("type", "cta");
                content.put("title", s.ctaTitle);
                content.put("description", s.ctaDesc);
                content.put("buttonText", s.ctaButtonText);
                content.put("textColor", s.ctaTextColor);
                content.put("listItems", s.ctaListItems);
            }
            case "summary" -> {
                content.put("type", "summary");
                content.put("buttonText", s.summaryButtonText);
                content.put("buttonType", s.summaryButtonType);
            }
            default -> {
                return s.contentBlocks;
            }
        }
        return content;
    }

    private int estimateReadTime(List<ParsedSection> sections) {
        int wordCount = 0;
        for (ParsedSection s : sections) {
            wordCount += estimateContentBlocksWordCount(s.contentBlocks);
            for (ParsedSubSection sub : s.subSections) {
                wordCount += estimateContentBlocksWordCount(sub.contentBlocks);
            }
        }
        return Math.max(1, (int) Math.ceil(wordCount / 200.0));
    }

    private int estimateContentBlocksWordCount(List<Object> blocks) {
        int count = 0;
        for (Object b : blocks) {
            if (b instanceof Map) {
                Map<String, Object> block = (Map<String, Object>) b;
                Object childrenObj = block.get("children");
                if (childrenObj instanceof List) {
                    List<?> children = (List<?>) childrenObj;
                    for (Object child : children) {
                        if (child instanceof Map) {
                            Map<?, ?> leaf = (Map<?, ?>) child;
                            Object textObj = leaf.get("text");
                            if (textObj instanceof String) {
                                String text = (String) textObj;
                                count += text.split("\\s+").length;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private String generateKeywords(String title, String mTitle, String mDesc) {
        String raw = (title + " " + (mTitle != null ? mTitle : "") + " " + (mDesc != null ? mDesc : "")).toLowerCase();
        String[] words = raw.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        Set<String> uniqueKeywords = new LinkedHashSet<>();
        for (String w : words) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                uniqueKeywords.add(w);
                if (uniqueKeywords.size() >= 15) break;
            }
        }
        return String.join(", ", uniqueKeywords);
    }
}
