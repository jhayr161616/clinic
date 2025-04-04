package com.clinicAppointment.clinic.controller;

import com.clinicAppointment.clinic.model.AccountModel;
import com.clinicAppointment.clinic.repository.AccountRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AccountController {


    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    @GetMapping("/login")
    public String showLoginPage(HttpServletRequest request, HttpSession session) {
        // Check session for user email
        if (session.getAttribute("userEmail") != null) {
            System.out.println("test");
            return "redirect:/client";
        }

        // Check cookies for session token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("sessionToken")) {
                    String email = accountRepository.getEmailBySession(cookie.getValue());
                    if (email != null) {
                        session.setAttribute("userEmail", email);
                        return "redirect:/client";
                    }
                }
            }
        }

        return "login"; // Show login page if not logged in
    }

    @GetMapping("/signup")
    public String showSignupPage(HttpServletRequest request, HttpSession session) {
        // Check if user is already logged in
        if (session.getAttribute("userEmail") != null) {
            return "redirect:/client";
        }

        // Check cookies for session token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("sessionToken")) {
                    String email = accountRepository.getEmailBySession(cookie.getValue());
                    if (email != null) {
                        session.setAttribute("userEmail", email);
                        return "redirect:/client";
                    }
                }
            }
        }
        return "signup"; // Show signup page if not logged in
    }


    @PostMapping("/loginAccount")
    public String loginAccount(@RequestParam String email, @RequestParam String password,
                               Model model, HttpSession session, HttpServletResponse response) throws Exception{
        boolean accountExists = accountRepository.checkAccount(email, password);

        if (accountExists) {
            session.setAttribute("userEmail", email);

            // Generate session token and store it in the database
            String sessionToken = accountRepository.generateSessionToken(email);

            // Set session token in a cookie
            Cookie sessionCookie = new Cookie("sessionToken", sessionToken);
            sessionCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            sessionCookie.setPath("/");
            response.addCookie(sessionCookie);
            String role = accountRepository.getRoleByEmail(email);
            if (role.equals("admin")){
                return "redirect:/admin";
            }else {
                return "redirect:/client";
            }


        } else {
            model.addAttribute("error", "Invalid email or password");
            System.out.println("error login");
            return "login";
        }
    }

    @GetMapping("/client")
    public String showClientPage(HttpSession session, HttpServletRequest request) {
        String userEmail = (String) session.getAttribute("userEmail");

        if (userEmail == null) {
            // Check if sessionToken exists in cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("sessionToken")) {
                        String email = accountRepository.getEmailBySession(cookie.getValue());
                        if (email != null) {
                            session.setAttribute("userEmail", email);
                            return "client";
                        }
                    }
                }
            }
            return "redirect:/login";
        }
        if (accountRepository.getRoleByEmail(userEmail).equals("admin")){
            return "redirect:/admin";
        }
        return "client";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response, HttpServletRequest request) {
        String userEmail = (String) session.getAttribute("userEmail");

        if (userEmail != null) {
            accountRepository.clearSessionToken(userEmail);
        }


        session.invalidate();

        // Remove session token from cookies
        Cookie sessionCookie = new Cookie("sessionToken", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        return "redirect:/login";
    }

    @PostMapping("/addAccount")
    public String addAccount(@RequestParam String fname,
                             @RequestParam String lname,
                             @RequestParam String email,
                             @RequestParam String password,
                             Model model) {
        if (accountRepository.emailExists(email)) {
            model.addAttribute("error", "Email already exists!");
            return "signup";
        }


        accountRepository.addAccount(fname, lname, email, password);
        System.out.println("Account added: " + email);

        return "redirect:/login"; // Redirect to login after successful signup
    }


    @GetMapping("/admin")
    public String adminHome(HttpSession session, Model model){
        model.addAttribute("accounts", accountRepository.getAllAccounts());
        String userEmail = (String) session.getAttribute("userEmail");

        if (userEmail==null){
            System.out.println("null: " + userEmail);
            return "login";
        }
        if (accountRepository.getRoleByEmail(userEmail).equals("admin")){
            System.out.println("admin: " + userEmail );
            return "admin";
        }
        if  (accountRepository.getRoleByEmail(userEmail).equals("user")){
            System.out.println("user: "+ userEmail);
            return "client";
        }
        System.out.println(userEmail);
    return "admin";
    }

    @PostMapping("/removeAccount")
    public String removeAccount(@RequestParam int id) {
        accountRepository.deleteAccount(id);
        return "redirect:/admin";
    }

}