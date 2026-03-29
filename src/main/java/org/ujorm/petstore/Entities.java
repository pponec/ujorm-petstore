package org.ujorm.petstore;

import jakarta.persistence.*;
import org.ujorm.petstore.Constants.Status;

/** Wrapper class for all domain entities */
public class Entities {

    /** Category of the pet */
    @Table(name = "category")
    public record Category(
            /** Gets the primary key */
            @Id
            @Column(name = "id")
            Long id,

            /** Gets the category name */
            @Column(name = "name")
            String name
    ) {}

    /** Customer entity */
    @Table(name = "customer")
    public record Customer(
            /** Gets the primary key */
            @Id
            @Column(name = "id")
            Long id,

            /** Gets the customer name */
            @Column(name = "name")
            String name
    ) {}

    /** Pet entity */
    @Table(name = "pet")
    public record Pet(
            /** Gets the primary key */
            @Id
            @Column(name = "id")
            Long id,

            /** Gets the pet name */
            @Column(name = "name")
            String name,

            /** Gets the pet status */
            @Column(name = "status")
            Status status,

            /** Gets the category */
            @Column(name = "category_id")
            Category category
    ) {}

    /** Order for a pet */
    @Table(name = "pet_order")
    public record PetOrder(
            /** Gets the primary key */
            @Id
            @Column(name = "id")
            Long id,

            /** Gets the customer */
            @Column(name = "customer_id")
            Customer customer,

            /** Gets the ordered pet */
            @Column(name = "pet_id")
            Pet pet
    ) {}

}