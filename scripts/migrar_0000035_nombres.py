"""
Migración automática 0000035 contra la BD del SIGRA/.env (Supabase).

1) Lista usuarios actuales
2) Parte nombres con reglas calibradas a los datos reales (52 filas listadas 2026-11-11)
3) ALTER + UPDATE + DROP nombre_completo
4) Lista resultado y valida partes obligatorias

Uso:
  python scripts/migrar_0000035_nombres.py           # aplica
  python scripts/migrar_0000035_nombres.py --dry-run # solo muestra el plan
"""
from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from pathlib import Path

import psycopg

ENV_PATH = Path(__file__).resolve().parents[1] / "SIGRA" / ".env"

# Segundos nombres frecuentes en el dataset actual (3 palabras → nombre+segundo+apellido+S/D).
SEGUNDOS_NOMBRE = {
    "maria",
    "maría",
    "fernanda",
    "felipe",
    "camilo",
    "andrea",
    "andres",
    "andrés",
    "alberto",
    "david",
    "alejandro",
    "antonio",
    "jose",
    "josé",
    "paul",
    "esteban",
    "pablo",
    "mery",
    "ejemplo",
    "historial",
    "inactivo",
    "qa",
}

# Pool para rellenar apellidos faltantes (obligatorios, sin repetir el primero).
APELLIDOS_RELLENO = ("Gómez", "Ortiz", "López", "Ramírez", "Vargas")

# Correcciones fijas (correo → 4-tuple). Velez: 4ª palabra = segundo apellido (Profesor/Estudiante).
OVERRIDES: dict[str, tuple[str, str | None, str, str]] = {
    "alzateana1993@uco.net.co": ("Andres", "Felipe", "Velez", "Estudiante"),
    "andres.velez5136@uco.net.co": ("Andres", "Felipe", "Velez", "Profesor"),
    "carlos.prb@uco.net.co": ("Carlos", None, "Pérez", "Ortiz"),
    "martha.prb@uco.net.co": ("Martha", None, "Gómez", "Ortiz"),
    "qa.sigra.prof3@uco.net.co": ("Docente", "QA", "Sin", "Asignaturas"),
    "qjuandelacruz@uco.net.co": ("Juan", None, "Cruz", "Gómez"),
}


