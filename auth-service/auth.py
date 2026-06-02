from fastapi import APIRouter, HTTPException
from jwt_utils import crear_token
from models import Usuario
import bcrypt

router = APIRouter()

# Simulación de base de datos en memoria (username -> hashed_password)
db_usuarios = {}

@router.post("/register")
def registrar(usuario: Usuario):
    if usuario.username in db_usuarios:
        raise HTTPException(status_code=400, detail="El usuario ya existe")
    hashed = bcrypt.hashpw(usuario.password.encode(), bcrypt.gensalt())
    db_usuarios[usuario.username] = hashed
    print(f"DEBUG - Usuario registrado: {usuario.username} | Hash guardado (bcrypt): {hashed.decode('utf-8')}")
    return {"mensaje": "Usuario registrado"}

@router.post("/login")
def login(usuario: Usuario):
    hash_guardado = db_usuarios.get(usuario.username)
    if not hash_guardado or not bcrypt.checkpw(usuario.password.encode(), hash_guardado):
        raise HTTPException(status_code=401, detail="Credenciales inválidas")
    token = crear_token({"sub": usuario.username})
    return {"token": token}

