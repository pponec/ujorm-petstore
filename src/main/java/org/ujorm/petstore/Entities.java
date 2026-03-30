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
            String name
    ) {}

    /** Customer entity */
    @Table(name = "customer")
    public record Customer(
            /** The primary key */
            @Id
            Long id,

            /** The customer name */
            String name
    ) {}

    /** Pet entity */
    @Table(name = "pet")
    public record Pet(
            /** The primary key */
            @Id
            Long id,

            /** The pet name */
            String name,

            /** The pet status */
            Status status,

            /** The category */
            @Column(name = "category_id")
            Category category
    ) {}

    /** Order for a pet */
    @Table(name = "pet_order")
    public record PetOrder(
            /** The primary key */
            @Id
            Long id,

            /** The customer */
            @Column(name = "customer_id")
            Customer customer,

            /** The ordered pet */
            @Column(name = "pet_id")
            Pet pet
    ) {}

}