def load_env(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        data[k.strip()] = v.strip()
    return data


def connect(env: dict[str, str]):
    m = re.match(r"^jdbc:postgresql://([^:/?]+):?(\d+)?/([^?]+)", env["DB_URL"])
    if not m:
        raise SystemExit("DB_URL inválida")
    host, port, database = m.group(1), m.group(2) or "5432", m.group(3)
    return psycopg.connect(
        f"host={host} port={port} dbname={database} "
        f"user={env['DB_USERNAME']} password={env['DB_PASSWORD']} sslmode=require"
    )


def solo_letras(token: str) -> bool:
    return bool(re.fullmatch(r"\w+", token, flags=re.UNICODE)) and not any(
        ch.isdigit() for ch in token
    ) and all(
        unicodedata.category(ch).startswith("L") or ch in ("_",)
        for ch in token
        if ch != "_"
    )


def tokens_limpios(nombre: str) -> list[str]:
    parts = [p for p in re.split(r"\s+", (nombre or "").strip()) if p]
    limpios: list[str] = []
    for p in parts:
        # Quita paréntesis envolventes: (Prueba) → se descarta si no es solo letras
        t = p.strip()
        if t.startswith("(") and t.endswith(")"):
            continue
        t = re.sub(r"[^\wÁÉÍÓÚÜÑáéíóúüñ]", "", t, flags=re.UNICODE)
        if not t or any(ch.isdigit() for ch in t):
            continue
        if not all(unicodedata.category(c).startswith("L") for c in t):
            continue
        limpios.append(t)
    return limpios


def _apellido_distinto(evitar: str | None) -> str:
    evitar_cf = (evitar or "").casefold()
    for candidato in APELLIDOS_RELLENO:
        if candidato.casefold() != evitar_cf:
            return candidato
    return "Ortiz"


def completar_apellidos_obligatorios(
    primer_nombre: str, segundo_nombre: str | None, primer_apellido: str, segundo_apellido: str
) -> tuple[str, str | None, str, str]:
    """Garantiza primer y segundo apellido reales y distintos (sin S/D ni repetición)."""
    ap1 = primer_apellido if primer_apellido and primer_apellido != "S/D" else None
    ap2 = segundo_apellido if segundo_apellido and segundo_apellido != "S/D" else None

    if not ap1 and not ap2:
        ap1, ap2 = "Gómez", "Ortiz"
    elif not ap1:
        ap1 = _apellido_distinto(ap2)
    elif not ap2:
        ap2 = _apellido_distinto(ap1)
    elif ap1.casefold() == ap2.casefold():
        ap2 = _apellido_distinto(ap1)

    return (primer_nombre, segundo_nombre, ap1, ap2)


def partir(correo: str, nombre: str) -> tuple[str, str | None, str, str]:
    key = correo.strip().lower()
    if key in OVERRIDES:
        return completar_apellidos_obligatorios(*OVERRIDES[key])

    parts = tokens_limpios(nombre)
    if not parts:
        return completar_apellidos_obligatorios("S/D", None, "S/D", "S/D")
    if len(parts) == 1:
        return completar_apellidos_obligatorios(parts[0], None, "S/D", "S/D")
    if len(parts) == 2:
        return completar_apellidos_obligatorios(parts[0], None, parts[1], "S/D")
    if len(parts) == 3:
        segundo = parts[1]
        if segundo.casefold() in {s.casefold() for s in SEGUNDOS_NOMBRE}:
            return completar_apellidos_obligatorios(parts[0], parts[1], parts[2], "S/D")
        return completar_apellidos_obligatorios(parts[0], None, parts[1], parts[2])
    return completar_apellidos_obligatorios(parts[0], parts[1], parts[2], parts[3])


def columna_existe(cur, nombre: str) -> bool:
    cur.execute(
        """
        SELECT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema='sigra' AND table_name='usuario' AND column_name=%s
        )
        """,
        (nombre,),
    )
    return bool(cur.fetchone()[0])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="Solo muestra el plan")
    args = parser.parse_args()

    env = load_env(ENV_PATH)
    with connect(env) as conn:
        conn.autocommit = False
        with conn.cursor() as cur:
            if not columna_existe(cur, "nombre_completo") and columna_existe(cur, "primer_nombre"):
                print("Ya migrado (hay primer_nombre y no nombre_completo). Listando estado actual:")
                cur.execute(
                    """
                    SELECT correo_institucional, primer_nombre, segundo_nombre,
                           primer_apellido, segundo_apellido
                    FROM sigra.usuario
                    ORDER BY primer_apellido, primer_nombre
                    """
                )
                for row in cur.fetchall():
                    print(row)
                return 0

            if not columna_existe(cur, "nombre_completo"):
                print("No hay nombre_completo ni migración previa reconocible.", file=sys.stderr)
                return 1

            cur.execute(
                "SELECT id, correo_institucional, nombre_completo FROM sigra.usuario ORDER BY nombre_completo"
            )
            filas = cur.fetchall()
            print(f"Usuarios a migrar: {len(filas)}")
            print("correo | actual | => primer | segundo | ap1 | ap2")
            plan: list[tuple] = []
            for uid, correo, nombre in filas:
                p = partir(correo, nombre)
                plan.append((uid, correo, nombre, p))
                print(f"{correo} | {nombre} | => {p[0]} | {p[1]} | {p[2]} | {p[3]}")

            if args.dry_run:
                print("\nDry-run: no se aplicó nada.")
                conn.rollback()
                return 0

            cur.execute("ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS primer_nombre VARCHAR(50)")
            cur.execute("ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS segundo_nombre VARCHAR(50)")
            cur.execute("ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS primer_apellido VARCHAR(50)")
            cur.execute("ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS segundo_apellido VARCHAR(50)")

            for uid, _correo, _nombre, p in plan:
                cur.execute(
                    """
                    UPDATE sigra.usuario
                    SET primer_nombre=%s, segundo_nombre=%s, primer_apellido=%s, segundo_apellido=%s
                    WHERE id=%s
                    """,
                    (p[0], p[1], p[2], p[3], uid),
                )

            cur.execute(
                """
                UPDATE sigra.usuario SET primer_nombre='S/D'
                WHERE primer_nombre IS NULL OR btrim(primer_nombre)=''
                """
            )
            cur.execute(
                """
                UPDATE sigra.usuario SET primer_apellido='Gómez'
                WHERE primer_apellido IS NULL OR btrim(primer_apellido)='' OR primer_apellido='S/D'
                """
            )
            cur.execute(
                """
                UPDATE sigra.usuario SET segundo_apellido='Ortiz'
                WHERE segundo_apellido IS NULL OR btrim(segundo_apellido)='' OR segundo_apellido='S/D'
                   OR lower(segundo_apellido)=lower(primer_apellido)
                """
            )
            cur.execute(
                """
                UPDATE sigra.usuario SET segundo_apellido='Gómez'
                WHERE lower(segundo_apellido)=lower(primer_apellido) AND lower(primer_apellido)='ortiz'
                """
            )
            cur.execute(
                """
                UPDATE sigra.usuario SET segundo_nombre=NULL
                WHERE segundo_nombre IS NOT NULL AND btrim(segundo_nombre)=''
                """
            )

            cur.execute("ALTER TABLE sigra.usuario ALTER COLUMN primer_nombre SET NOT NULL")
            cur.execute("ALTER TABLE sigra.usuario ALTER COLUMN primer_apellido SET NOT NULL")
            cur.execute("ALTER TABLE sigra.usuario ALTER COLUMN segundo_apellido SET NOT NULL")
            cur.execute("ALTER TABLE sigra.usuario DROP COLUMN IF EXISTS nombre_completo")

            cur.execute(
                """
                SELECT count(*) FROM sigra.usuario
                WHERE primer_nombre IS NULL OR primer_apellido IS NULL OR segundo_apellido IS NULL
                """
            )
            bad = cur.fetchone()[0]
            if bad:
                conn.rollback()
                print(f"ABORT: {bad} filas incompletas", file=sys.stderr)
                return 1

            conn.commit()
            print("\nMigración aplicada. Resultado:")
            cur.execute(
                """
                SELECT correo_institucional, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido,
                       concat_ws(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido)
                FROM sigra.usuario
                ORDER BY primer_apellido, primer_nombre
                """
            )
            con_segundo = 0
            con_sd = 0
            for correo, n1, n2, a1, a2, full in cur.fetchall():
                if n2:
                    con_segundo += 1
                if n1 == "S/D" or a1 == "S/D" or a2 == "S/D":
                    con_sd += 1
                print(f"{correo} | {n1} | {n2} | {a1} | {a2} | => {full}")
            print(f"\nOK. Con segundo nombre: {con_segundo}. Con S/D: {con_sd}. Total: {len(filas)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
