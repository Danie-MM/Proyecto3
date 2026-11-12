"""Comprueba el contrato de la prueba de entorno usando el proxy del frontend."""

import json
import sys
from datetime import datetime
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, build_opener, HTTPCookieProcessor
from http.cookiejar import CookieJar
from uuid import UUID, uuid4


cliente = build_opener(HTTPCookieProcessor(CookieJar()))
token_csrf = None


def solicitar(url, estado, cuerpo=None):
    datos = None if cuerpo is None else json.dumps(cuerpo).encode()
    cabeceras = {"Content-Type": "application/json"}
    if cuerpo is not None and token_csrf:
        cabeceras["X-CSRF-TOKEN"] = token_csrf
    solicitud = Request(url, data=datos, headers=cabeceras)
    try:
        respuesta = cliente.open(solicitud, timeout=15)
    except HTTPError as error:
        respuesta = error
    with respuesta:
        if respuesta.status != estado:
            raise RuntimeError(f"{solicitud.get_method()} {url}: {respuesta.status}, se esperaba {estado}")
        return json.load(respuesta), respuesta.headers


def verificar(origen):
    global token_csrf
    csrf, _ = solicitar(origen.rstrip("/") + "/api/v1/sesion/csrf", 200)
    token_csrf = csrf["token"]
    contrato = json.loads(Path(__file__).with_name("entorno.openapi.json").read_text(encoding="utf-8"))
    ruta = next(r for r, operaciones in contrato["paths"].items() if "post" in operaciones)
    api = origen.rstrip("/") + ruta
    mensaje = "Comprobación G-02"
    creado, cabeceras = solicitar(api, 201, {"texto": mensaje})
    for campo in contrato["components"]["schemas"]["Registro"]["required"]:
        if campo not in creado:
            raise ValueError(f"Falta el campo {campo}")
    registro_id = UUID(creado["id"])
    fecha = datetime.fromisoformat(creado["creadoEn"].replace("Z", "+00:00"))
    if fecha.utcoffset() is None or creado["texto"] != mensaje:
        raise ValueError("El registro guardado difiere de la solicitud o carece de zona horaria")
    if cabeceras.get("Location") != f"{ruta}/{registro_id}":
        raise ValueError("La cabecera Location debe identificar el registro creado")
    leido, _ = solicitar(f"{api}/{registro_id}", 200)
    if leido != creado:
        raise ValueError("La consulta devolvió un registro distinto")
    for url, estado, cuerpo in [(api, 400, {"texto": " "}), (f"{api}/{uuid4()}", 404, None)]:
        error, _ = solicitar(url, estado, cuerpo)
        if not set(contrato["components"]["schemas"]["Error"]["required"]).issubset(error):
            raise ValueError("La respuesta de error no cumple el contrato")
    print(f"G-02 verificado: alta y consulta del registro {registro_id}, rechazos 400 y 404.")


if __name__ == "__main__":
    verificar(sys.argv[1] if len(sys.argv) > 1 else "http://localhost:4200")
