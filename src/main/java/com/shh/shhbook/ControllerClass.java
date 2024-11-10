package com.shh.shhbook;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.shh.shhbook.model.Likes;
import com.shh.shhbook.model.LoginStatistics;
import com.shh.shhbook.model.Posts;
import com.shh.shhbook.repository.LikesRepository;
import com.shh.shhbook.repository.LoginStatisticsRepository;
import com.shh.shhbook.repository.PostsRepository;
import com.shh.shhbook.model.Users;
import com.shh.shhbook.repository.UsersRepository;
import com.shh.shhbook.response.LikeResponse;
import com.shh.shhbook.service.LikesService;
import com.shh.shhbook.service.PostService;
import com.shh.shhbook.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;

@Controller
public class ControllerClass {
    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PostsRepository postsRepository;
    @Autowired
    private LikesRepository likesRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private LoginStatisticsRepository loginStatisticsRepository;
    private void incrementLoginCount(int year, int month) {
        String checkSql = "SELECT COUNT(*) FROM login_statistics WHERE year = ? AND month = ?";
        int existingRecord = jdbcTemplate.queryForObject(checkSql, new Object[]{year, month}, Integer.class);

        if (existingRecord > 0) {
            String updateSql = "UPDATE login_statistics SET login_count = login_count + 1 WHERE year = ? AND month = ?";
            jdbcTemplate.update(updateSql, year, month);
        } else {
            String insertSql = "INSERT INTO login_statistics (year, month, login_count) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, year, month, 1);
        }
    }

