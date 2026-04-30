package org.ujorm.petstore.utilities;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ujorm.petstore.Services;
import org.ujorm.tools.web.request.HttpContext;

import java.io.IOException;

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

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(HttpContext.of(req, resp));
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(HttpContext.of(req, resp));
    }

    @Override
    protected final void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPut(HttpContext.of(req, resp));
    }

    @Override
    protected final void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDelete(HttpContext.of(req, resp));
    }

    // --- CTX --

    /** @see #doGet(HttpServletRequest, HttpServletResponse)  */
    protected void doGet(HttpContext ctx) throws ServletException, IOException {

    }

    /** @see #doPost(HttpServletRequest, HttpServletResponse)  */
    protected void doPost(HttpContext ctx) throws ServletException, IOException {

    }

    /** @see #doPut(HttpServletRequest, HttpServletResponse)  */
    protected void doPut(HttpContext ctx) throws ServletException, IOException {

    }

    /** @see #doDelete(HttpServletRequest, HttpServletResponse)  */
    protected void doDelete(HttpContext ctx) throws ServletException, IOException {

    }

    public Services services() {
        return services;
    }

}
