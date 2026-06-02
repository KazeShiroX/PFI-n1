from fastapi import APIRouter, HTTPException
from jwt_utils import crear_token
from models import Usuario
import bcrypt
import psycopg2
import re
import os

router = APIRouter()

def get_db_connection():
    db_url = os.getenv("DB_URL", "")
    db_user = os.getenv("DB_USER", "banco")
    db_pass = os.getenv("DB_PASS", "1234")
    
    # Parse jdbc:postgresql://host:port/db
    match = re.search(r"jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)", db_url)
    if match:
        host = match.group(1)
        port = match.group(2) or "5432"
        dbname = match.group(3)
    else:
        host = "192.168.1.49"
        port = "5432"
        dbname = "bancodb"
        
    return psycopg2.connect(
        host=host,
        port=port,
        database=dbname,
        user=db_user,
        password=db_pass
    )

def init_db():
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS usuarios (
                username VARCHAR(50) PRIMARY KEY,
                password_hash VARCHAR(100) NOT NULL
            );
        """)
        conn.commit()
        cur.close()
        conn.close()
        print("DATABASE - Tabla 'usuarios' inicializada correctamente.")
    except Exception as e:
        print(f"DATABASE ERROR - No se pudo inicializar la tabla 'usuarios': {e}")

# Inicializar tabla al cargar el módulo
init_db()

@router.post("/register")
def registrar(usuario: Usuario):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        
        # Verificar si ya existe el usuario
        cur.execute("SELECT username FROM usuarios WHERE username = %s;", (usuario.username,))
        if cur.fetchone():
            cur.close()
            conn.close()
            raise HTTPException(status_code=400, detail="El usuario ya existe")
            
        # Generar hash y guardar
        hashed = bcrypt.hashpw(usuario.password.encode(), bcrypt.gensalt()).decode('utf-8')
        cur.execute("INSERT INTO usuarios (username, password_hash) VALUES (%s, %s);", (usuario.username, hashed))
        conn.commit()
        cur.close()
        conn.close()
        
        print(f"DEBUG - Usuario registrado en BD: {usuario.username}")
        return {"mensaje": "Usuario registrado"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error en base de datos: {e}")

@router.post("/login")
def login(usuario: Usuario):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        
        # Buscar usuario en BD
        cur.execute("SELECT password_hash FROM usuarios WHERE username = %s;", (usuario.username,))
        row = cur.fetchone()
        cur.close()
        conn.close()
        
        if not row:
            raise HTTPException(status_code=401, detail="Credenciales inválidas")
            
        hash_guardado = row[0]
        if not bcrypt.checkpw(usuario.password.encode(), hash_guardado.encode('utf-8')):
            raise HTTPException(status_code=401, detail="Credenciales inválidas")
            
        token = crear_token({"sub": usuario.username})
        return {"token": token}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error en base de datos: {e}")
