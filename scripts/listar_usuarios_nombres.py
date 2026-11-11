"""Lista nombre_completo de sigra.usuario (solo lectura). Lee SIGRA/.env."""
from __future__ import annotations

import re
import sys
from pathlib import Path

import psycopg

ENV_PATH = Path(__file__).resolve().parents[1] / "SIGRA" / ".env"


def load_env(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        data[key.strip()] = value.strip()
    return data


def main() -> int:
    env = load_env(ENV_PATH)
    jdbc = env["DB_URL"]
    m = re.match(r"^jdbc:postgresql://([^:/?]+):?(\d+)?/([^?]+)", jdbc)
    if not m:
        print("DB_URL inválida", file=sys.stderr)
        return 1
    host, port, database = m.group(1), m.group(2) or "5432", m.group(3)
    conninfo = (
        f"host={host} port={port} dbname={database} "
        f"user={env['DB_USERNAME']} password={env['DB_PASSWORD']} sslmode=require"
    )
    sql = """
        SELECT correo_institucional,
               nombre_completo,
               cardinality(regexp_split_to_array(btrim(nombre_completo), '\\s+')) AS palabras,
               regexp_split_to_array(btrim(nombre_completo), '\\s+') AS tokens
        FROM sigra.usuario
        ORDER BY palabras, nombre_completo
    """
    resumen = """
        SELECT cardinality(regexp_split_to_array(btrim(nombre_completo), '\\s+')) AS palabras,
               count(*) AS usuarios
        FROM sigra.usuario
        GROUP BY 1
        ORDER BY 1
    """
    with psycopg.connect(conninfo) as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()
            print("correo | nombre_completo | palabras | tokens")
            print("-" * 100)
            for correo, nombre, palabras, tokens in rows:
                print(f"{correo} | {nombre} | {palabras} | {tokens}")
            print()
            cur.execute(resumen)
            print("RESUMEN por cantidad de palabras:")
            for palabras, usuarios in cur.fetchall():
                print(f"  {palabras} palabra(s): {usuarios} usuario(s)")
            print(f"\nTOTAL: {len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
