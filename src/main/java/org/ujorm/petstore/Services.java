package org.ujorm.petstore;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.ujorm.orm.core.EntityManager;
import org.ujorm.orm.dsl.SelectQuery;
import org.ujorm.orm.utils.EntityContext;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.petstore.meta.QCategory;
import org.ujorm.petstore.meta.QPet;
import org.ujorm.tools.Check;
import org.ujorm.petstore.Constants.Status;

/**
 * PetStore Service handling both business logic and data access using Ujorm3 ORM.
 * <p>
 *   This class utilizes a {@code Supplier<Connection>} to access the database, offloading
 *   transaction management to a {@code TransactionFilter} that wraps each HTTP request.
 *   This architectural approach enhances code readability and maintainability
 *   by eliminating repetitive transaction boilerplate, allowing the service methods to focus
 *   purely on business logic and the clarity of the ORM API while ensuring transactional
 *   integrity through the injected supplier.
 * </p>
 * <p>
 *   To manage transactions explicitly, replace the {@code Supplier<Connection>} with
 *   a {@code TransactionManager} attribute, wrap method bodies in {@code tm.run(() -> { ... })}
 *   blocks, and disable the {@code TransactionFilter} registration in the {@code Bootstrap} class.
 * </p>
 * Example of the explicit transactions:
 * <pre>
 * tm.run(() -> PET_EM.crud(tm.getConnection()).deleteById(id));
 * </pre>
 */
@Singleton
public class Services {

    private static final EntityContext CTX = EntityContext.ofDefault();
    private static final EntityManager<Pet, Long> PET_EM = CTX.entityManager(Pet.class);
    private static final EntityManager<Category, Long> CATEGORY_EM = CTX.entityManager(Category.class);
    private static final EntityManager<Customer, Long> CUSTOMER_EM = CTX.entityManager(Customer.class);
    private static final EntityManager<PetOrder, Long> ORDER_EM = CTX.entityManager(PetOrder.class);

    /** Provider of the current transaction connection */
    private final Supplier<Connection> connection;

    @Inject
    public Services(Supplier<Connection> connection) {
        this.connection = connection;
    }

    /** Gets all pets including their categories. */
    public List<Pet> getPets() {
        return SelectQuery.run(connection(), PET_EM, query -> query
                .columns(true)
                .column(QPet.category, QCategory.name)
                .tail("ORDER BY", QPet.id)
                .toList());
    }

    /** Gets all categories. */
    public List<Category> getCategories() {
        return SelectQuery.run(connection(), CATEGORY_EM, query -> query
                .columns(true)
                .tail("ORDER BY", QCategory.id)
                .toList());
    }

    /** Finds a specific pet by its identifier. */
    public Optional<Pet> getPetById(Long idNullable) {
        return idNullable != null
                ? PET_EM.crud(connection()).findById(idNullable)
                : Optional.empty();
    }

    /** Gets the default customer. */
    public Customer getCurrentCustomer() {
        return CUSTOMER_EM.crud(connection()).findById(1L).orElseThrow(() ->
                new IllegalStateException("Default customer is missing."));
    }

    /** Processes a pet purchase transaction. */
    public PetOrder buyPet(Long petId) {
        if (petId == null) {
            return null;
        }
        var pet = PET_EM.crud(connection()).findById(petId).orElseThrow(() ->
                new IllegalStateException("Pet not found."));

        if (!Status.AVAILABLE.equals(pet.status())) {
            throw new IllegalStateException("Pet is not available.");
        }

        var soldPet = new Pet(pet.id(), pet.name(), Status.SOLD, pet.category());
        PET_EM.crud(connection()).update(soldPet);

        return ORDER_EM.crud(connection()).insert(new PetOrder(null, getCurrentCustomer(), soldPet));
    }

    /** Saves a new pet or updates an existing one. */
    public void savePet(Long id, String name, Status status, Long categoryId) {
        var extName = Check.isEmpty(name) ? "?" : name;
        var category = CATEGORY_EM.crud(connection()).findById(categoryId).orElseThrow();
        var pet = new Pet(id, extName, status, category);
        PET_EM.crud(connection()).insertOrUpdate(pet);
    }

    /** Deletes a pet from the store. */
    public void deletePet(Long id) {
        if (id != null) {
            PET_EM.crud(connection()).deleteById(id);
        }
    }

    /** Returns the current database connection. */
    private Connection connection() {
        return connection.get();
    }
}