package org.ujorm.petstore;

import org.junit.jupiter.api.Test;
import org.ujorm.petstore.Constants.Status;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple reference tests for persistence/service layer on H2.
 */
class ServicesDatabaseTest extends AbstractDatabaseTest {

    private final Services services = new Services(this::connection);

    @Override
    protected void initSchema(Connection connection) {
        new DatabaseInitializer().createTables(connection);
    }

    @Test
    void shouldLoadInitialPets() {
        var pets = services.getPets();

        assertEquals(4, pets.size());
        assertEquals("Rex", pets.getFirst().name());
        assertNotNull(pets.getFirst().category());
    }

    @Test
    void shouldInsertAndUpdatePet() {
        var categoryId = services.getCategories().getFirst().id();

        services.savePet(null, "Milo", Status.AVAILABLE, categoryId);
        var inserted = services.getPets().stream()
                .filter(p -> "Milo".equals(p.name()))
                .findFirst()
                .orElseThrow();

        services.savePet(inserted.id(), "Milo Updated", Status.PENDING, categoryId);
        var updated = services.getPetById(inserted.id()).orElseThrow();

        assertEquals("Milo Updated", updated.name());
        assertEquals(Status.PENDING, updated.status());
    }

    @Test
    void shouldBuyAvailablePetAndCreateOrder() {
        createDefaultCustomer();

        var availablePet = services.getPets().stream()
                .filter(p -> p.status() == Status.AVAILABLE)
                .findFirst()
                .orElseThrow();

        var order = services.buyPet(availablePet.id());
        var soldPet = services.getPetById(availablePet.id()).orElseThrow();

        assertNotNull(order);
        assertNotNull(order.id());
        assertEquals(Status.SOLD, soldPet.status());
        assertEquals(soldPet.id(), order.pet().id());
        assertEquals(1L, order.customer().id());
    }

    private void createDefaultCustomer() {
        try (var stmt = connection().prepareStatement("INSERT INTO customer (id, name) VALUES (?, ?)")) {
            stmt.setLong(1, 1L);
            stmt.setString(2, "Reference Customer");
            stmt.executeUpdate();
            connection().commit();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create default customer for test.", e);
        }
    }
}
