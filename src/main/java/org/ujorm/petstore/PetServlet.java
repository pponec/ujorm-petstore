package org.ujorm.petstore;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.ujorm.tools.web.Element;

/** Web presentation layer for PetStore */
@WebServlet(urlPatterns = "/")
public class PetServlet extends HttpServlet {

    /** Injected business logic and data access */
    @Autowired
    private AppPetStore.Services services;

    /** Handles GET requests to display the UI */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var action = req.getParameter("action");
        Entities.Pet petToEdit = null;

        if ("edit".equals(action)) {
            var id = Long.valueOf(req.getParameter("id"));
            petToEdit = services.petDao().findById(id).orElse(null);
        }

        renderPage(resp, petToEdit);
    }

    /** Handles POST requests to modify data */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var action = req.getParameter("action");

        if ("buy".equals(action)) {
            var id = Long.valueOf(req.getParameter("id"));
            services.buyPet(id);
        } else if ("delete".equals(action)) {
            var id = Long.valueOf(req.getParameter("id"));
            var pet = services.petDao().findById(id).orElseThrow();
            services.petDao().delete(pet);
        } else if ("save".equals(action)) {
            var idParam = req.getParameter("id");
            var name = req.getParameter("name");
            var status = req.getParameter("status");
            var categoryId = Long.valueOf(req.getParameter("categoryId"));

            var category = services.categoryDao().findAll().stream()
                    .filter(c -> c.id().equals(categoryId)).findFirst().orElseThrow();

            if (idParam != null && !idParam.isEmpty()) {
                var pet = new Entities.Pet(Long.valueOf(idParam), name, status, category);
                services.petDao().update(pet);
            } else {
                var pet = new Entities.Pet(null, name, status, category);
                services.petDao().insert(pet);
            }
        }

        resp.sendRedirect(req.getContextPath() + "/");
    }

    /** Renders the main HTML page */
    private void renderPage(HttpServletResponse resp, Entities.Pet petToEdit) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        var bootstrapCss = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";

        try (var html = Element.createHtmlRoot("Ujorm PetStore", StandardCharsets.UTF_8, bootstrapCss)) {
            try (var body = html.addBody("container", "mt-5")) {

                // Header with Title and Logo
                try (var header = body.addDiv("d-flex", "justify-content-between", "align-items-center", "mb-4", "border-bottom", "pb-3")) {
                    header.addHeading(1, "Ujorm PetStore", "text-primary");
                    header.addAnchor("#")
                            .addImage("images/logo-ujorm.png", "Ujorm Logo")
                            .setAttr("width", 150)
                            .setAttr("height", 150);
                }

                // Pets Table
                body.addHeading(2, "Available Pets", "mb-3");
                try (var table = body.addTable("table", "table-hover", "align-middle")) {
                    try (var headRow = table.addTableHead("table-dark").addTableRow()) {
                        headRow.addTableDetail().addText("ID");
                        headRow.addTableDetail().addText("Name");
                        headRow.addTableDetail().addText("Status");
                        headRow.addTableDetail().addText("Category");
                        headRow.addTableDetail().addText("Actions");
                    }
                    try (var tbody = table.addTableBody()) {
                        for (var pet : services.petDao().findAll()) {
                            try (var row = tbody.addTableRow()) {
                                row.addTableDetail().addText(pet.id());
                                row.addTableDetail().addText(pet.name());

                                // Colorize status
                                var badgeClass = "AVAILABLE".equals(pet.status()) ? "bg-success" :
                                        ("SOLD".equals(pet.status()) ? "bg-secondary" : "bg-warning");
                                row.addTableDetail().addSpan("badge", badgeClass).addText(pet.status());

                                row.addTableDetail().addText(pet.category() != null ? pet.category().name() : "");

                                try (var tdActions = row.addTableDetail()) {
                                    // Buy action
                                    try (var form = tdActions.addForm("d-inline", "me-1").setMethod("POST").setAction("?action=buy")) {
                                        form.addHiddenInput("id", pet.id());
                                        var btn = form.addSubmitButton("btn", "btn-sm", "btn-success").addText("Buy");
                                        if (!"AVAILABLE".equals(pet.status())) {
                                            btn.setAttr("disabled", "disabled");
                                        }
                                    }
                                    // Edit action
                                    try (var form = tdActions.addForm("d-inline", "me-1").setMethod("GET").setAction("")) {
                                        form.addHiddenInput("action", "edit");
                                        form.addHiddenInput("id", pet.id());
                                        form.addSubmitButton("btn", "btn-sm", "btn-outline-primary").addText("Edit");
                                    }
                                    // Delete action
                                    try (var form = tdActions.addForm("d-inline").setMethod("POST").setAction("?action=delete")) {
                                        form.addHiddenInput("id", pet.id());
                                        form.addSubmitButton("btn", "btn-sm", "btn-outline-danger").addText("Delete");
                                    }
                                }
                            }
                        }
                    }
                }

                // Add / Edit Form
                body.addHeading(2, petToEdit == null ? "Add New Pet" : "Edit Pet", "mt-5", "mb-3");
                try (var form = body.addForm().setMethod("POST").setAction("?action=save")) {
                    if (petToEdit != null) {
                        form.addHiddenInput("id", petToEdit.id());
                    }

                    try (var row = form.addDiv("row", "g-3", "align-items-end")) {
                        try (var col = row.addDiv("col-md-4")) {
                            col.addLabel("form-label").addText("Name");
                            col.addTextInput("form-control").setName("name")
                                    .setValue(petToEdit != null ? petToEdit.name() : "")
                                    .setAttr("required", "required");
                        }
                        try (var col = row.addDiv("col-md-3")) {
                            col.addLabel("form-label").addText("Status");
                            var statuses = new LinkedHashMap<String, String>();
                            statuses.put("AVAILABLE", "Available");
                            statuses.put("PENDING", "Pending");
                            statuses.put("SOLD", "Sold");
                            col.addSelect("form-select").setName("status")
                                    .addSelectOptions(petToEdit != null ? petToEdit.status() : "AVAILABLE", statuses);
                        }
                        try (var col = row.addDiv("col-md-3")) {
                            col.addLabel("form-label").addText("Category");
                            var select = col.addSelect("form-select").setName("categoryId");
                            var currentCatId = petToEdit != null && petToEdit.category() != null ? petToEdit.category().id() : null;
                            for (var cat : services.categoryDao().findAll()) {
                                select.addElement("option")
                                        .setAttr("value", cat.id())
                                        .setAttr("selected", cat.id().equals(currentCatId) ? "selected" : null)
                                        .addText(cat.name());
                            }
                        }
                        try (var col = row.addDiv("col-md-2")) {
                            form.addSubmitButton("btn", "btn-primary", "w-100").addText("Save Pet");
                        }
                    }
                    if (petToEdit != null) {
                        form.addAnchor("?", "btn", "btn-link", "mt-2").addText("Cancel Edit");
                    }
                }
            }
            resp.getWriter().write(html.toString());
        }
    }
}