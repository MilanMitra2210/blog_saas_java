package com.quillforge.api.common.config;

import com.quillforge.api.blog.entity.*;
import com.quillforge.api.blog.repository.*;
import com.quillforge.api.cms.entity.CMSPage;
import com.quillforge.api.cms.repository.CMSPageRepository;
import com.quillforge.api.common.entity.SeoMetadata;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.repository.MediaRepository;
import com.quillforge.api.user.entity.User;
import com.quillforge.api.user.entity.User.ProviderEnum;
import com.quillforge.api.user.entity.User.RoleEnum;
import com.quillforge.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaRepository mediaRepository;
    private final BlogCategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final BlogAuthorRepository authorRepository;
    private final BlogRepository blogRepository;
    private final BlogCommentRepository commentRepository;
    private final CMSPageRepository cmsPageRepository;

    @Override
    public void run(String... args) {
        String adminEmail = "milanmitra2204@gmail.com";
        User admin = userRepository.findByEmail(adminEmail).orElseGet(() -> {
            log.info("🚀 Seeding default admin user: {}", adminEmail);
            User newAdmin = new User();
            newAdmin.setName("Milan Mitra");
            newAdmin.setEmail(adminEmail);
            newAdmin.setPassword(passwordEncoder.encode("AdminPassword123"));
            newAdmin.setRole(RoleEnum.SUPER_ADMIN);
            newAdmin.setActive(true);
            newAdmin.setProvider(ProviderEnum.MANUAL);
            return userRepository.save(newAdmin);
        });

        // 1. Seed Media
        if (mediaRepository.count() == 0) {
            log.info("🚀 Seeding Media items...");
            UUID folderId = null;

            Media m1 = createMedia("blue-almirah-open.png", "uploads/f48a6a50-fdcf-4421-b97e-16b81ffc65f3.png",
                    "https://75way-web.sfo3.cdn.digitaloceanspaces.com/uploads/f48a6a50-fdcf-4421-b97e-16b81ffc65f3.png",
                    2371461L, "image/png", folderId, 1024, 1536, admin);

            Media m2 = createMedia("yellow-almirah-open.png", "uploads/fb3e90ee-abf7-41ad-9c54-6bcac26188e8.png",
                    "https://75way-web.sfo3.cdn.digitaloceanspaces.com/uploads/fb3e90ee-abf7-41ad-9c54-6bcac26188e8.png",
                    2203209L, "image/png", folderId, 1024, 1536, admin);

            Media m3 = createMedia("gray-almirah.png", "uploads/cee6cfd3-eb59-48d8-896f-376ae2a8d6f6.png",
                    "https://75way-web.sfo3.cdn.digitaloceanspaces.com/uploads/cee6cfd3-eb59-48d8-896f-376ae2a8d6f6.png",
                    1143410L, "image/png", folderId, 804, 1536, admin);

            Media m4 = createMedia("blue-almirah.png", "uploads/339bf776-c3d5-47f8-bdda-5057aa35d163.png",
                    "https://75way-web.sfo3.cdn.digitaloceanspaces.com/uploads/339bf776-c3d5-47f8-bdda-5057aa35d163.png",
                    920234L, "image/png", folderId, 804, 1536, admin);

            Media m5 = createMedia("yellow-almirah.png", "uploads/135b5c53-6046-4917-9617-73babd3ce257.png",
                    "https://75way-web.sfo3.cdn.digitaloceanspaces.com/uploads/135b5c53-6046-4917-9617-73babd3ce257.png",
                    1220360L, "image/png", folderId, 804, 1536, admin);

            Media m6 = createMedia("gray-almirah-open.png", "uploads/cd66a46d-9d5c-447b-91b7-95970dc1c978.png",
                    "https://75way-web.sfo3.cdn.digitaloceanspaces.com/uploads/cd66a46d-9d5c-447b-91b7-95970dc1c978.png",
                    2495603L, "image/png", folderId, 1024, 1536, admin);

            List<Media> seededMedia = mediaRepository.saveAll(Arrays.asList(m1, m2, m3, m4, m5, m6));
            log.info("✅ Seeded {} Media files", seededMedia.size());
        }

        // 2. Seed Blog Categories
        if (categoryRepository.count() == 0) {
            log.info("🚀 Seeding Blog Categories...");
            BlogCategory c1 = new BlogCategory();
            c1.setName("Design Trends");
            c1.setSlug("design-trends");
            c1.setActive(true);
            c1.setCreatedBy(admin);

            BlogCategory c2 = new BlogCategory();
            c2.setName("Furniture & Interiors");
            c2.setSlug("furniture-interiors");
            c2.setActive(true);
            c2.setCreatedBy(admin);

            categoryRepository.saveAll(Arrays.asList(c1, c2));
        }

        // 3. Seed Tags
        if (tagRepository.count() == 0) {
            log.info("🚀 Seeding Tags...");
            Tag t1 = new Tag();
            t1.setName("Modern Living");
            t1.setSlug("modern-living");
            t1.setCreatedBy(admin);

            Tag t2 = new Tag();
            t2.setName("Minimalism");
            t2.setSlug("minimalism");
            t2.setCreatedBy(admin);

            tagRepository.saveAll(Arrays.asList(t1, t2));
        }

        // 4. Seed Blog Author
        if (authorRepository.count() == 0) {
            log.info("🚀 Seeding Blog Author...");
            Media authorAvatar = mediaRepository.findAll().stream().findFirst().orElse(null);
            BlogAuthor author = new BlogAuthor();
            author.setName("Milan Mitra");
            author.setDesignation("Lead Interior Architect & Editor");
            author.setBio("Specializing in luxury residential design, contemporary aesthetics, and sustainable crafting.");
            author.setImage(authorAvatar);
            author.setCreatedBy(admin);
            authorRepository.save(author);
        }

        // 5. Seed Blogs & Comments
        if (blogRepository.count() == 0) {
            log.info("🚀 Seeding Blogs...");
            BlogCategory cat = categoryRepository.findAll().get(0);
            BlogAuthor author = authorRepository.findAll().get(0);
            List<Tag> tags = tagRepository.findAll();
            List<Media> mediaList = mediaRepository.findAll();

            Media banner = mediaList.isEmpty() ? null : mediaList.get(0);

            Blog blog = new Blog();
            blog.setTitle("The Art of Modern Almirah & Cabinet Craftsmanship");
            blog.setSlug("art-of-modern-almirah-craftsmanship");
            blog.setExcerpt("Explore how vibrant colors, sleek handles, and premium space optimization transform traditional storage into contemporary art pieces.");
            blog.setPublishDate(Instant.now());
            blog.setReadTime(6);
            blog.setPublished(true);
            blog.setFeatured(true);
            blog.setCategory(cat);
            blog.setAuthor(author);
            blog.setBannerImage(banner);
            blog.setTags(tags);
            blog.setCreatedBy(admin);

            SeoMetadata seo = new SeoMetadata();
            seo.setMetaTitle("Modern Almirah Craftsmanship - QuillForge Interiors");
            seo.setMetaDescription("Discover modern interior storage design trends with custom almirahs and sleek storage elements.");
            blog.setSeo(seo);

            BlogSection section1 = new BlogSection();
            section1.setTitle("1. Bold Color Choices in Modern Furniture");
            section1.setSectionType("content");
            section1.setDisplayOrder(1);
            section1.setMedia(mediaList.size() > 1 ? mediaList.get(1) : null);

            blog.getSections().add(section1);

            Blog savedBlog = blogRepository.save(blog);

            // Seed Comment for Blog
            BlogComment comment = new BlogComment();
            comment.setPostId(savedBlog.getId());
            comment.setAuthorName("Sarah Jenkins");
            comment.setAuthorEmail("sarah.j@example.com");
            comment.setContent("This guide completely changed how I look at wardrobe storage options!");
            comment.setApproved(true);
            comment.setCreatedBy(admin);
            commentRepository.save(comment);

            log.info("✅ Seeded Blog and Comment");
        }

        // 6. Seed CMS Page
        if (cmsPageRepository.count() == 0) {
            log.info("🚀 Seeding CMS Page...");
            CMSPage page = new CMSPage();
            page.setName("About QuillForge");
            page.setSlug("about-us");
            page.setActive(true);
            page.setCreatedBy(admin);

            SeoMetadata pageSeo = new SeoMetadata();
            pageSeo.setMetaTitle("About Us - QuillForge");
            pageSeo.setMetaDescription("Learn more about QuillForge's story, mission, and craftsmanship.");
            page.setSeo(pageSeo);

            cmsPageRepository.save(page);
            log.info("✅ Seeded CMS Page");
        }
    }

    private Media createMedia(String name, String key, String url, Long size, String mimeType, UUID folderId, Integer width, Integer height, User creator) {
        Media m = new Media();
        m.setName(name);
        m.setKey(key);
        m.setUrl(url);
        m.setSize(size);
        m.setMimeType(mimeType);
        m.setFolderId(folderId);
        m.setWidth(width);
        m.setHeight(height);
        m.setCreatedBy(creator);
        return m;
    }
}
