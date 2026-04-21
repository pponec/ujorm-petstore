from typing import Optional, List
from sqlmodel import SQLModel, Field, Relationship, create_engine
from constants import Status

class Category(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    name: str = Field(index=True)
    
    pets: List["Pet"] = Relationship(back_populates="category")

class Customer(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    name: str

class Pet(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    name: str
    status: Status = Field(default=Status.AVAILABLE)
    category_id: Optional[int] = Field(default=None, foreign_key="category.id")
    
    category: Optional[Category] = Relationship(back_populates="pets")

class PetOrder(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    customer_id: int = Field(foreign_key="customer.id")
    pet_id: int = Field(foreign_key="pet.id")
