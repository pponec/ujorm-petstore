package org.ujorm.petstore;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import org.ujorm.orm.core.EntityManager;
import org.ujorm.orm.dsl.SelectQuery;
import org.ujorm.orm.utils.EntityContext;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.petstore.meta.QCategory;
import org.ujorm.petstore.meta.QPet;
import org.ujorm.petstore.utilities.AbstractDao;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Factory wrapper for data access objects */
@Factory
public class DaoFactory {

    private static final EntityContext CTX = EntityContext.ofDefault();
    private static final EntityManager<Pet, Long> PET_EM = CTX.entityManager(Pet.class);
    private static final EntityManager<Category, Long> CATEGORY_EM = CTX.entityManager(Category.class);
    private static final EntityManager<Customer, Long> CUSTOMER_EM = CTX.entityManager(Customer.class);
    private static final EntityManager<PetOrder, Long> ORDER_EM = CTX.entityManager(PetOrder.class);

    /** DAO facade grouping all data access objects */
    public record DaoFacade(
            /** Data access for categories */
            CategoryDao category,

            /** Data access for customers */
            CustomerDao customer,

            /** Data access for pets */
            PetDao pet,

            /** Data access for orders */
            PetOrderDao order
    ) {}

    /** Data access object for categories */
    public static class CategoryDao extends AbstractDao {
        public CategoryDao(Supplier<Connection> connection) { super(connection); }

        /** Finds all categories */
        public List<Category> findAll(long fromId) {
            return SelectQuery.run(connection(), CATEGORY_EM, query -> query
                    .columns(true)
                    .where(QCategory.id.whereGe(fromId))
                    .tail("ORDER BY", QCategory.id)
                    .toList());
        }
    }

    /** Data access object for customers */
    public static class CustomerDao extends AbstractDao {
        public CustomerDao(Supplier<Connection> connection) { super(connection); }

        /** Finds customer by ID */
        public Optional<Customer> findById(Long id) {
            return CUSTOMER_EM.crud(connection()).findById(id);
        }
    }

    /** Data access object for pets */
    public static class PetDao extends AbstractDao {
        public PetDao(Supplier<Connection> connection) { super(connection); }

        /** Finds all pets including their categories */
        public List<Pet> findAll(long fromId) {
            return SelectQuery.run(connection(), PET_EM, query -> query
                    .columns(true)
                    .column(QPet.category, QCategory.name)
                    .where(QPet.id.whereGe(fromId))
                    .tail("ORDER BY", QPet.id)
                    .toList());
        }

        /** Finds pet by ID */
        public Optional<Pet> findById(Long id) { return PET_EM.crud(connection()).findById(id); }

        /** Inserts a new pet */
        public Pet insert(Pet p) { return PET_EM.crud(connection()).insert(p); }

        /** Updates an existing pet */
        public void update(Pet p) { PET_EM.crud(connection()).update(p); }

        /** Deletes a pet */
        public void delete(Pet p) { PET_EM.crud(connection()).delete(p); }
    }

    /** Data access object for orders */
    public static class PetOrderDao extends AbstractDao {
        public PetOrderDao(Supplier<Connection> connection) { super(connection); }

        /** Inserts a new order */
        public PetOrder insert(PetOrder pOrder) {
            return ORDER_EM.crud(connection()).insert(pOrder);
        }
    }

    /** Creates the DAO facade instance */
    @Bean
    public DaoFacade daoFacade(Supplier<Connection> connection) {
        return new DaoFacade(
                new CategoryDao(connection),
                new CustomerDao(connection),
                new PetDao(connection),
                new PetOrderDao(connection)
        );
    }
}