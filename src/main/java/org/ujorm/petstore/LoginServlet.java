package org.ujorm.petstore;

import jakarta.inject.Singleton;
import jakarta.servlet.annotation.WebServlet;
import org.ujorm.petstore.utilities.AbstractServlet;
import org.ujorm.petstore.utilities.WebRoute;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.request.ExchangeContext;

/** Web presentation layer for PetStore */
@WebRoute("login")
@Singleton
public class LoginServlet extends AbstractServlet {

    /** CSS link */
    static final String BOOTSTRAP_CSS = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";

    /** Handles GET requests to display the UI */
    @Override
    protected void doGet(ExchangeContext ctx) {

        try (var html = HtmlElement.of(ctx, BOOTSTRAP_CSS)) {
            html.getBody().addHeading("TODO");
        }
    }

}
