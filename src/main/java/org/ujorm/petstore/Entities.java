package org.ujorm.petstore;

/** Wrapper class for all domain entities */
public class Entities {

    /** Category of the pet */
    @Table("category")
    public record Category(
            /** Gets the primary key */
            @Id
            @Column("id")
            Long id,

            /** Gets the category name */
            @Column("name")
            String name
    ) {}

    /** Customer entity */
    @Table("customer")
    public record Customer(
            /** Gets the primary key */
            @Id
            @Column("id")
            Long id,

            /** Gets the customer name */
            @Column("name")
            String name
    ) {}

    /** Pet entity */
    @Table("pet")
    public record Pet(
            /** Gets the primary key */
            @Id
            @Column("id")
            Long id,

            /** Gets the pet name */
            @Column("name")
            String name,

            /** Gets the pet status */
            @Column("status")
            String status,

            /** Gets the category */
            @Column("category_id")
            Category category
    ) {}

    /** Order for a pet */
    @Table("pet_order")
    public record PetOrder(
            /** Gets the primary key */
            @Id
            @Column("id")
            Long id,

            /** Gets the customer */
            @Column("customer_id")
            Customer customer,

            /** Gets the ordered pet */
            @Column("pet_id")
            Pet pet
    ) {}

}