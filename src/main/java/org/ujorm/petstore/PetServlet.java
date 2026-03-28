package org.ujorm.petstore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.ujorm.tools.web.Element;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Pet;

/** Web presentation layer for PetStore */
@WebServlet(urlPatterns = "")
public class PetServlet extends HttpServlet {

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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Pet petToEdit = null;
        var action = req.getParameter("action");
        var idParam = req.getParameter("id");

        if ("edit".equals(action) && idParam != null) {
            petToEdit = services.getPetById(Long.valueOf(idParam)).orElse(null);
        }

        renderPage(resp, petToEdit, services.getPets(), services.getCategories(), req.getContextPath());
    }

    /** Handles POST requests to modify data */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var action = req.getParameter("action");
        var idParam = req.getParameter("id");

        if ("buy".equals(action) && idParam != null) {
            services.buyPet(Long.valueOf(idParam));
        } else if ("delete".equals(action) && idParam != null) {
            services.deletePet(Long.valueOf(idParam));
        } else if ("save".equals(action)) {
            services.savePet(
                    (idParam != null && !idParam.isEmpty()) ? Long.valueOf(idParam) : null,
                    req.getParameter("name"),
                    req.getParameter("status"),
                    Long.valueOf(req.getParameter("categoryId"))
            );
        }

        resp.sendRedirect(req.getContextPath() + "/");
    }

    /** Renders the main HTML page */
    private void renderPage(HttpServletResponse resp, Pet petToEdit, List<Pet> pets, List<Category> categories, String contextPath) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        var bootstrapCss = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";

        try (var html = Element.createHtmlRoot("Ujorm PetStore", StandardCharsets.UTF_8, bootstrapCss)) {
            try (var body = html.addBody("container", "mt-5")) {

                // Header
                try (var header = body.addDiv("d-flex", "justify-content-between", "align-items-center", "mb-4", "border-bottom", "pb-3")) {
                    header.addHeading(1, "Ujorm PetStore", "text-primary");
                    header.addAnchor(contextPath + "/")
                            .addImage(contextPath + "/images/logo-ujorm.png", "Ujorm Logo")
                            .setAttr("width", 150).setAttr("height", 150);
                }

                // Table
                body.addHeading(2, "Available Pets", "mb-3");
                try (var table = body.addTable("table", "table-hover")) {
                    try (var headRow = table.addTableHead("table-dark").addTableRow()) {
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
                                var badgeClass = "AVAILABLE".equals(pet.status()) ? "bg-success" : "bg-secondary";
                                row.addTableDetail().addSpan("badge", badgeClass).addText(pet.status());
                                row.addTableDetail().addText(pet.category() != null ? pet.category().name() : "");

                                try (var tdActions = row.addTableDetail()) {
                                    // Buy form
                                    try (var form = tdActions.addForm("d-inline").setMethod("POST").setAction("?action=buy")) {
                                        form.addHiddenInput("id", pet.id());
                                        var btn = form.addSubmitButton("btn", "btn-sm", "btn-success");
                                        if (!"AVAILABLE".equals(pet.status())) btn.setAttr("disabled", "disabled");
                                        btn.addText("Buy");
                                    }
                                    // Edit form
                                    try (var form = tdActions.addForm("d-inline", "ms-1").setMethod("GET").setAction("")) {
                                        form.addHiddenInput("action", "edit");
                                        form.addHiddenInput("id", pet.id());
                                        form.addSubmitButton("btn", "btn-sm", "btn-outline-primary").addText("Edit");
                                    }
                                    // Delete form
                                    try (var form = tdActions.addForm("d-inline", "ms-1").setMethod("POST").setAction("?action=delete")) {
                                        form.addHiddenInput("id", pet.id());
                                        form.addSubmitButton("btn", "btn-sm", "btn-outline-danger").addText("Delete");
                                    }
                                }
                            }
                        }
                    }
                }

                // Form
                body.addHeading(2, petToEdit == null ? "Add New Pet" : "Edit Pet", "mt-5");
                try (var form = body.addForm().setMethod("POST").setAction("?action=save")) {
                    if (petToEdit != null) form.addHiddenInput("id", petToEdit.id());
                    try (var row = form.addDiv("row", "g-3")) {
                        try (var col = row.addDiv("col-md-4")) {
                            col.addTextInput("form-control").setName("name").setValue(petToEdit != null ? petToEdit.name() : "").setAttr("placeholder", "Name");
                        }
                        try (var col = row.addDiv("col-md-3")) {
                            var statuses = new LinkedHashMap<String, String>();
                            statuses.put("AVAILABLE", "Available"); statuses.put("SOLD", "Sold");
                            col.addSelect("form-select").setName("status").addSelectOptions(petToEdit != null ? petToEdit.status() : "AVAILABLE", statuses);
                        }
                        try (var col = row.addDiv("col-md-3")) {
                            var select = col.addSelect("form-select").setName("categoryId");
                            for (var cat : categories) {
                                var opt = select.addElement("option").setAttr("value", cat.id());
                                if (petToEdit != null && petToEdit.category() != null && cat.id().equals(petToEdit.category().id())) {
                                    opt.setAttr("selected", "selected");
                                }
                                opt.addText(cat.name());
                            }
                        }
                        row.addDiv("col-md-2").addSubmitButton("btn", "btn-primary", "w-100").addText("Save");
                    }
                }
            }
            resp.getWriter().write(html.toString());
        }
    }
}