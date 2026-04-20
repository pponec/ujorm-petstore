package org.ujorm.petstore.utilities;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.ujorm.petstore.Services;

public abstract class AbstractServlet extends HttpServlet {

    /**
     * Injected business logic and data access
     */
    private Services services;

    /**
     * Initializes the servlet and retrieves beans from the Avaje context
     */
    @Override
    public void init() throws ServletException {
        super.init();
        this.services = Bootstrap.getBeanScope().get(Services.class);
    }

    public Services services() {
        return services;
    }

    protected String contextPathSlash(HttpServletRequest req) {
        var result = req.getContextPath();
        return result.isEmpty() ? "/" : (result + "/");
    }

}
