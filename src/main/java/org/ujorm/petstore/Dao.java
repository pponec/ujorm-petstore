package org.ujorm.petstore;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.ujorm.orm.SqlQuery;
import org.ujorm.orm.core.EntityManager;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;

/** Data access wrapper for the PetStore */
@Repository
public class Dao {

    private static final EntityManager<Category, Long> CATEGORY_EM = EntityManager.of(Category.class);
    private static final EntityManager<Customer, Long> CUSTOMER_EM = EntityManager.of(Customer.class);
    private static final EntityManager<Pet, Long> PET_EM = EntityManager.of(Pet.class);
    private static final EntityManager<PetOrder, Long> ORDER_EM = EntityManager.of(PetOrder.class);

    /** Current data source managed by Spring */
    private final DataSource dataSource;

    /** Internal DAO instances */
    private final CategoryDao categoryDao = new CategoryDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final PetDao petDao = new PetDao();
    private final PetOrderDao orderDao = new PetOrderDao();

    /** Creates a new Dao with the given data source */
    public Dao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Provides access to categories */
    public CategoryDao getCategory() { return categoryDao; }

    /** Provides access to customers */
    public CustomerDao getCustomer() { return customerDao; }

    /** Provides access to pets */
    public PetDao getPet() { return petDao; }

    /** Provides access to orders */
    public PetOrderDao getOrder() { return orderDao; }

    /** Gets the current connection managed by Spring transaction */
    private Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

    /** Data access object for categories */
    public class CategoryDao {

        /** Finds all categories */
        public List<Category> findAll() {
            return CATEGORY_EM.crud(getConnection())
                    .selectWhere("", builder -> builder
                    .streamMap(CATEGORY_EM.mapper())
                    .toList());
        }
    }

    /** Data access object for customers */
    public class CustomerDao {

        /** Finds customer by ID */
        public Optional<Customer> findById(Long id) {
            return CUSTOMER_EM.crud(getConnection()).findById(id);
        }
    }

    /** Data access object for pets */
    public class PetDao {

        /** Finds all pets including their categories */
        public List<Pet> findAll() {
            var sql = """
                    SELECT ${COLUMNS}
                    FROM pet p
                    LEFT JOIN category c ON c.id = p.category_id
                    ORDER BY p.id
                    """;

            return SqlQuery.run(getConnection(), query -> query
                    .sql(sql)
                    .column("p.id", MetaPet.id)
                    .column("p.name", MetaPet.name)
                    .column("p.status", MetaPet.status)
                    .column("c.id", MetaPet.category, MetaCategory.id)
                    .column("c.name", MetaPet.category, MetaCategory.name)
                    .streamMap(PET_EM.mapper())
                    .toList());
        }

        /** Finds pet by ID */
        public Optional<Pet> findById(Long id) {
            return PET_EM.crud(getConnection()).findById(id);
        }

        /** Inserts a new pet */
        public Pet insert(Pet pet) {
            return PET_EM.crud(getConnection()).insert(pet);
        }

        /** Updates an existing pet */
        public void update(Pet pet) {
            PET_EM.crud(getConnection()).update(pet);
        }

        /** Deletes a pet */
        public void delete(Pet pet) {
            PET_EM.crud(getConnection()).delete(pet);
        }
    }

    /** Data access object for orders */
    public class PetOrderDao {

        /** Inserts a new order */
        public PetOrder insert(PetOrder order) {
            return ORDER_EM.crud(getConnection()).insert(order);
        }
    }

}