package org.ujorm.petstore;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
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
import org.ujorm.petstore.Entities.User;
import org.ujorm.tools.Check;
import org.ujorm.petstore.Constants.Status;
import org.ujorm.petstore.Constants.Role;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ujorm.petstore.Constants.Text;

/** Main Spring Boot Application and Service wrapper */
@SpringBootApplication
@ServletComponentScan
public class AppPetStore {

    /** Component to initialize data on startup */
    @Component
    public static class DataInitializer {
        private final Services services;

        public DataInitializer(Services services) {
            this.services = services;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void init() {
            if (services.getUsers().isEmpty()) {
                services.saveUser(null, Text.DEFAULT_USER, Text.DEFAULT_PASSWORD, Role.ADMIN, true);
            }
        }
    }

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
            return dao.getPet().findAll(0L);
        }

        /** Gets all categories for the form */
        public List<Category> getCategories() {
            return dao.getCategory().findAll(0L);
        }

        /** Finds a specific pet */
        public Optional<Pet> getPetById(Long idNullable) {
            return idNullable != null ? dao.getPet().findById(idNullable) : Optional.empty();
        }

        /** Gets the default customer */
        public Customer getCurrentCustomer() {
            return dao.getCustomer().findById(1L).orElseThrow(() ->
                    new IllegalStateException("Default customer is missing."));
        }

        /** Processes a pet purchase */
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

        /** Deletes a pet */
        public void deletePet(Long id) {
            if (id != null) {
                var pet = dao.getPet().findById(id).orElseThrow();
                dao.getPet().delete(pet);
            }
        }

        /** Authenticates a user */
        public Optional<User> login(String login, String password) {
            if (Check.isEmpty(login) || Check.isEmpty(password)) {
                return Optional.empty();
            }
            return dao.getUser().findByLogin(login)
                    .filter(u -> u.active())
                    .filter(u -> PasswordUtils.verifyPassword(password, u.password()));
        }

        /** Gets all users */
        public List<User> getUsers() {
            return dao.getUser().findAll();
        }

        /** Gets user by ID */
        public Optional<User> getUserById(Long id) {
            return id != null ? dao.getUser().findById(id) : Optional.empty();
        }

        /** Saves or updates a user */
        public void saveUser(Long id, String login, String password, Role role, boolean active) {
            if (Check.isEmpty(login)) {
                return;
            }
            var existingByLogin = dao.getUser().findByLogin(login);
            if (existingByLogin.isPresent() && (id == null || !existingByLogin.get().id().equals(id))) {
                throw new IllegalArgumentException(Text.ERR_DUPLICATE_LOGIN);
            }

            if (id != null) {
                var existing = dao.getUser().findById(id).orElseThrow();
                var newPassword = Check.hasLength(password) ? PasswordUtils.hashPassword(password) : existing.password();
                dao.getUser().update(new User(id, login, newPassword, role, active));
            } else {
                if (Check.hasLength(password)) {
                    dao.getUser().insert(new User(null, login, PasswordUtils.hashPassword(password), role, active));
                }
            }
        }
    }

    /** Utility for password hashing using PBKDF2 */
    public static class PasswordUtils {
        private static final int ITERATIONS = 10000;
        private static final int KEY_LENGTH = 256;
        private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

        public static String hashPassword(String password) {
            var random = new SecureRandom();
            var salt = new byte[16];
            random.nextBytes(salt);
            var hash = hash(password.toCharArray(), salt, ITERATIONS);
            return ITERATIONS + ":" + toHex(salt) + ":" + toHex(hash);
        }

        public static boolean verifyPassword(String password, String storedHash) {
            var parts = storedHash.split(":");
            if (parts.length != 3) return false;
            try {
                var iterations = Integer.parseInt(parts[0]);
                var salt = fromHex(parts[1]);
                var hash = fromHex(parts[2]);
                var testHash = hash(password.toCharArray(), salt, iterations);
                return java.util.Arrays.equals(hash, testHash);
            } catch (Exception e) {
                return false;
            }
        }

        private static byte[] hash(char[] password, byte[] salt, int iterations) {
            try {
                var factory = SecretKeyFactory.getInstance(ALGORITHM);
                var spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
                return factory.generateSecret(spec).getEncoded();
            } catch (Exception e) {
                throw new RuntimeException("Error hashing password", e);
            }
        }

        private static String toHex(byte[] array) {
            var hex = new StringBuilder(array.length * 2);
            for (byte b : array) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }

        private static byte[] fromHex(String hex) {
            var bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
            }
            return bytes;
        }
    }

    /** Provides a supplier of transaction-aware JDBC Connections */
    @Bean
    public Supplier<Connection> connectionSupplier(DataSource dataSource) {
        return () -> DataSourceUtils.getConnection(dataSource);
    }

    /** Application entry point */
    public static void main(String[] args) {
        SpringApplication.run(AppPetStore.class, args);
    }
}
