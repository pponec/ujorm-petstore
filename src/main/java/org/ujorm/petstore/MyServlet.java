package org.ujorm.petstore;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * Common abstract ancestor for all servlets in the project.
 * Handles Spring dependency injection initialization.
 */
public abstract class MyServlet extends HttpServlet {

    /** Initializes the servlet and enables Spring dependency injection */
    @Override
    public void init() throws ServletException {
        super.init();
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, getServletContext());
    }
}
