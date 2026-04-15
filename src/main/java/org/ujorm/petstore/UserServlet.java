package org.ujorm.petstore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.Html;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.ao.HttpParameter;
import org.ujorm.tools.web.request.HttpContext;
import org.ujorm.petstore.Constants.Css;
import org.ujorm.petstore.Constants.Role;
import org.ujorm.petstore.Constants.Text;
import org.ujorm.petstore.Entities.User;

import static org.ujorm.petstore.UserServlet.Attrib.*;

/** Servlet for user management (Admin only) */
@WebServlet(urlPatterns = "/users")
public class UserServlet extends MyServlet {

    @Autowired
    private AppPetStore.Services services;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        var loggedUser = (User) req.getSession().getAttribute(Text.LOGGED_USER);
        if (loggedUser == null || loggedUser.role() != Role.ADMIN) {
            try { resp.sendError(HttpServletResponse.SC_FORBIDDEN); } catch (IOException e) {}
            return;
        }

        var ctx = HttpContext.ofServlet(req, resp);
        var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
        var userId = ctx.parameter(USER_ID, Long::parseLong);
        var users = services.getUsers();
        var userToEdit = action == Action.EDIT ? services.getUserById(userId) : Optional.<User>empty();
        var error = ctx.parameter(ERROR, "");

        try (var html = HtmlElement.of(ctx, PetServlet.BOOTSTRAP_CSS)) {
            try (var body = html.addBody(Css.container, Css.mt5)) {
                renderHeader(body, req.getContextPath(), loggedUser);
                renderTable(body, users, req.getContextPath());
                renderForm(body, userToEdit, error);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var loggedUser = (User) req.getSession().getAttribute(Text.LOGGED_USER);
        if (loggedUser == null || loggedUser.role() != Role.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        var ctx = HttpContext.ofServlet(req, resp);
        var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
        var userId = ctx.parameter(USER_ID, Long::parseLong);

        if (action == Action.SAVE) {
            try {
                services.saveUser(
                        userId,
                        ctx.parameter(LOGIN, ""),
                        ctx.parameter(PASSWORD, ""),
                        ctx.parameter(ROLE, s -> Role.valueOf(s.toUpperCase())),
                        ctx.parameter(ACTIVE, Boolean::parseBoolean, false));
            } catch (IllegalArgumentException e) {
                resp.sendRedirect(req.getContextPath() + "/users?action=edit&user-id=" + (userId != null ? userId : "") + "&error=" + e.getMessage());
                return;
            }
        }

        resp.sendRedirect(req.getContextPath() + "/users");
    }

    private void renderHeader(Element body, String contextPath, User loggedUser) {
        try (var header = body.addDiv(Css.dFlex, Css.justifyContentBetween, Css.alignItemsCenter, Css.mb4, Css.borderBottom, Css.pb3)) {
            header.addHeading(1, "User Management", Css.textPrimary);
            try (var nav = header.addDiv()) {
                nav.addAnchor(contextPath + "/", "btn btn-link").addText("Back to Pets");
                nav.addSpan(Css.ms1).addText("Logged as: " + loggedUser.login() + " (" + loggedUser.role() + ")");
            }
        }
    }

    private void renderTable(Element body, java.util.List<User> users, String contextPath) {
        try (var table = body.addTable(Css.table, Css.tableHover)) {
            try (var headRow = table.addTableHead(Css.tableDark).addTableRow()) {
                headRow.addTableDetail().addText("ID");
                headRow.addTableDetail().addText("Login");
                headRow.addTableDetail().addText("Role");
                headRow.addTableDetail().addText("Status");
                headRow.addTableDetail().addText("Actions");
            }
            try (var tbody = table.addTableBody()) {
                for (var user : users) {
                    try (var row = tbody.addTableRow()) {
                        row.addTableDetail().addText(user.id());
                        row.addTableDetail().addText(user.login());
                        row.addTableDetail().addText(user.role());
                        row.addTableDetail().addSpan(Css.badge, user.active() ? Css.bgSuccess : Css.bgSecondary)
                                .addText(user.active() ? "Active" : "Inactive");
                        try (var td = row.addTableDetail()) {
                            td.addAnchor(contextPath + "/users?action=edit&user-id=" + user.id(), Css.btn, Css.btnSm, Css.btnOutlinePrimary)
                                    .addText("Edit");
                        }
                    }
                }
            }
        }
    }

    private void renderForm(Element body, Optional<User> userToEdit, String error) {
        body.addHeading(2, userToEdit.isPresent() ? "Edit User" : "Add New User", Css.mt5);
        if (org.ujorm.tools.Check.hasLength(error)) {
            body.addDiv("alert", "alert-danger").addText(error);
        }
        try (var form = body.addForm().setMethod(Html.V_POST).setAction("?action=save")) {
            userToEdit.ifPresent(user -> form.addHiddenInput(USER_ID, user.id()));
            try (var row = form.addDiv(Css.row, Css.g3)) {
                row.addDiv(Css.colMd3).addTextInput(Css.formControl).setNameValue(LOGIN, userToEdit.map(User::login).orElse("")).setAttr("placeholder", "Login");
                row.addDiv(Css.colMd3).addElement("input", Css.formControl).setAttr("type", "password")
                        .setName(PASSWORD).setAttr("placeholder", userToEdit.isPresent() ? "New Password (optional)" : "Password");
                try (var col = row.addDiv(Css.colMd2)) {
                    var roles = new EnumMap<Role, String>(Role.class);
                    for (var role : Role.values()) roles.put(role, role.name());
                    col.addSelect(Css.formSelect).setName(ROLE).addSelectOptions(userToEdit.map(User::role).orElse(Role.CUSTOMER), roles);
                }
                try (var col = row.addDiv(Css.colMd2, Css.dFlex, Css.alignItemsCenter)) {
                    col.addCheckBox(ACTIVE, Css.formCheckInput).setCheckBoxValue(userToEdit.map(User::active).orElse(true));
                    col.addLabel(Css.ms1).addText("Active");
                }
                row.addDiv(Css.colMd2).addSubmitButton(Css.btn, Css.btnPrimary, Css.w100).addText(userToEdit.isPresent() ? "Update" : "Create");
            }
        }
    }

    enum Attrib implements HttpParameter {
        ACTION, USER_ID, LOGIN, PASSWORD, ROLE, ACTIVE, ERROR;
        @Override public String toString() { return name().toLowerCase().replace('_', '-'); }
    }

    enum Action implements HttpParameter {
        EDIT, SAVE, UNKNOWN;
        @Override public String toString() { return name().toLowerCase(); }
        public static Action paramValueOf(String name) { return HttpParameter.paramValueOf(Action.class, name, UNKNOWN); }
    }
}
