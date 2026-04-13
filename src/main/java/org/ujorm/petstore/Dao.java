package org.ujorm.petstore;

import org.springframework.stereotype.Repository;
import org.ujorm.orm.core.EntityManager;
import org.ujorm.orm.dsl.SelectQuery;
import org.ujorm.orm.utils.EntityContext;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.petstore.meta.QCategory;
import org.ujorm.petstore.meta.QPet;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Data access wrapper for the PetStore */
@Repository
public class Dao {

    private static final EntityContext CTX = EntityContext.ofDefault();
    private static final EntityManager<Pet, Long> PET_EM = CTX.entityManager(Pet.class);
    private static final EntityManager<Category, Long> CATEGORY_EM = CTX.entityManager(Category.class);
    private static final EntityManager<Customer, Long> CUSTOMER_EM = CTX.entityManager(Customer.class);
    private static final EntityManager<PetOrder, Long> ORDER_EM = CTX.entityManager(PetOrder.class);

    /** Provider of the transaction-aware connection */
    private final Supplier<Connection> connection;

    /** Internal DAO instances */
    private final CategoryDao categoryDao = new CategoryDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final PetDao petDao = new PetDao();
    private final PetOrderDao orderDao = new PetOrderDao();

    /** Creates a new Dao with the connection supplier */
    public Dao(Supplier<Connection> connection) {
        this.connection = connection;
    }

    /** Provides access to categories */
    public CategoryDao getCategory() { return categoryDao; }

    /** Provides access to customers */
    public CustomerDao getCustomer() { return customerDao; }

    /** Provides access to pets */
    public PetDao getPet() { return petDao; }

    /** Provides access to orders */
    public PetOrderDao getOrder() { return orderDao; }

    /** Data access object for categories */
    public class CategoryDao {

        /** Finds all categories */
        public List<Category> findAll(long fromId) {
            return SelectQuery.run(connection.get(), CATEGORY_EM, query -> query
                    .columnsOfDomain(true)
                    .where(QCategory.id.whereGe(fromId))
                    .tail("ORDER BY", QCategory.id)
                    .toList());
        }
    }

    /** Data access object for customers */
    public class CustomerDao {

        /** Finds customer by ID */
        public Optional<Customer> findById(Long id) {
            return CUSTOMER_EM.crud(connection.get()).findById(id);
        }
    }

    /** Data access object for pets */
    public class PetDao {

        /** Finds all pets including their categories */
        public List<Pet> findAll(long fromId) {
            return SelectQuery.run(connection.get(), PET_EM, query -> query
                    .columnsOfDomain(true)
                    .column(QPet.category, QCategory.name)
                    .where(QPet.id.whereGe(fromId))
                    .tail("ORDER BY", QPet.id)
                    .toList());
        }

        /** Finds pet by ID */
        public Optional<Pet> findById(Long id) {
            return PET_EM.crud(connection.get()).findById(id);
        }

        /** Inserts a new pet */
        public Pet insert(Pet pet) {
            return PET_EM.crud(connection.get()).insert(pet);
        }

        /** Updates an existing pet */
        public void update(Pet pet) {
            PET_EM.crud(connection.get()).update(pet);
        }

        /** Deletes a pet */
        public void delete(Pet pet) {
            PET_EM.crud(connection.get()).delete(pet);
        }
    }

    /** Data access object for orders */
    public class PetOrderDao {

        /** Inserts a new order */
        public PetOrder insert(PetOrder order) {
            return ORDER_EM.crud(connection.get()).insert(order);
        }
    }
}