package org.ujorm.petstore;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.petstore.utilities.TransactionManager;
import org.ujorm.tools.Check;
import org.ujorm.petstore.Constants.Status;

/**
 * Main Avaje Application and Service wrapper.
 * This class orchestrates business logic using a DAO facade and transaction management.
 */
@Singleton
public class Services {

    /** Unified access to data objects */
    private final DaoFactory.DaoFacade dao;

    /** Manager for handling database transactions */
    private final TransactionManager tm;

    @Inject
    public Services(DaoFactory.DaoFacade dao, TransactionManager tm) {
        this.dao = dao;
        this.tm = tm;
    }

    /**
     * Gets all pets for display.
     * @return List of all pets in the store.
     */
    public List<Pet> getPets() {
        return tm.run(() -> dao.pet().findAll(0L));
    }

    /**
     * Gets all categories for the form.
     * @return List of all pet categories.
     */
    public List<Category> getCategories() {
        return tm.run(() -> dao.category().findAll(0L));
    }

    /**
     * Finds a specific pet by its identifier.
     * @param idNullable Optional identifier.
     * @return The found pet wrapped in an Optional, or Optional.empty() if not found or the ID is null.
     */
    public Optional<Pet> getPetById(Long idNullable) {
        return idNullable != null
                ? tm.run(() -> dao.pet().findById(idNullable))
                : Optional.empty();
    }

    /**
     * Gets the default customer for the pet store.
     * @return The default customer instance.
     * @throws IllegalStateException if the default customer is missing in the database.
     */
    public Customer getCurrentCustomer() {
        return dao.customer().findById(1L).orElseThrow(() ->
                new IllegalStateException("Default customer is missing."));
    }

    /**
     * Processes a pet purchase transaction.
     * @param petId Optional pet identifier.
     * @return The created order if successful, or null if petId was null.
     */
    public PetOrder buyPet(Long petId) {
        if (petId == null) {
            return null;
        }
        return tm.run(() -> {
            var pet = dao.pet().findById(petId).orElseThrow(() ->
                    new IllegalStateException("Pet not found."));

            if (!Status.AVAILABLE.equals(pet.status())) {
                throw new IllegalStateException("Pet is not available.");
            }

            var soldPet = new Pet(pet.id(), pet.name(), Status.SOLD, pet.category());
            dao.pet().update(soldPet);

            return dao.order().insert(new PetOrder(null, getCurrentCustomer(), soldPet));
        });
    }

    /**
     * Saves a new pet or updates an existing one.
     * @param id Optional identifier (null for new pets).
     * @param name Name of the pet.
     * @param status Current status.
     * @param categoryId Identifier of the assigned category.
     */
    public void savePet(Long id, String name, Status status, Long categoryId) {
        tm.run(() -> {
            var extName = Check.isEmpty(name) ? "?" : name;
            var category = getCategories().stream()
                    .filter(c -> c.id().equals(categoryId))
                    .findFirst()
                    .orElseThrow();

            if (id != null) {
                dao.pet().update(new Pet(id, extName, status, category));
            } else {
                dao.pet().insert(new Pet(null, extName, status, category));
            }
        });
    }

    /**
     * Deletes a pet from the store.
     * @param id Optional pet identifier.
     */
    public void deletePet(Long id) {
        if (id != null) {
            tm.run(() -> {
                var pet = dao.pet().findById(id).orElseThrow();
                dao.pet().delete(pet);
            });
        }
    }
}