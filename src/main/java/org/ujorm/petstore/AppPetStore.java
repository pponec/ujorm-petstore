package org.ujorm.petstore;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ujorm.petstore.Entities.Category;
import org.ujorm.petstore.Entities.Customer;
import org.ujorm.petstore.Entities.Pet;
import org.ujorm.petstore.Entities.PetOrder;
import org.ujorm.tools.Check;

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

        /** Finds a specific pet */
        public Optional<Pet> getPetById(Long id) {
            return dao.getPet().findById(id);
        }

        /** Gets the default customer */
        public Customer getCurrentCustomer() {
            return dao.getCustomer().findById(1L).orElseThrow(() ->
                    new IllegalStateException("Default customer is missing."));
        }

        /** Processes a pet purchase
         * @param petId Optional pet identeifier do nothing.
         */
        public PetOrder buyPet(Long petId) {
            if (petId == null) {
                return null;
            }
            var pet = dao.getPet().findById(petId)
                    .orElseThrow(() -> new IllegalStateException("Pet not found."));

            if (!"AVAILABLE".equals(pet.status())) {
                throw new IllegalStateException("Pet is not available.");
            }

            var soldPet = new Pet(pet.id(), pet.name(), "SOLD", pet.category());
            dao.getPet().update(soldPet);

            return dao.getOrder().insert(new PetOrder(null, getCurrentCustomer(), soldPet));
        }

        /** Saves or updates a pet */
        public void savePet(Long id, String name, String status, Long categoryId) {
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
         * @param id Optional pet identifier do nothong.
         */
        public void deletePet(Long id) {
            if (id != null) {
                var pet = dao.getPet().findById(id).orElseThrow();
                dao.getPet().delete(pet);
            }
        }
    }

    /** Application entry point */
    public static void main(String[] args) {
        SpringApplication.run(AppPetStore.class, args);
    }
}