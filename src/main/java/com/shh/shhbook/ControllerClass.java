package com.shh.shhbook;

import com.shh.shhbook.model.Posts;
import com.shh.shhbook.repository.CommentsRepository;
import com.shh.shhbook.repository.PostsRepository;
import com.shh.shhbook.model.Users;
import com.shh.shhbook.service.FtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;

@Controller
public class ControllerClass<Map> {
    private final FtpService ftpService;
    @Autowired
    public ControllerClass(FtpService ftpService) {
        this.ftpService = ftpService;
    }
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PostsRepository postsRepository;
    @Autowired
    private CommentsRepository commentsRepository;
    // strona poczatkowa - logowanie
    @RequestMapping(value={"/", "/search"}, method = RequestMethod.GET)
    public String session(HttpServletRequest request, ModelMap model) {

        //  jezeli bylo wyszukiwanie, to wyswietlamy posty wyszukane
        if (request.getRequestURI().equals("/search"))
        {
            if (request.getMethod().equals("GET")) {
                List<Posts> postsList = postsRepository.findByDescriptionContaining(request.getParameter("search_field"));
                Collections.reverse(postsList);
                model.addAttribute("db_posts", postsList);
            }
        }

        // w przeciwnym wypadku (strona glowna) wyswietlamy wszystkie
        else {
            List<Posts> postsList = postsRepository.findAll();
            Collections.reverse(postsList);
            model.addAttribute("db_posts", postsList);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            model.addAttribute("user", session.getAttribute("user"));
            String sql = "SELECT ID FROM users WHERE username = ?";
            Integer userPermission = jdbcTemplate.queryForObject(sql, new Object[]{session.getAttribute("user")}, Integer.class);
            model.addAttribute("can_post", userPermission);
            return "index";
        }
        return "login";
    }

    // zalogowanie sie i wejscie na strone glowna
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(final HttpServletRequest request,
                       @ModelAttribute("users") Users user,
                       ModelMap model) {
        if (request.getMethod().equals("POST")) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            int count = jdbcTemplate.queryForObject(sql, new Object[]{user.getUsername(), user.getPassword()}, Integer.class);

            if (count > 0) {
                HttpSession session = request.getSession();
                session.setAttribute("user", user.getUsername());
                return "redirect:/";
            } else {
                model.addAttribute("error", "Zły login lub hasło");
                return "login";
            }
        }
        return "login";
    }

    // wylogowanie sie
    @RequestMapping("/logout")
    public String logout(final HttpServletRequest request)
    {
        request.getSession(false).invalidate();
        return "redirect:/";
    }

    // wstawianie posta
    @RequestMapping(value="/post", method = RequestMethod.POST)
    public String post(final HttpServletRequest request,
                       @ModelAttribute("posts") Posts post,
                       @RequestParam("files") List<MultipartFile> files,
                       ModelMap model) {
        if (request.getMethod().equals("POST")) {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Timestamp timestamp = Timestamp.from(now);
            String sql = "SELECT MAX(ID) FROM posts";
            Long lastPostId = jdbcTemplate.queryForObject(sql, Long.class);
            if (lastPostId == null) lastPostId = 0L;
            List<String> filePaths = ftpService.uploadFilesToFTP(files, lastPostId + 1);

            sql = "INSERT INTO posts (username, title, description, files_path, created_at) VALUES (?, ?, ?, ?, ?)";
            String filesCSV = String.join(",", filePaths);
            int inserted = jdbcTemplate.update(sql, request.getSession().getAttribute("user"), post.getTitle(), post.getDescription(), filesCSV, timestamp);
            if (inserted == 0) {
                model.addAttribute("error", "Nie udało się dodać posta.");
            }
        }
        return "redirect:/";
    }

    @RequestMapping(value="/comment", method = RequestMethod.POST)
    public String comment(final HttpServletRequest request,
                          @RequestParam("comment_content") String commentContent,
                          @RequestParam("post_id") int postId,
                          ModelMap model)
    {
        if (request.getMethod().equals("POST")) {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Timestamp timestamp = Timestamp.from(now);
            String sql = "INSERT INTO comments (post_id, comment_content, user, created_at) VALUES (?, ?, ?, ?)";
            int inserted = jdbcTemplate.update(sql, postId, commentContent, request.getSession().getAttribute("user"), timestamp);
            if (inserted == 0)
            {
                model.addAttribute("error", "Nie udało się dodać komentarza.");
            }
        }
        return "redirect:/";
    }

    @RequestMapping(value="/comments/{postId}", method = RequestMethod.GET)
    @ResponseBody
    public List<Map> getComments(@PathVariable("postId") int postId) {
        String sql = "SELECT user, comment_content, created_at FROM comments WHERE post_id = ? ORDER BY comment_id DESC";
        return (List<Map>) jdbcTemplate.queryForList(sql, postId);
    }

    @RequestMapping(value="/files/{postId}", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getFilePaths(@PathVariable("postId") int postId) {
        String sql = "SELECT files_path FROM posts WHERE ID = ?";
        String filesCSV = jdbcTemplate.queryForObject(sql, new Object[]{postId}, String.class);

        List<String> filePaths = new ArrayList<>();
        if (filesCSV != null && !filesCSV.isEmpty()) {
            filePaths = Arrays.asList(filesCSV.split(","));
        }

        return filePaths;
    }

    @RequestMapping(value = "/download/{postId}/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Object> downloadFile(@PathVariable("postId") Long postId, @PathVariable("fileName") String fileName) {
        String remoteFilePath = "uploads/" + postId + "/" + fileName;
        File file = ftpService.downloadFileFromFTP(remoteFilePath);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] fileContent;
        HttpHeaders headers = new HttpHeaders();

        try {
            if (fileName.toLowerCase().endsWith(".pdf")) {
                PDDocument document = PDDocument.load(file);

                List<BufferedImage> images = new ArrayList<>();

                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    BufferedImage image = renderer.renderImageWithDPI(page, 300);
                    images.add(image);
                }

                document.close();

                int width = images.get(0).getWidth();
                int height = images.stream().mapToInt(BufferedImage::getHeight).sum();
                BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = combinedImage.createGraphics();
                int y = 0;
                for (BufferedImage image : images) {
                    g2.drawImage(image, 0, y, null);
                    y += image.getHeight();
                }
                g2.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(combinedImage, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();

                headers.setContentType(MediaType.IMAGE_JPEG);
                headers.setContentDispositionFormData("attachment", fileName.replace(".pdf", ".jpg"));

                Resource resource = new ByteArrayResource(imageBytes);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            } else {
                try (InputStream inputStream = new FileInputStream(file)) {
                    fileContent = inputStream.readAllBytes();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        headers.setContentDispositionFormData("attachment", fileName);
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        headers.setContentType(MediaType.parseMediaType(contentType));

        Resource resource = new ByteArrayResource(fileContent);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }


}