    // strona poczatkowa - logowanie
    @RequestMapping(value={"/", "/search"}, method = RequestMethod.GET)
    public String session(HttpServletRequest request,
                          ModelMap model,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "12") int size) {
        List<Users> users = usersRepository.findAll();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("users", users);
        if (request.getRequestURI().equals("/search") && request.getMethod().equals("GET")) {
            String searchField = request.getParameter("search_field");
            Page<Posts> postsPage = postsRepository.findByDescriptionContainingOrTitleContaining(searchField, searchField, pageable);
            model.addAttribute("db_posts", postsPage.getContent());
            model.addAttribute("totalPages", postsPage.getTotalPages());
        }
        else {
            Page<Posts> postsPage = postsRepository.findAll(pageable);
            model.addAttribute("db_posts", postsPage.getContent());
            model.addAttribute("totalPages", postsPage.getTotalPages());
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            List<Long> likesList = likesRepository.findPostIdsByUsername(new Users((String) session.getAttribute("user")));
            model.addAttribute("likes_list", likesList);
            model.addAttribute("user", session.getAttribute("user"));

            String sql = "SELECT ID FROM users WHERE username = ?";
            Integer userPermission = jdbcTemplate.queryForObject(sql, new Object[]{session.getAttribute("user")}, Integer.class);
            model.addAttribute("can_post", userPermission);
        }

        return session != null ? "index" : "login";
    }

    // zalogowanie sie i wejscie na strone glowna
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(final HttpServletRequest request,
                        @ModelAttribute("users") Users user,
                        ModelMap model, RedirectAttributes redirectAttributes) {
        if (request.getMethod().equals("POST")) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            int count = jdbcTemplate.queryForObject(sql, new Object[]{user.getUsername(), user.getPassword()}, Integer.class);

            if (count > 0) {
                HttpSession session = request.getSession();
                session.setAttribute("user", user.getUsername());

                LocalDate currentDate = LocalDate.now();
                int currentMonth = currentDate.getMonthValue();
                int currentYear = currentDate.getYear();

                incrementLoginCount(currentYear, currentMonth);

                return "redirect:/";
            } else {
                model.addAttribute("error", "Niepoprawny login lub hasło");
                return "login";
            }
        }
        return "login";
    }

    // wylogowanie sie
    @RequestMapping("/logout")
    public String logout(final HttpServletRequest request)
    {
        if (request.getSession(false) != null)
            request.getSession(false).invalidate();
        return "redirect:/";
    }

    // wstawianie posta
    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public String post(HttpServletRequest request, @ModelAttribute("posts") Posts post, @RequestParam("files") List<MultipartFile> files, ModelMap model) {
        if (request.getMethod().equals("POST")) {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
            String formattedTimestamp = formatter.format(now);

            String sql = "SELECT MAX(ID) FROM posts";
            Long lastPostId = jdbcTemplate.queryForObject(sql, Long.class);
            if (lastPostId == null) lastPostId = 1L;
            else lastPostId += 1;

            List<String> fileUrls = new ArrayList<>();
            String thumbnailUrl = null;
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                        String fileKey = "uploads/" + lastPostId + "/" + fileName;

                        InputStream inputStream = file.getInputStream();
                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(file.getSize());
                        metadata.setContentType(file.getContentType());

                        amazonS3.putObject(bucketName, fileKey, inputStream, metadata);
                        amazonS3.setObjectAcl(bucketName, fileKey, CannedAccessControlList.Private);

                        String fileUrl = amazonS3.getUrl(bucketName, fileKey).toString();
                        fileUrls.add(fileUrl);

                        if (thumbnailUrl == null) {
                            if (fileName.endsWith(".pdf") || fileName.endsWith(".PDF")) {
                                thumbnailUrl = createPdfThumbnail(fileKey);
                            } else {
                                thumbnailUrl = fileUrl;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        model.addAttribute("error", "Nie udało się zapisać pliku " + file.getOriginalFilename());
                        return "redirect:/";
                    }
                }
            }

            String filesCSV = String.join(",", fileUrls);
            try {
                sql = "INSERT INTO posts (username, title, description, show_desc, gallery_link, files_path, thumbnail_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                int inserted = jdbcTemplate.update(sql, request.getSession().getAttribute("user"), post.getTitle(), post.getDescription(), post.getShow_desc(), post.getGallery_link(), filesCSV, thumbnailUrl, formattedTimestamp);
                if (inserted == 0) {
                    model.addAttribute("error", "Nie udało się dodać posta.");
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stackTrace = sw.toString();

                String errorMessage = "<html><body><h2>Blad przy dodawaniu posta:</h2><pre>" + stackTrace + "</pre></body></html>";
                return String.valueOf(new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR));
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
            String formattedTimestamp = formatter.format(now);

            String sql = "INSERT INTO comments (post_id, comment_content, user, created_at) VALUES (?, ?, ?, ?)";
            int inserted = jdbcTemplate.update(sql, postId, commentContent, request.getSession().getAttribute("user"), formattedTimestamp);
            if (inserted == 0)
            {
                model.addAttribute("error", "Nie udało się dodać komentarza.");
            }
        }
        return "redirect:/";
    }

    private List<BufferedImage> convertPdfToImages(InputStream inputStream) throws IOException {
        PDDocument document = PDDocument.load(inputStream);
        PDFRenderer renderer = new PDFRenderer(document);

        List<BufferedImage> images = new ArrayList<>();
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            BufferedImage image = renderer.renderImageWithDPI(page, 300);
            images.add(image);
        }

        document.close();
        return images;
    }

    private BufferedImage combineImages(List<BufferedImage> images) {
        int totalHeight = images.stream().mapToInt(BufferedImage::getHeight).sum();
        int maxWidth = images.stream().mapToInt(BufferedImage::getWidth).max().orElse(0);
        BufferedImage combinedImage = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = combinedImage.createGraphics();
        int currentY = 0;
        for (BufferedImage image : images) {
            g2d.drawImage(image, 0, currentY, null);
            currentY += image.getHeight();
        }
        g2d.dispose();
        return combinedImage;
    }

    private String createPdfThumbnail(String fileKey) {
        try {
            S3Object s3Object = amazonS3.getObject(new GetObjectRequest(bucketName, fileKey));
            InputStream inputStream = s3Object.getObjectContent();
            String thumbnailKey;
            List<BufferedImage> images = convertPdfToImages(inputStream);
            BufferedImage combinedImage = combineImages(images);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(combinedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            if (fileKey.endsWith(".PDF"))
                thumbnailKey = fileKey.replace(".PDF", "_thumbnail.jpg");
            else
                thumbnailKey = fileKey.replace(".pdf", "_thumbnail.jpg");
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageBytes.length);
            metadata.setContentType("image/jpeg");

            amazonS3.putObject(bucketName, thumbnailKey, new ByteArrayInputStream(imageBytes), metadata);
            amazonS3.setObjectAcl(bucketName, thumbnailKey, CannedAccessControlList.Private);

            return amazonS3.getUrl(bucketName, thumbnailKey).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Autowired
    private PostService postService;
    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable("id") Long id) {
        try {
            List<String> keysToDelete = new ArrayList<>();

            Long maxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM posts", Long.class);
            if (maxId == null) {
                maxId = 0L;
            }

            ListObjectsV2Result result = amazonS3.listObjectsV2(bucketName, "uploads/" + id);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                keysToDelete.add(objectSummary.getKey());
            }

            if (!keysToDelete.isEmpty()) {
                DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                        .withKeys(keysToDelete.toArray(new String[0]));
                amazonS3.deleteObjects(deleteObjectsRequest);
            }

            postService.deletePostById(id);

            if (maxId.equals(id)) {
                System.out.println("maxid=id");
                String sql = "ALTER TABLE hostel_news.posts AUTO_INCREMENT=" + maxId;
                jdbcTemplate.execute(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/";
    }

    @RequestMapping(value="/edit/{id}")
    public String editPost(@PathVariable("id") Long id, @ModelAttribute Posts post) {
        postService.updatePost(id, post);
        return "redirect:/";
    }

    @RequestMapping(value = "/statistics", method = RequestMethod.GET)
    @ResponseBody
    public void statistics(HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"statystyki_logowania.pdf\"");

        PdfWriter pdfWriter = new PdfWriter(response.getOutputStream());
        Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter));

        var font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        document.add(new Paragraph("Statystyki logowania w ostatnich 3 miesiacach").setFont(font).setBold());

        List<LoginStatistics> statistics = loginStatisticsRepository.findLastThreeMonths();

        float[] pointColumnWidths = {150F, 150F, 150F};
        Table table = new Table(pointColumnWidths);

        table.addHeaderCell("Rok");
        table.addHeaderCell("Miesiac");
        table.addHeaderCell("Liczba logowan");

        for (LoginStatistics stat : statistics) {
            table.addCell(String.valueOf(stat.getYear()));
            table.addCell(String.valueOf(stat.getMonth()));
            table.addCell(String.valueOf(stat.getLoginCount()));
        }

        document.add(table);

        document.close();
    }
    @RestController
    @RequestMapping("/api/likes")
    public static class LikeController {
        @Autowired
        private LikesService likesService;
        @PostMapping("/toggle")
        public LikeResponse toggleLike(final HttpServletRequest request, @RequestParam Long postId) {
            String username = (String) request.getSession().getAttribute("user");
            boolean liked = likesService.toggleLike(username, postId);
            int likeCount = likesService.getLikeCount(postId);
            return new LikeResponse(liked, likeCount);
        }
    }
    @Autowired
    private UserService userService;
    @PostMapping("/addUser")
    public String addUser(@RequestParam("username") String username, RedirectAttributes redirectAttributes) {
        if (userService.usernameExists(username))
        {
            redirectAttributes.addFlashAttribute("error", "Użytkownik o takiej nazwie już istnieje!");
            return "redirect:/";
        }
        userService.addUser(username);
        return "redirect:/";
    }

    // Delete a user
    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam("username") String username) {
        userService.deleteUser(username);
        return "redirect:/";
    }

    @RequestMapping(value = "/usernames", method = RequestMethod.GET)
    @ResponseBody
    public void generateUsernamesPdf(HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"uzytkownicy_w_bazie.pdf\"");

        PdfWriter pdfWriter = new PdfWriter(response.getOutputStream());
        Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter));

        var font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        document.add(new Paragraph("Lista uzytkowników w bazie danych").setFont(font).setBold());

        List<Users> usersList = usersRepository.findAll();

        float[] pointColumnWidths = {200F};
        Table table = new Table(pointColumnWidths);

        for (Users user : usersList) {
            table.addCell(user.getUsername());
        }

        document.add(table);

        document.close();
    }

    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        String username = (String) session.getAttribute("user");

        if (username == null) {
            redirectAttributes.addFlashAttribute("error", "Nie jesteś zalogowany.");
            return "redirect:/login";
        }

        Users user = usersRepository.findByUsername(username);
        if (user == null || !user.getPassword().equals(currentPassword)) {
            redirectAttributes.addFlashAttribute("error", "Aktualne hasło jest niepoprawne.");
            return "redirect:/";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Nowe hasła nie są zgodne.");
            return "redirect:/";
        }

        user.setPassword(newPassword);
        usersRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Hasło zostało zmienione pomyślnie.");
        return "redirect:/";
    }
}
