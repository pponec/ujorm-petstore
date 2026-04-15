package org.ujorm.petstore;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.ujorm.petstore.Constants.Text;
import org.ujorm.petstore.Entities.User;

/** Filter to protect the application from unauthorized access */
@WebFilter(urlPatterns = "/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var resp = (HttpServletResponse) response;
        var path = req.getRequestURI().substring(req.getContextPath().length());

        // Allow login page and static resources
        if (path.startsWith("/login") || path.startsWith("/images/")) {
            chain.doFilter(request, response);
            return;
        }

        var user = (User) req.getSession().getAttribute(Text.LOGGED_USER);
        if (user == null || !user.active()) {
            if (user != null) {
                req.getSession().invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/login");
        } else {
            chain.doFilter(request, response);
        }
    }
}
