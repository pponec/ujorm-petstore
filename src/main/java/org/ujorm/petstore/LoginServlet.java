package org.ujorm.petstore;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.Html;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.ao.HttpParameter;
import org.ujorm.tools.web.request.HttpContext;
import org.ujorm.petstore.Constants.Css;
import org.ujorm.petstore.Constants.Text;

import static org.ujorm.petstore.LoginServlet.Attrib.*;

/** Servlet for user authentication */
@WebServlet(urlPatterns = "/login")
public class LoginServlet extends MyServlet {

    /** Session attribute for the logged user */
    public static final String SESSION_USER = Text.LOGGED_USER;

    @Autowired
    private AppPetStore.Services services;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        var ctx = HttpContext.ofServlet(req, resp);
        var error = ctx.parameter(ERROR, Boolean::parseBoolean, false);

        try (var html = HtmlElement.of(ctx, PetServlet.BOOTSTRAP_CSS)) {
            try (var body = html.addBody(Css.bgSecondary, Css.vh100, Css.dFlex, Css.alignItemsCenter)) {
                try (var container = body.addDiv(Css.container)) {
                    try (var row = container.addDiv(Css.row, "justify-content-center")) {
                        try (var col = row.addDiv(Css.colMd4)) {
                            loginForm(col, error);
                        }
                    }
                }
            }
        }
    }

    private void loginForm(Element col, Boolean error) {
        try (var card = col.addDiv(Css.bgSuccess, Css.shadow, Css.p4, Css.rounded, "text-white")) {
            card.addHeading(2, "PetStore Login", Css.mb4, "text-center");
            if (error) {
                card.addDiv("alert", "alert-danger", Css.mb3).addText("Invalid login or password.");
            }
            try (var form = card.addForm().setMethod(Html.V_POST)) {
                try (var mb3 = form.addDiv(Css.mb3)) {
                    mb3.addElement("label").addText("Login");
                    mb3.addTextInput(Css.formControl).setName(LOGIN).setAttr("required", "required");
                }
                try (var mb3 = form.addDiv(Css.mb3)) {
                    mb3.addElement("label").addText("Password");
                    mb3.addElement("input", Css.formControl).setAttr("type", "password")
                            .setName(PASSWORD).setAttr("required", "required");
                }
                form.addSubmitButton(Css.btn, Css.btnPrimary, Css.w100, Css.mt5).addText("Login");
            }
            card.addDiv(Css.mt5, "text-center", "small").addText("Default: " + Text.DEFAULT_USER + " / " + Text.DEFAULT_PASSWORD);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var ctx = HttpContext.ofServlet(req, resp);
        var login = ctx.parameter(LOGIN, "");
        var password = ctx.parameter(PASSWORD, "");

        var user = services.login(login, password);
        if (user.isPresent()) {
            req.getSession().setAttribute(SESSION_USER, user.get());
            resp.sendRedirect(req.getContextPath() + "/");
        } else {
            resp.sendRedirect(req.getContextPath() + "/login?" + ERROR + "=true");
        }
    }

    /** Servlet attributes */
    enum Attrib implements HttpParameter {
        LOGIN, PASSWORD, ERROR;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
