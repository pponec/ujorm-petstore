package org.ujorm.petstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.PetOrder;

/** Main Spring Boot Application and Service wrapper */
@SpringBootApplication
@ServletComponentScan
public class AppPetStore {

    /** Service layer encapsulating business logic */
    @Service
    public static class Services {

        /** Data access component */
        private final Dao dao;

        /** Creates a new Services layer with injected DAO */
        public Services(Dao dao) {
            this.dao = dao;
        }

        /**
         * Gets the currently logged-in customer.
         * For demonstration purposes, this always returns the customer with ID 1.
         */
        public Customer getCurrentCustomer() {
            return dao.customer.findById(1L).orElseThrow(() ->
                    new IllegalStateException("Default customer (ID=1) is missing in the database.")
            );
        }

        /**
         * Processes a pet purchase in a single database transaction.
         * @param petId The ID of the pet to buy
         * @return The created order
         * @throws IllegalStateException if the pet is not found or not available
         */
        @Transactional
        public PetOrder buyPet(Long petId) {
            var pet = dao.pet.findById(petId)
                    .orElseThrow(() -> new IllegalStateException("Pet not found: " + petId));

            if (!"AVAILABLE".equals(pet.status())) {
                throw new IllegalStateException("Pet is not available for purchase: " + pet.name());
            }

            // Create new pet record with updated status (since Records are immutable)
            var soldPet = new Entities.Pet(pet.id(), pet.name(), "SOLD", pet.category());
            dao.pet.update(soldPet);

            // Create and save the order
            var customer = getCurrentCustomer();
            var order = new Entities.PetOrder(null, customer, soldPet);

            return dao.order.insert(order);
        }

        /** Exposes PetDao for read-only operations */
        public Dao.PetDao petDao() {
            return dao.pet;
        }

        /** Exposes CategoryDao for read-only operations */
        public Dao.CategoryDao categoryDao() {
            return dao.category;
        }
    }

    /** Application entry point */
    public static void main(String[] args) {
        SpringApplication.run(AppPetStore.class, args);
    }
}