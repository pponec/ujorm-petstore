package org.ujorm.petstore;

import jakarta.persistence.*;
import org.ujorm.petstore.Constants.Status;

/** Wrapper class for all domain entities */
public class Entities {

    /** Category of the pet */
    @Table(name = "category")
    public record Category(
            /** The primary key */
            @Id
            Long id,

            /** The category name */
            @Column(nullable = false)
            String name
    ) {}

    /** Customer entity */
    @Table(name = "customer")
    public record Customer(
            /** The primary key */
            @Id
            Long id,

            /** The customer name */
            @Column(nullable = false)
            String name
    ) {}

    /** Pet entity */
    @Table(name = "pet")
    public record Pet(
            /** The primary key */
            @Id
            Long id,

            /** The pet name */
            @Column(nullable = false)
            String name,

            /** The pet status */
            @Column(nullable = false)
            Status status,

            /** The category */
            @Column(name = "category_id", nullable = false)
            Category category
    ) {}

    /** Order for a pet */
    @Table(name = "pet_order")
    public record PetOrder(
            /** The primary key */
            @Id
            Long id,

            /** The customer */
            @Column(name = "customer_id", nullable = false)
            Customer customer,

            /** The ordered pet */
            @Column(name = "pet_id", nullable = false)
            Pet pet
    ) {}

}