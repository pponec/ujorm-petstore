package org.ujorm.petstore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.Html;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.ao.HttpParameter;
import org.ujorm.tools.web.request.HttpContext;
import org.ujorm.petstore.Constants.Css;
import org.ujorm.petstore.Constants.Status;

import static org.ujorm.petstore.PetServlet.Attrib.*;

/** Web presentation layer for PetStore */
@WebServlet(urlPatterns = "")
public class PetServlet extends HttpServlet {

    /** CSS link */
    static final String BOOTSTRAP_CSS = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";

    /** Injected business logic and data access */
    @Autowired
    private AppPetStore.Services services;

    /** Initializes the servlet and enables Spring dependency injection */
    @Override
    public void init() throws ServletException {
        super.init();
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, getServletContext());
    }

    /** Handles GET requests to display the UI */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        var ctx = HttpContext.ofServlet(req, resp);
        var action = ctx.parameter(ACTION, Action::paramValueOf);
        var petId = ctx.parameter(ID, Long::parseLong);
        var petToEdit = (Action.EDIT.equals(action) && petId != null)
                ? services.getPetById(petId).orElse(null)
                : null;
        renderPage(ctx, petToEdit, services.getPets(), services.getCategories(), req.getContextPath());
    }

    /** Handles POST requests to modify data using a unified action detection */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var ctx = HttpContext.ofServlet(req, resp);
        var action = Action.UNKNOWN;
        var idParam = (Long) null;

        // 1. Try to detect action and pet ID from button names (for the table form)
        for (var actionName : Action.values()) {
            var val = ctx.parameter(actionName, Long::parseLong);
            if (val != null) {
                action = actionName;
                idParam = val;
                break;
            }
        }

        // 2. Fallback for the "Save" form which uses explicit URL parameters
        if (action == Action.UNKNOWN) {
            action = Action.paramValueOf(ctx.parameter(ACTION, ""));
            idParam = ctx.parameter(ID, Long::parseLong);
        }

        var resultUrl = req.getContextPath() + "/";
        switch (action) {
            case BUY -> services.buyPet(idParam);
            case DELETE -> services.deletePet(idParam);
            case EDIT -> resultUrl += "?" + ACTION + "=" + Action.EDIT + "&" + ID + "=" + idParam;
            case SAVE -> services.savePet(idParam,
                    ctx.parameter(NAME, ""),
                    ctx.parameter(STATUS, s -> Status.valueOf(s.toUpperCase())), // POP?
                    ctx.parameter(CATEGORY_ID, Long::parseLong));
        }
        // Prevents duplicate form submissions (PRG pattern)
        resp.sendRedirect(resultUrl);
    }

    /** Renders the main HTML page */
    private void renderPage(HttpContext ctx, Pet petToEdit,
                            List<Pet> pets,
                            List<Category> categories,
                            String contextPath) {
        try (var html = HtmlElement.of(ctx, BOOTSTRAP_CSS)) {
            try (var body = html.addBody(Css.container, Css.mt5)) {
                try (var header = body.addDiv(
                        Css.dFlex,
                        Css.justifyContentBetween,
                        Css.alignItemsCenter,
                        Css.mb4,
                        Css.borderBottom,
                        Css.pb3)) {
                    header.addHeading(1, "Ujorm PetStore", Css.textPrimary);
                    header.addAnchor(contextPath + "/")
                            .addImage(contextPath + "/images/ujorm3-logo.png", "Ujorm Logo")
                            .setAttr("width", 150).setAttr("height", 150);
                }

                // Table
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

                                var statusCss = statusCss(pet.status());
                                var statusName = statusName(pet.status());
                                row.addTableDetail().addSpan(Css.badge, statusCss).addText(statusName);
                                row.addTableDetail().addText(pet.category() != null ? pet.category().name() : "");
                                buttonBar(pet, row.addTableDetail());
                            }
                        }
                    }
                }

                // Form
                body.addHeading(2, petToEdit == null ? "Add New Pet" : "Edit Pet", Css.mt5);
                try (var form = body.addForm().setMethod(Html.V_POST).setAction("?" + ACTION + "=" + Action.SAVE)) {
                    if (petToEdit != null) form.addHiddenInput(ID, petToEdit.id());
                    try (var row = form.addDiv(Css.row, Css.g3)) {
                        try (var col = row.addDiv(Css.colMd4)) {
                            col.addTextInput(Css.formControl).setNameValue(NAME, petToEdit != null ? petToEdit.name() : "").setAttr("placeholder", "Name");
                        }
                        try (var col = row.addDiv(Css.colMd3)) {
                            var selectValue = petToEdit != null ? petToEdit.status() : Status.AVAILABLE;
                            var statuses = new EnumMap<Status, String>(Status.class);
                            for (var status : Status.values()) {
                                statuses.put(status, statusName(status));
                            }
                            col.addSelect(Css.formSelect).setName(STATUS).addSelectOptions(selectValue, statuses);
                        }
                        try (var col = row.addDiv(Css.colMd3)) {
                            var select = col.addSelect(Css.formSelect).setName(CATEGORY_ID);
                            for (var cat : categories) {
                                var opt = select.addElement("option").setAttr("value", cat.id());
                                if (petToEdit != null && petToEdit.category() != null && cat.id().equals(petToEdit.category().id())) {
                                    opt.setAttr("selected", "selected");
                                }
                                opt.addText(cat.name());
                            }
                        }
                        row.addDiv(Css.colMd2).addSubmitButton(Css.btn, Css.btnPrimary, Css.w100).addText("Save");
                    }
                }
            }
        }
    }

    /** Renders action buttons in a single form using button names to distinguish actions */
    private void buttonBar(Pet pet, Element tdActions) {
        try (var form = tdActions.addForm(Css.dInline).setMethod(Html.V_POST)) {
            var available = Status.AVAILABLE.equals(pet.status());
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnSuccess)
                    .setNameValue(Action.BUY, pet.id())
                    .setAttr(available ? null : "disabled", "disabled")
                    .addText("Buy");
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlinePrimary, Css.ms1)
                    .setNameValue(Action.EDIT, pet.id())
                    .addText("Edit");
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlineDanger, Css.ms1)
                    .setNameValue(Action.DELETE, pet.id())
                    .addText("Delete");
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

    /** Servlet attributes */
    enum Attrib implements HttpParameter {
        /** Action parameter */
        ACTION,
        /** Pet ID */
        ID,
        /** Pet name */
        NAME,
        /** Pet status */
        STATUS,
        /** Category ID */
        CATEGORY_ID;

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    /** Action types */
    enum Action implements HttpParameter {
        BUY, DELETE, SAVE, EDIT, UNKNOWN;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        /** Returns Action by name with a default UNKNOWN value */
        public static Action paramValueOf(String name) {
            return HttpParameter.paramValueOf(Action.class, name, UNKNOWN);
        }
    }
}