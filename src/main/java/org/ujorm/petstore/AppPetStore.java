package org.ujorm.petstore;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.tools.Check;
import org.ujorm.petstore.Constants.Status;

/** Main Spring Boot Application and Service wrapper */
@SpringBootApplication
@ServletComponentScan
public class AppPetStore {

    /** Service layer encapsulating business logic */
    @Service
    @Transactional
    public static class Services {

        /** Data access component */
        private final Dao dao;

        /** Creates a new Services layer with injected DAO */
        public Services(Dao dao) {
            this.dao = dao;
        }

        /** Gets all pets for display */
        public List<Pet> getPets() {
            return dao.getPet().findAll();
        }

        /** Gets all categories for the form */
        public List<Category> getCategories() {
            return dao.getCategory().findAll();
        }

        /**
         * Finds a specific pet.
         * @param idNullable Optional identifier.
         * @return The found pet wrapped in an Optional, or Optional.empty() if not found or the ID is null.
         */
        public Optional<Pet> getPetById(Long idNullable) {
            return idNullable != null ? dao.getPet().findById(idNullable) : Optional.empty();
        }

        /** Gets the default customer */
        public Customer getCurrentCustomer() {
            return dao.getCustomer().findById(1L).orElseThrow(() ->
                    new IllegalStateException("Default customer is missing."));
        }

        /**
         * Processes a pet purchase
         * @param petId Optional pet identifier do nothing.
         */
        public PetOrder buyPet(Long petId) {
            if (petId == null) {
                return null;
            }
            var pet = dao.getPet().findById(petId).orElseThrow(() ->
                    new IllegalStateException("Pet not found."));

            if (!Status.AVAILABLE.equals(pet.status())) {
                throw new IllegalStateException("Pet is not available.");
            }

            var soldPet = new Pet(pet.id(), pet.name(), Status.SOLD, pet.category());
            dao.getPet().update(soldPet);

            return dao.getOrder().insert(new PetOrder(null, getCurrentCustomer(), soldPet));
        }

        /** Saves or updates a pet */
        public void savePet(Long id, String name, Status status, Long categoryId) {
            var extName = Check.isEmpty(name) ? "?" : name;
            var category = getCategories().stream()
                    .filter(c -> c.id().equals(categoryId)).findFirst().orElseThrow();

            if (id != null) {
                dao.getPet().update(new Pet(id, extName, status, category));
            } else {
                dao.getPet().insert(new Pet(null, extName, status, category));
            }
        }

        /**
         * Deletes a pet
         * @param id Optional pet identifier do nothing.
         */
        public void deletePet(Long id) {
            if (id != null) {
                var pet = dao.getPet().findById(id).orElseThrow();
                dao.getPet().delete(pet);
            }
        }
    }

    /**
     * Provides a supplier of standard JDBC Connections fully managed by the Spring Framework.
     * <p>
     * The supplied connection is <strong>transaction-aware</strong>. It uses {@link DataSourceUtils}
     * to either retrieve an existing connection bound to the current thread
     * (if a transaction is active) or creates a new one.
     * </p>
     * <p>
     * <strong>Lifecycle Management:</strong> The opening and closing of the connection
     * is automatically handled by Spring's {@code PlatformTransactionManager}.
     * This is typically triggered by the {@code @Transactional} annotation in the service layer.
     * Manual closing of the connection is not required and should be avoided to prevent
     * breaking the transaction synchronization.
     * </p>
     *
     * @param dataSource the underlying data source managed by Spring
     * @return a Supplier providing a transaction-aware JDBC Connection
     * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection(DataSource)
     */
    @Bean
    public Supplier<Connection> connectionSupplier(DataSource dataSource) {
        return () -> DataSourceUtils.getConnection(dataSource);
    }

    /** Application entry point */
    public static void main(String[] args) {
        SpringApplication.run(AppPetStore.class, args);
    }
}