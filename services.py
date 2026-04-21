from typing import List, Optional
from sqlmodel import Session, select
from models import Pet, Category, Customer, PetOrder
from constants import Status

class Services:
    """Main business logic and data access orchestrator."""

    DEFAULT_CUSTOMER_ID: int = 1

    class Error(Exception):
        """Internal service error."""
        pass

    @staticmethod
    def get_pets(session: Session) -> List[Pet]:
        """Gets all pets for display."""
        statement = select(Pet).order_by(Pet.id)
        return session.exec(statement).all()

    @staticmethod
    def get_categories(session: Session) -> List[Category]:
        """Gets all categories for the form."""
        statement = select(Category).order_by(Category.id)
        return session.exec(statement).all()

    @staticmethod
    def get_pet_by_id(session: Session, pet_id: int) -> Optional[Pet]:
        """Finds a specific pet by its identifier."""
        return session.get(Pet, pet_id)

    @staticmethod
    def get_current_customer(session: Session) -> Customer:
        """Gets the default customer for the pet store."""
        customer: Optional[Customer] = session.get(Customer, Services.DEFAULT_CUSTOMER_ID)
        if not customer:
            raise Services.Error("Default customer is missing.")
        return customer

    @staticmethod
    def buy_pet(session: Session, pet_id: int) -> Optional[PetOrder]:
        """Processes a pet purchase transaction."""
        pet: Optional[Pet] = session.get(Pet, pet_id)
        if not pet:
            raise Services.Error("Pet not found.")

        if pet.status != Status.AVAILABLE:
            raise Services.Error("Pet is not available.")

        pet.status = Status.SOLD
        session.add(pet)

        current_customer: Customer = Services.get_current_customer(session)
        order: PetOrder = PetOrder(customer_id=current_customer.id, pet_id=pet.id)
        session.add(order)
        session.commit()
        session.refresh(order)
        return order

    @staticmethod
    def save_pet(session: Session, id: Optional[int], name: str, status: Status, category_id: int) -> None:
        """Saves a new pet or updates an existing one."""
        ext_name: str = name if name else "?"
        category: Optional[Category] = session.get(Category, category_id)
        if not category:
            raise Services.Error("Category not found.")

        if id:
            pet: Optional[Pet] = session.get(Pet, id)
            if pet:
                pet.name = ext_name
                pet.status = status
                pet.category_id = category_id
                session.add(pet)
        else:
            new_pet: Pet = Pet(name=ext_name, status=status, category_id=category_id)
            session.add(new_pet)
        
        session.commit()

    @staticmethod
    def delete_pet(session: Session, pet_id: int) -> None:
        """Deletes a pet from the store."""
        pet: Optional[Pet] = session.get(Pet, pet_id)
        if pet:
            session.delete(pet)
            session.commit()

    @staticmethod
    def seed_data(session: Session) -> None:
        """Initializes the database with default data if empty."""
        if session.exec(select(Category)).first():
            return

        dogs: Category = Category(name="Dogs")
        cats: Category = Category(name="Cats")
        birds: Category = Category(name="Birds")
        session.add_all([dogs, cats, birds])
        session.commit()

        session.add(Customer(id=1, name="John Doe"))
        
        session.add_all([
            Pet(name="Rex", status=Status.AVAILABLE, category_id=dogs.id),
            Pet(name="Buddy", status=Status.PENDING, category_id=dogs.id),
            Pet(name="Micka", status=Status.AVAILABLE, category_id=cats.id),
            Pet(name="Tweety", status=Status.SOLD, category_id=birds.id)
        ])
        session.commit()
