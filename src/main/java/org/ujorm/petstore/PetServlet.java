package org.ujorm.petstore;

import jakarta.servlet.annotation.WebServlet;
import org.ujorm.petstore.Constants.Css;
import org.ujorm.petstore.Constants.Msg;
import org.ujorm.petstore.Constants.Status;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.utilities.AbstractServlet;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.Html;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.ao.HttpParameter;
import org.ujorm.tools.web.request.HttpContext;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ujorm.petstore.PetServlet.Attrib.*;

/** Web presentation layer for PetStore */
@WebServlet(urlPatterns = "", loadOnStartup = 1)
public class PetServlet extends AbstractServlet {

    /** CSS link */
    static final String BOOTSTRAP_CSS = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";
    private static final Map<Status, String> STATUS_OPTIONS = createStatusOptions();

    /** Handles GET requests to display the UI */
    @Override
    protected void doGet(HttpContext ctx) {
        var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
        var petId = ctx.parameter(PET_ID, Long::parseLong);
        var pets = services().getPets();
        var categories = services().getCategories();
        var petToEdit = findPetToEdit(action, petId);

        try (var html = HtmlElement.of(ctx, BOOTSTRAP_CSS)) {
            renderPage(html, pets, petToEdit, categories, ctx);
        }
    }

    /** Handles POST requests to modify data using a standardized action detection */
    @Override
    protected void doPost(HttpContext ctx) throws IOException {
        var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
        var petId = ctx.parameter(PET_ID, Long::parseLong);
        var resultUrl = handlePostAction(action, petId, ctx);
        ctx.sendRedirect(resultUrl); // Prevents duplicate form submissions (PRG pattern)
    }

    private void renderPage(HtmlElement html, List<Pet> pets, Optional<Pet> petToEdit, List<Category> categories, HttpContext ctx) {
        try (var body = html.addBody(Css.container, Css.mt5)) {
            renderHeader(body, ctx);
            renderTable(body, pets);
            renderForm(body, petToEdit, categories);
        }
    }

    private Optional<Pet> findPetToEdit(Action action, Long petId) {
        return switch(action) {
            case EDIT -> services().getPetById(petId);
            default -> Optional.empty();
        };
    }

    private String handlePostAction(Action action, Long petId, HttpContext ctx) {
        var resultUrl = ctx.getPathSlash();
        switch (action) {
            case BUY -> services().buyPet(petId);
            case DELETE -> services().deletePet(petId);
            case EDIT -> resultUrl = buildEditUrl(resultUrl, petId);
            case SAVE -> savePet(ctx, petId);
            case UNKNOWN -> { }
        }
        return resultUrl;
    }

    private String buildEditUrl(String contextPath, Long petId) {
        return contextPath + "?" + ACTION + "=" + Action.EDIT + "&" + PET_ID + "=" + petId;
    }

    private void savePet(HttpContext ctx, Long petId) {
        services().savePet(
                petId,
                ctx.parameter(NAME, ""),
                ctx.parameter(STATUS, s -> Status.valueOf(s.toUpperCase())),
                ctx.parameter(CATEGORY_ID, Long::parseLong));
    }

    /**
     * Renders the page header
     * @param body The body element
     * @param ctx The exchage context
     */
    private void renderHeader(Element body, HttpContext ctx) {
        var contextPath = ctx.getPathSlash();
        try (var header = body.addDiv(
                Css.dFlex,
                Css.justifyContentBetween,
                Css.alignItemsCenter,
                Css.mb4,
                Css.borderBottom,
                Css.pb3)) {
            header.addHeading(1, "Ujorm PetStore", Css.textPrimary);
            header.addAnchor(contextPath)
                    .addImage(contextPath + Constants.IMG_LOGO, "Ujorm Logo")
                    .setAttr("width", 150).setAttr("height", 150);
        }
    }

