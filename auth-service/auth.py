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
    
    url = db_url.replace("jdbc:postgresql://", "")
    if "?" in url:
        url = url.split("?")[0]
    
    parts = url.split("/")
    hosts_str = parts[0]
    dbname = parts[1] if len(parts) > 1 else "bancodb"
    
    hosts = []
    if hosts_str:
        for h in hosts_str.split(","):
            if ":" in h:
                shost, sport = h.split(":")
            else:
                shost, sport = h, "5432"
            hosts.append((shost, sport))
    else:
        hosts = [("172.20.0.20", "5432"), ("172.20.0.30", "5432")]

    for host, port in hosts:
        try:
            conn = psycopg2.connect(
                host=host,
                port=port,
                database=dbname,
                user=db_user,
                password=db_pass,
                connect_timeout=3
            )
            cur = conn.cursor()
            cur.execute("SELECT pg_is_in_recovery();")
            is_recovery = cur.fetchone()[0]
            cur.close()
            
            if not is_recovery:
                return conn
            else:
                conn.close()
        except Exception:
            continue

    for host, port in hosts:
        try:
            return psycopg2.connect(
                host=host,
                port=port,
                database=dbname,
                user=db_user,
                password=db_pass,
                connect_timeout=3
            )
        except Exception:
            continue
            
    raise Exception("No se pudo conectar a ninguna base de datos disponible.")

import time

def init_db():
    retries = 10
    while retries > 0:
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
            return
        except Exception as e:
            retries -= 1
            print(f"DATABASE ERROR - No se pudo conectar/inicializar la tabla 'usuarios' (intentos restantes {retries}): {e}")
            if retries > 0:
                time.sleep(3)
            else:
                print("DATABASE ERROR - Falló la inicialización final de la tabla 'usuarios'.")

init_db()

@router.post("/register")
def registrar(usuario: Usuario):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        
        cur.execute("SELECT username FROM usuarios WHERE username = %s;", (usuario.username,))
        if cur.fetchone():
            cur.close()
            conn.close()
            raise HTTPException(status_code=400, detail="El usuario ya existe")
            
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
