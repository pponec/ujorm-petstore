package org.ujorm.petstore.utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class AbstractServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, getServletContext());
    }

}