    /**
     * Renders the table of available pets
     * @param body The body element
     * @param pets List of pets to display
     */
    private void renderTable(Element body, List<Pet> pets) {
        body.addHeading(2, Msg.TITLE_AVAILABLE_PETS, Css.mb3);
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
                    renderPetRow(tbody, pet);
                }
            }
        }
    }

    private void renderPetRow(Element tbody, Pet pet) {
        try (var row = tbody.addTableRow()) {
            row.addTableDetail().addText(pet.id());
            row.addTableDetail().addText(pet.name());

            var statusCss = statusCss(pet.status());
            var statusName = statusName(pet.status());
            row.addTableDetail().addSpan(Css.badge, statusCss).addText(statusName);
            row.addTableDetail().addText(categoryName(pet));
            buttonBar(pet, row.addTableDetail());
        }
    }

    private String categoryName(Pet pet) {
        return pet.category() != null ? pet.category().name() : "";
    }

    /**
     * Renders the form for adding or editing a pet
     * @param body The body element
     * @param petToEdit The pet to edit, or empty for a new pet
     * @param categories List of available categories
     */
    private void renderForm(Element body, Optional<Pet> petToEdit, List<Category> categories) {
        body.addHeading(2, petToEdit.isPresent() ? Msg.TITLE_EDIT_PET : Msg.TITLE_ADD_PET, Css.mt5);
        try (var form = body.addForm().setMethod(Html.V_POST).setAction("?" + ACTION + "=" + Action.SAVE)) {
            petToEdit.ifPresent(pet -> form.addHiddenInput(PET_ID, pet.id()));
            try (var row = form.addDiv(Css.row, Css.g3)) {
                try (var col = row.addDiv(Css.colMd4)) {
                    var petName = petToEdit.map(Pet::name).orElse("");
                    col.addTextInput(Css.formControl).setNameValue(NAME, petName).setAttr("placeholder", "Name");
                }
                try (var col = row.addDiv(Css.colMd3)) {
                    var selectValue = petToEdit.map(Pet::status).orElse(Status.AVAILABLE);
                    col.addSelect(Css.formSelect).setName(STATUS).addSelectOptions(selectValue, STATUS_OPTIONS);
                }
                try (var col = row.addDiv(Css.colMd3)) {
                    var select = col.addSelect(Css.formSelect).setName(CATEGORY_ID);
                    for (var cat : categories) {
                        var opt = select.addElement("option").setAttr("value", cat.id());
                        if (isSelectedCategory(petToEdit, cat)) {
                            opt.setAttr("selected", "selected");
                        }
                        opt.addText(cat.name());
                    }
                }
                row.addDiv(Css.colMd2).addSubmitButton(Css.btn, Css.btnPrimary, Css.w100).addText(Msg.BUTTON_SAVE);
            }
        }
    }

    private boolean isSelectedCategory(Optional<Pet> petToEdit, Category category) {
        return petToEdit
                .map(Pet::category)
                .map(Category::id)
                .map(categoryId -> categoryId.equals(category.id()))
                .orElse(false);
    }

    /** Renders action buttons with a hidden ID and standard action name */
    private void buttonBar(Pet pet, Element tdActions) {
        try (var form = tdActions.addForm(Css.dInline).setMethod(Html.V_POST)) {
            form.addHiddenInput(PET_ID, pet.id());
            var available = Status.AVAILABLE.equals(pet.status());
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnSuccess)
                    .setNameValue(ACTION, Action.BUY)
                    .setAttr(available ? null : "disabled", "disabled")
                    .addText(Msg.BUTTON_BUY);
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlinePrimary, Css.ms1)
                    .setNameValue(ACTION, Action.EDIT)
                    .addText(Msg.BUTTON_EDIT);
            form.addSubmitButton(Css.btn, Css.btnSm, Css.btnOutlineDanger, Css.ms1)
                    .setNameValue(ACTION, Action.DELETE)
                    .addText(Msg.BUTTON_DELETE);
        }
    }

    private static Map<Status, String> createStatusOptions() {
        var statuses = new EnumMap<Status, String>(Status.class);
        for (var status : Status.values()) {
            statuses.put(status, statusName(status));
        }
        return statuses;
    }

    /** Returns a label for the status */
    public static String statusName(Status status) {
        return switch (status) {
            case AVAILABLE -> Msg.STATUS_AVAILABLE;
            case PENDING -> Msg.STATUS_PENDING;
            default -> Msg.STATUS_SOLD;
        };
    }

    /** Returns CSS class for the status badge */
    public static String statusCss(Status status) {
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
        PET_ID,
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
