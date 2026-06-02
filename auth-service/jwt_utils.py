import jwt
import os
from datetime import datetime, timedelta

SECRET = os.getenv("JWT_SECRET")
if not SECRET:
    for env_path in [".env", "../.env"]:
        if os.path.exists(env_path):
            try:
                with open(env_path, "r", encoding="utf-8") as f:
                    for line in f:
                        if "=" in line and not line.strip().startswith("#"):
                            k, v = line.strip().split("=", 1)
                            if k.strip() == "JWT_SECRET":
                                SECRET = v.strip()
                                break
            except Exception:
                pass
            if SECRET:
                break

if not SECRET:
    SECRET = "super-secret-key-for-development"


def crear_token(datos: dict) -> str:
    payload = datos.copy()
    payload["exp"] = datetime.utcnow() + timedelta(hours=8)
    return jwt.encode(payload, SECRET, algorithm="HS256")

def validar_token(token: str) -> dict:
    try:
        return jwt.decode(token, SECRET, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        raise Exception("Token expirado")
    except jwt.InvalidTokenError:
        raise Exception("Token inválido")
