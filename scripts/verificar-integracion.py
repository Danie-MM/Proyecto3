"""Comprueba el proxy Angular -> Spring -> PostgreSQL con un registro ficticio.

No requiere paquetes Python adicionales. En CI se ejecuta sobre una base vacia.
Si se ejecuta manualmente, deja un registro de prueba en la base local.
"""

import argparse
import datetime
import json
import urllib.error
import urllib.request
import uuid


def solicitar(base_url, metodo, ruta, estado_esperado, cuerpo=None):
    contenido = None if cuerpo is None else json.dumps(cuerpo).encode("utf-8")
    peticion = urllib.request.Request(
        base_url.rstrip("/") + ruta,
        data=contenido,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method=metodo,
    )
    try:
        respuesta = urllib.request.urlopen(peticion, timeout=15)
    except urllib.error.HTTPError as error:
        respuesta = error
    with respuesta:
        estado = respuesta.status
        dato = json.load(respuesta)
    if estado != estado_esperado:
        raise AssertionError(f"{metodo} {ruta}: se esperaba {estado_esperado}, se recibio {estado}")
    return dato


def verificar(base_url):
    ruta = "/api/v1/pruebas-entorno"
    texto = "Prueba automatica del equipo 🐾"
    creado = solicitar(base_url, "POST", ruta, 201, {"texto": f"  {texto}  "})
    if creado.get("texto") != texto:
        raise AssertionError("El backend no conservo el texto Unicode normalizado")
    identificador = creado["id"]
    if str(uuid.UUID(identificador)) != identificador:
        raise AssertionError("El identificador recibido no es un UUID canonico")
    fecha = creado["creadoEn"]
    if not fecha.endswith("Z"):
        raise AssertionError("La fecha debe estar expresada en UTC")
    datetime.datetime.fromisoformat(fecha.replace("Z", "+00:00"))
    consultado = solicitar(base_url, "GET", f"{ruta}/{identificador}", 200)
    if consultado != creado:
        raise AssertionError("La consulta no recupero el mismo registro")

    invalido = solicitar(base_url, "POST", ruta, 400, {"texto": "   "})
    inexistente = solicitar(base_url, "GET", f"{ruta}/00000000-0000-0000-0000-000000000000", 404)
    for error in (invalido, inexistente):
        if not all(campo in error for campo in ("codigo", "mensaje", "erroresCampos", "trazaId")):
            raise AssertionError("La respuesta de error no respeta el contrato comun")
    if inexistente["codigo"] != "NO_ENCONTRADO":
        raise AssertionError("El error de registro inexistente no coincide con el contrato")
    print(json.dumps({"resultado": "correcto", "comprobaciones": [201, 200, 400, 404], "registro": creado}, ensure_ascii=True))


if __name__ == "__main__":
    argumentos = argparse.ArgumentParser(description=__doc__)
    argumentos.add_argument("--base-url", default="http://127.0.0.1:4200")
    verificar(argumentos.parse_args().base_url)
