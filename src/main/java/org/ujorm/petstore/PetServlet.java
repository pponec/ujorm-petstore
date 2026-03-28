package org.ujorm.petstore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
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
        var actionText = ctx.getParameter(ACTION, "");
        var petId = ctx.getParameter(ID, null, Long::parseLong);
        var petToEdit = (Action.EDIT.equalsName(actionText) && petId != null)
                ? services.getPetById(petId).orElse(null)
                : null;
        renderPage(ctx, petToEdit, services.getPets(), services.getCategories(), req.getContextPath());
    }

    /** Handles POST requests to modify data */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var ctx = HttpContext.ofServlet(req, resp);
        var actionText = ctx.getParameter(ACTION, "");
        var idText = ctx.getParameter(ID, "");
        var idParam = idText.isEmpty() ? null : Long.valueOf(idText);

        try {
            var action = Action.valueOf(actionText);
            switch (action) {
                case BUY -> services.buyPet(idParam);
                case DELETE -> services.deletePet(idParam);
                case SAVE -> {
                    var catIdText = ctx.getParameter(CATEGORY_ID, "");
                    var categoryId = catIdText.isEmpty() ? null : Long.valueOf(catIdText);
                    services.savePet(idParam,
                            ctx.getParameter(NAME, ""),
                            ctx.getParameter(STATUS, ""),
                            categoryId
                    );
                }
                default -> { }
            }
        } catch (IllegalArgumentException e) {
            // Ignore invalid action and proceed to redirect
        }
        resp.sendRedirect(req.getContextPath() + "/");
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

                                var status = Status.findByName(pet.status());
                                row.addTableDetail().addSpan(Css.badge, status.getBadgeClass()).addText(status.name());
                                row.addTableDetail().addText(pet.category() != null ? pet.category().name() : "");

                                try (var tdActions = row.addTableDetail()) {
                                    buyForm(pet, tdActions);
                                    editForm(pet, tdActions);
                                    deleteForm(pet, tdActions);
                                }
                            }
                        }
                    }
                }

                // Form
                body.addHeading(2, petToEdit == null ? "Add New Pet" : "Edit Pet", Css.mt5);
                try (var form = body.addForm().setMethod(Html.V_POST).setAction("?action=" + Action.SAVE.name())) {
                    if (petToEdit != null) form.addHiddenInput(ID.toString(), petToEdit.id());
                    try (var row = form.addDiv(Css.row, Css.g3)) {
                        try (var col = row.addDiv(Css.colMd4)) {
                            col.addTextInput(Css.formControl).setNameValue(NAME.toString(), petToEdit != null ? petToEdit.name() : "").setAttr("placeholder", "Name");
                        }
                        try (var col = row.addDiv(Css.colMd3)) {
                            var statuses = new LinkedHashMap<String, String>();
                            for (var status : Status.values()) {
                                statuses.put(status.name(), status.getDisplayName());
                            }
                            var selectValue = petToEdit != null ? petToEdit.status() : Status.AVAILABLE.name();
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

    /** Renders buy form */
    private void buyForm(Pet pet, Element tdActions) {
        try (var form = tdActions.addForm(Css.dInline).setMethod(Html.V_POST).setAction("?action=" + Action.BUY.name())) {
            form.addHiddenInput(ID.toString(), pet.id());
            var btn = form.addSubmitButton(Css.btn, Css.btnSm, Css.btnSuccess);
            if (!Status.AVAILABLE.name().equals(pet.status())) btn.setAttr("disabled", "disabled");
            btn.addText("Buy");
        }
    }

    /** Renders edit form */
    private void editForm(Pet pet, Element tdActions) {
        try (var form = tdActions.addForm(Css.dInline, Css.ms1).setMethod(Html.V_GET).setAction("")) {
            form.addHiddenInput(ACTION.toString(), Action.EDIT.name());
            form.addHiddenInput(ID.toString(), pet.id());
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlinePrimary).addText("Edit");
        }
    }

    /** Renders delete form */
    private void deleteForm(Pet pet, Element tdActions) {
        try (var form = tdActions.addForm(Css.dInline, Css.ms1).setMethod(Html.V_POST).setAction("?action=" + Action.DELETE.name())) {
            form.addHiddenInput(ID.toString(), pet.id());
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlineDanger).addText("Delete");
        }
    }

    /** Servlet attributes */
    enum Attrib implements HttpParameter {
        /** Action parameter */
        ACTION("action"),
        /** Pet ID */
        ID("id"),
        /** Pet name */
        NAME("name"),
        /** Pet status */
        STATUS("status"),
        /** Category ID */
        CATEGORY_ID("categoryId");

        private final String paramName;

        Attrib(String name) {
            this.paramName = name;
        }

        @Override
        public String toString() {
            return paramName;
        }
    }

    /** Action types */
    enum Action {
        BUY, DELETE, SAVE, EDIT;

        public boolean equalsName(String name) {
            return name().equals(name);
        }
    }

    /** Pet statuses */
    enum Status {
        AVAILABLE("Available", Css.bgSuccess),
        PENDING("Pending", Css.bgWarning),
        SOLD("Sold", Css.bgSecondary);

        private final String displayName;
        private final String badgeClass;

        Status(String displayName, String badgeClass) {
            this.displayName = displayName;
            this.badgeClass = badgeClass;
        }

        /** Gets the display name */
        public String getDisplayName() {
            return displayName;
        }

        /** Gets the badge CSS class */
        public String getBadgeClass() {
            return badgeClass;
        }

        /** Finds status by string name */
        public static Status findByName(String name) {
            var result = SOLD;
            for (var status : values()) {
                if (status.name().equals(name)) {
                    result = status;
                    break;
                }
            }
            return result;
        }
    }

    /** CSS constants */
    static final class Css {
        static final String alignItemsCenter = "align-items-center";
        static final String badge = "badge";
        static final String bgSecondary = "bg-secondary";
        static final String bgSuccess = "bg-success";
        static final String bgWarning = "bg-warning";
        static final String borderBottom = "border-bottom";
        static final String btn = "btn";
        static final String btnOutlineDanger = "btn-outline-danger";
        static final String btnOutlinePrimary = "btn-outline-primary";
        static final String btnPrimary = "btn-primary";
        static final String btnSm = "btn-sm";
        static final String btnSuccess = "btn-success";
        static final String colMd2 = "col-md-2";
        static final String colMd3 = "col-md-3";
        static final String colMd4 = "col-md-4";
        static final String container = "container";
        static final String dFlex = "d-flex";
        static final String dInline = "d-inline";
        static final String formControl = "form-control";
        static final String formSelect = "form-select";
        static final String g3 = "g-3";
        static final String justifyContentBetween = "justify-content-between";
        static final String mb3 = "mb-3";
        static final String mb4 = "mb-4";
        static final String ms1 = "ms-1";
        static final String mt5 = "mt-5";
        static final String pb3 = "pb-3";
        static final String row = "row";
        static final String table = "table";
        static final String tableDark = "table-dark";
        static final String tableHover = "table-hover";
        static final String textPrimary = "text-primary";
        static final String w100 = "w-100";
    }
}