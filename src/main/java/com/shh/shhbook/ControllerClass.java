package com.shh.shhbook;

import com.shh.shhbook.model.Posts;
import com.shh.shhbook.repository.CommentsRepository;
import com.shh.shhbook.repository.PostsRepository;
import com.shh.shhbook.model.Users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;


@Controller
public class ControllerClass<Map> {

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
                       ModelMap model)
    {
        if (request.getMethod().equals("POST")) {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Timestamp timestamp = Timestamp.from(now);
            String sql = "INSERT INTO posts (username, title, description, created_at) VALUES (?, ?, ?, ?)";
            int inserted = jdbcTemplate.update(sql, request.getSession().getAttribute("user"), post.getTitle(), post.getDescription(), timestamp);
            if (inserted == 0)
            {
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
}