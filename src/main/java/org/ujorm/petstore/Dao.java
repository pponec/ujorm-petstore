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
import org.ujorm.petstore.utils.AbstractDao;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Data access wrapper for the PetStore */
public class Dao {

    private static final EntityContext CTX = EntityContext.ofDefault();
    private static final EntityManager<Pet, Long> PET_EM = CTX.entityManager(Pet.class);
    private static final EntityManager<Category, Long> CATEGORY_EM = CTX.entityManager(Category.class);
    private static final EntityManager<Customer, Long> CUSTOMER_EM = CTX.entityManager(Customer.class);
    private static final EntityManager<PetOrder, Long> ORDER_EM = CTX.entityManager(PetOrder.class);

    /** Data access object for categories */
    @Repository
    public static class CategoryDao extends AbstractDao {

        public CategoryDao(Supplier<Connection> connection) {
            super(connection);
        }

        /** Finds all categories */
        public List<Category> findAll(long fromId) {
            return SelectQuery.run(connection(), CATEGORY_EM, query -> query
                    .columnsOfDomain(true)
                    .where(QCategory.id.whereGe(fromId))
                    .tail("ORDER BY", QCategory.id)
                    .toList());
        }
    }

    /** Data access object for customers */
    @Repository
    public static class CustomerDao extends AbstractDao {

        public CustomerDao(Supplier<Connection> connection) {
            super(connection);
        }

        /** Finds customer by ID */
        public Optional<Customer> findById(Long id) {
            return CUSTOMER_EM.crud(connection()).findById(id);
        }
    }

    /** Data access object for pets */
    @Repository
    public static class PetDao extends AbstractDao {

        public PetDao(Supplier<Connection> connection) {
            super(connection);
        }

        /** Finds all pets including their categories */
        public List<Pet> findAll(long fromId) {
            return SelectQuery.run(connection(), PET_EM, query -> query
                    .columnsOfDomain(true)
                    .column(QPet.category, QCategory.name)
                    .where(QPet.id.whereGe(fromId))
                    .tail("ORDER BY", QPet.id)
                    .toList());
        }

        /** Finds pet by ID */
        public Optional<Pet> findById(Long id) {
            return PET_EM.crud(connection()).findById(id);
        }

        /** Inserts a new pet */
        public Pet insert(Pet pet) {
            return PET_EM.crud(connection()).insert(pet);
        }

        /** Updates an existing pet */
        public void update(Pet pet) {
            PET_EM.crud(connection()).update(pet);
        }

        /** Deletes a pet */
        public void deleteById(Long petId) {
            PET_EM.crud(connection()).deleteById(petId);
        }
    }

    /** Data access object for orders */
    @Repository
    public static class PetOrderDao  extends AbstractDao {

        public PetOrderDao(Supplier<Connection> connection) {
            super(connection);
        }

        /** Inserts a new order */
        public PetOrder insert(PetOrder order) {
            return ORDER_EM.crud(connection()).insert(order);
        }
    }
}