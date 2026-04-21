from fastapi import FastAPI, Depends, Request, Form, Query
from fastapi.responses import HTMLResponse, RedirectResponse
from sqlmodel import Session
from typing import Optional, List, Annotated
import uvicorn

from database import engine, get_session, create_db_and_tables
from models import Pet, Category
from services import Services
from constants import Status
from view import PetView

app: FastAPI = FastAPI(title="Ujorm PetStore Python Port")

@app.on_event("startup")
def on_startup() -> None:
    create_db_and_tables()
    with Session(engine) as session:
        Services.seed_data(session)

@app.get("/", response_class=HTMLResponse)
async def index(
    session: Annotated[Session, Depends(get_session)],
    action: Optional[str] = None,
    pet_id: Optional[int] = None
) -> HTMLResponse:
    pets: List[Pet] = Services.get_pets(session)
    categories: List[Category] = Services.get_categories(session)
    
    pet_to_edit: Optional[Pet] = None
    if action == "edit" and pet_id:
        pet_to_edit = Services.get_pet_by_id(session, pet_id)

    html_content: str = PetView.render_page(pets, categories, pet_to_edit)
    return HTMLResponse(content=html_content)

@app.post("/pets")
async def save_pet(
    session: Annotated[Session, Depends(get_session)],
    id: Annotated[Optional[int], Form()] = None,
    name: Annotated[str, Form()] = None,
    status: Annotated[Status, Form()] = None,
    category_id: Annotated[int, Form()] = None
) -> RedirectResponse:
    try:
        Services.save_pet(session, id, name, status, category_id)
    except Services.Error:
        pass
    return RedirectResponse(url="/", status_code=303)

@app.post("/pets/{pet_id}/buy")
async def buy_pet(
    pet_id: int,
    session: Annotated[Session, Depends(get_session)]
) -> RedirectResponse:
    try:
        Services.buy_pet(session, pet_id)
    except Services.Error:
        pass
    return RedirectResponse(url="/", status_code=303)

@app.post("/pets/{pet_id}/delete")
async def delete_pet(
    pet_id: int,
    session: Annotated[Session, Depends(get_session)]
) -> RedirectResponse:
    Services.delete_pet(session, pet_id)
    return RedirectResponse(url="/", status_code=303)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=3000)
