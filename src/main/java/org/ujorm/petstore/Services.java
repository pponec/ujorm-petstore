package org.ujorm.petstore;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

import org.ujorm.orm.core.EntityManager;
import org.ujorm.orm.dsl.SelectQuery;
import org.ujorm.orm.utils.EntityContext;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.petstore.meta.QCategory;
import org.ujorm.petstore.meta.QPet;
import org.ujorm.petstore.utilities.TransactionManager;
import org.ujorm.tools.Check;
import org.ujorm.petstore.Constants.Status;

/**
 * PetStore Service handling both business logic and data access.
 */
@Singleton
public class Services {

    private static final EntityContext CTX = EntityContext.ofDefault();
    private static final EntityManager<Pet, Long> PET_EM = CTX.entityManager(Pet.class);
    private static final EntityManager<Category, Long> CATEGORY_EM = CTX.entityManager(Category.class);
    private static final EntityManager<Customer, Long> CUSTOMER_EM = CTX.entityManager(Customer.class);
    private static final EntityManager<PetOrder, Long> ORDER_EM = CTX.entityManager(PetOrder.class);

    /** Manager for handling database transactions */
    private final TransactionManager tm;

    @Inject
    public Services(TransactionManager tm) {
        this.tm = tm;
    }

    /** Gets all pets including their categories. */
    public List<Pet> getPets() {
        return tm.run(() -> SelectQuery.run(tm.getConnection(), PET_EM, query -> query
                .columns(true)
                .column(QPet.category, QCategory.name)
                .tail("ORDER BY", QPet.id)
                .toList()));
    }

    /** Gets all categories. */
    public List<Category> getCategories() {
        return tm.run(() -> SelectQuery.run(tm.getConnection(), CATEGORY_EM, query -> query
                .columns(true)
                .tail("ORDER BY", QCategory.id)
                .toList()));
    }

    /** Finds a specific pet by its identifier. */
    public Optional<Pet> getPetById(Long idNullable) {
        return idNullable != null
                ? tm.run(() -> PET_EM.crud(tm.getConnection()).findById(idNullable))
                : Optional.empty();
    }

    /** Gets the default customer. */
    public Customer getCurrentCustomer() {
        return tm.run(() -> CUSTOMER_EM.crud(tm.getConnection()).findById(1L).orElseThrow(() ->
                new IllegalStateException("Default customer is missing.")));
    }

    /** Processes a pet purchase transaction. */
    public PetOrder buyPet(Long petId) {
        if (petId == null) {
            return null;
        }
        return tm.run(() -> {
            var conn = tm.getConnection();
            var pet = PET_EM.crud(conn).findById(petId).orElseThrow(() ->
                    new IllegalStateException("Pet not found."));

            if (!Status.AVAILABLE.equals(pet.status())) {
                throw new IllegalStateException("Pet is not available.");
            }

            var soldPet = new Pet(pet.id(), pet.name(), Status.SOLD, pet.category());
            PET_EM.crud(conn).update(soldPet);

            return ORDER_EM.crud(conn).insert(new PetOrder(null, getCurrentCustomer(), soldPet));
        });
    }

    /** Saves a new pet or updates an existing one. */
    public void savePet(Long id, String name, Status status, Long categoryId) {
        tm.run(() -> {
            var conn = tm.getConnection();
            var extName = Check.isEmpty(name) ? "?" : name;
            var category = CATEGORY_EM.crud(conn).findById(categoryId).orElseThrow();
            var pet = new Pet(id, extName, status, category);
            PET_EM.crud(conn).insertOrUpdate(pet);
        });
    }

    /** Deletes a pet from the store. */
    public void deletePet(Long id) {
        if (id != null) {
            tm.run(() -> {
                PET_EM.crud(tm.getConnection()).deleteById(id);
            });
        }
    }
}