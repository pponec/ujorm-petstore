package org.ujorm.petstore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.User;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.Html;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.ao.HttpParameter;
import org.ujorm.tools.web.request.HttpContext;
import org.ujorm.petstore.Constants.Css;
import org.ujorm.petstore.Constants.Status;
import org.ujorm.petstore.Constants.Role;
import org.ujorm.petstore.Constants.Text;

import static org.ujorm.petstore.PetServlet.Attrib.*;

/** Web presentation layer for PetStore */
@WebServlet(urlPatterns = "")
public class PetServlet extends MyServlet {

    /** CSS link */
    static final String BOOTSTRAP_CSS = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";

    /** Injected business logic and data access */
    @Autowired
    private AppPetStore.Services services;

    /** Handles GET requests to display the UI */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        var loggedUser = (User) req.getSession().getAttribute(Text.LOGGED_USER);
        var contextPath = req.getContextPath();
        var ctx = HttpContext.ofServlet(req, resp);
        var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);

        if (action == Action.LOGOUT) {
            req.getSession().invalidate();
            try { resp.sendRedirect(contextPath + "/login"); } catch (IOException e) {}
            return;
        }

        var petId = ctx.parameter(PET_ID, Long::parseLong);
        var pets = services.getPets();
        var categories = services.getCategories();
        var petToEdit = switch(action) {
            case EDIT -> services.getPetById(petId);
            default -> Optional.<Pet>empty(); };

        try (var html = HtmlElement.of(ctx, BOOTSTRAP_CSS)) {
            try (var body = html.addBody(Css.container, Css.mt5)) {
                renderHeader(body, contextPath, loggedUser);
                renderTable(body, pets, loggedUser);
                if (loggedUser.role() == Role.ADMIN) {
                    renderForm(body, petToEdit, categories);
                }
            }
        }
    }

    /** Handles POST requests to modify data */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var loggedUser = (User) req.getSession().getAttribute(Text.LOGGED_USER);
        var ctx = HttpContext.ofServlet(req, resp);
        var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
        var petId = ctx.parameter(PET_ID, Long::parseLong);
        var resultUrl = req.getContextPath() + "/";

        // Authorization check
        var isAdmin = loggedUser != null && loggedUser.role() == Role.ADMIN;

        switch (action) {
            case BUY -> services.buyPet(petId);
            case DELETE -> { if (isAdmin) services.deletePet(petId); }
            case EDIT -> { if (isAdmin) resultUrl += "?" + ACTION + "=" + Action.EDIT + "&" + PET_ID + "=" + petId; }
            case SAVE -> {
                if (isAdmin) services.savePet(petId,
                        ctx.parameter(NAME, ""),
                        ctx.parameter(STATUS, s -> Status.valueOf(s.toUpperCase())),
                        ctx.parameter(CATEGORY_ID, Long::parseLong));
            }
        }
        resp.sendRedirect(resultUrl);
    }

    /** Renders the page header */
    private void renderHeader(Element body, String contextPath, User loggedUser) {
        try (var header = body.addDiv(Css.dFlex, Css.justifyContentBetween, Css.alignItemsCenter, Css.mb4, Css.borderBottom, Css.pb3)) {
            try (var title = header.addDiv()) {
                title.addHeading(1, "Ujorm PetStore", Css.textPrimary);
                try (var nav = title.addDiv("small")) {
                    nav.addText("Logged as: ").addSpan("fw-bold").addText(loggedUser.login());
                    nav.addText(" (" + loggedUser.role() + ") | ");
                    if (loggedUser.role() == Role.ADMIN) {
                        nav.addAnchor(contextPath + "/users", "text-decoration-none me-2").addText("User Management");
                    }
                    nav.addAnchor(contextPath + "/?action=logout", "text-danger text-decoration-none").addText("Logout");
                }
            }
            header.addAnchor(contextPath + "/")
                    .addImage(contextPath + "/images/ujorm3-logo.png", "Ujorm Logo")
                    .setAttr("width", 100)
                    .setAttr("height", 100);
        }
    }

    /** Renders the table of available pets */
    private void renderTable(Element body, List<Pet> pets, User loggedUser) {
        body.addHeading(2, "Available Pets", Css.mb3);
        try (var table = body.addTable(Css.table, Css.tableHover)) {
            try (var headRow = table.addTableHead(Css.tableDark).addTableRow()) {
                headRow.addTableDetail().addText("ID");
                headRow.addTableDetail().addText("Name");
                headRow.addTableDetail().addText("Status");
                headRow.addTableDetail().addText("Category");
                headRow.addTableDetail().addText("Actions");
            }
            try (var tbody = table.addTableBody()) {
                for (var pet : pets) {
                    try (var row = tbody.addTableRow()) {
                        row.addTableDetail().addText(pet.id());
                        row.addTableDetail().addText(pet.name());
                        row.addTableDetail().addSpan(Css.badge, statusCss(pet.status())).addText(statusName(pet.status()));
                        row.addTableDetail().addText(pet.category() != null ? pet.category().name() : "");
                        buttonBar(pet, row.addTableDetail(), loggedUser);
                    }
                }
            }
        }
    }

    /** Renders the form for adding or editing a pet */
    private void renderForm(Element body, Optional<Pet> petToEdit, List<Category> categories) {
        body.addHeading(2, petToEdit.isPresent() ? "Edit Pet" : "Add New Pet", Css.mt5);
        try (var form = body.addForm().setMethod(Html.V_POST).setAction("?" + ACTION + "=" + Action.SAVE)) {
            petToEdit.ifPresent(pet -> form.addHiddenInput(PET_ID, pet.id()));
            try (var row = form.addDiv(Css.row, Css.g3)) {
                try (var col = row.addDiv(Css.colMd4)) {
                    var petName = petToEdit.map(Pet::name).orElse("");
                    col.addTextInput(Css.formControl).setNameValue(NAME, petName).setAttr("placeholder", "Name");
                }
                try (var col = row.addDiv(Css.colMd3)) {
                    var selectValue = petToEdit.map(Pet::status).orElse(Status.AVAILABLE);
                    var statuses = new EnumMap<Status, String>(Status.class);
                    for (var status : Status.values()) statuses.put(status, statusName(status));
                    col.addSelect(Css.formSelect).setName(STATUS).addSelectOptions(selectValue, statuses);
                }
                try (var col = row.addDiv(Css.colMd3)) {
                    var select = col.addSelect(Css.formSelect).setName(CATEGORY_ID);
                    for (var cat : categories) {
                        var opt = select.addElement("option").setAttr("value", cat.id());
                        if (petToEdit.map(pet -> pet.category().id().equals(cat.id())).orElse(false)) opt.setAttr("selected", "selected");
                        opt.addText(cat.name());
                    }
                }
                row.addDiv(Css.colMd2).addSubmitButton(Css.btn, Css.btnPrimary, Css.w100).addText("Save");
            }
        }
    }

    /** Renders action buttons */
    private void buttonBar(Pet pet, Element tdActions, User loggedUser) {
        try (var form = tdActions.addForm(Css.dInline).setMethod(Html.V_POST)) {
            form.addHiddenInput(PET_ID, pet.id());
            var available = Status.AVAILABLE.equals(pet.status());
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnSuccess)
                    .setNameValue(ACTION, Action.BUY)
                    .setAttr(available ? null : "disabled", "disabled")
                    .addText("Buy");

            if (loggedUser.role() == Role.ADMIN) {
                form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlinePrimary, Css.ms1)
                        .setNameValue(ACTION, Action.EDIT)
                        .addText("Edit");
                form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlineDanger, Css.ms1)
                        .setNameValue(ACTION, Action.DELETE)
                        .addText("Delete");
            }
        }
    }

    String statusName(Status status) {
        return switch (status) {
            case AVAILABLE -> "Available";
            case PENDING -> "Pending";
            default -> "Sold";
        };
    }

    String statusCss(Status status) {
        return switch (status) {
            case AVAILABLE -> Css.bgSuccess;
            case PENDING -> Css.bgWarning;
            default -> Css.bgSecondary;
        };
    }

    enum Attrib implements HttpParameter { ACTION, PET_ID, NAME, STATUS, CATEGORY_ID;
        @Override public String toString() { return name().toLowerCase().replace('_', '-'); }
    }

    enum Action implements HttpParameter { BUY, DELETE, SAVE, EDIT, LOGOUT, UNKNOWN;
        @Override public String toString() { return name().toLowerCase(); }
        public static Action paramValueOf(String name) { return HttpParameter.paramValueOf(Action.class, name, UNKNOWN); }
    }
}
