package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        HttpSession session = request.getSession(false);
        String requestURI = request.getRequestURI();
        
        // Skip session check for login pages and public resources
        if (requestURI.contains("/login") || requestURI.contains("/cuslogin") || 
            requestURI.contains("/stafflogin") || requestURI.contains("/admin") || 
            requestURI.contains("/customer") || requestURI.contains("/staff") ||
            requestURI.contains("/home") || requestURI.contains("/static") ||
            requestURI.contains("/css") || requestURI.contains("/js")) {
            return true;
        }
        
        // Check if user is logged in
        if (session == null || session.getAttribute("user") == null) {
            // Redirect to appropriate login page based on request path
            if (requestURI.contains("/manage") || requestURI.contains("/update")) {
                response.sendRedirect("/admin");
            } else if (requestURI.contains("/send") || requestURI.contains("/track") || 
                       requestURI.contains("/enter") || requestURI.contains("/display") ||
                       requestURI.contains("/search")) {
                response.sendRedirect("/customer");
            } else if (requestURI.contains("/pickup") || requestURI.contains("/deliver")) {
                response.sendRedirect("/staff");
            } else {
                response.sendRedirect("/home");
            }
            return false;
        }
        
        // Refresh session timeout on each request
        session.setMaxInactiveInterval(1800); // 30 minutes
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                          ModelAndView modelAndView) throws Exception {
        // Add session info to all views
        if (modelAndView != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                modelAndView.addObject("sessionActive", true);
                modelAndView.addObject("sessionUser", session.getAttribute("user"));
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                               Exception ex) throws Exception {
        // Log request completion
        if (ex != null) {
            System.err.println("Request failed: " + ex.getMessage());
        }
    }
}
