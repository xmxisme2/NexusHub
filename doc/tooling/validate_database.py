"""Static validation for the MySQL schema and local seed artifacts.

The checker does not need a MySQL client, so it is suitable for pre-commit and
the contract CI job.  A CI job can additionally execute the files against a
throwaway MySQL service (see ci.yml) for parser and foreign-key validation.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "doc" / "database" / "schema.sql"
SEED = ROOT / "doc" / "database" / "seed-test.sql"
EXPECTED_TABLES = {
    "sys_user", "hub_system", "puzzle", "puzzle_version", "game_session",
    "game_action", "game_bot_job", "solve_task", "solve_result",
    "strategy_node", "strategy_edge", "audit_log", "idempotency_record",
}
AUDIT_COLUMNS = {"id", "create_time", "update_time", "create_by", "update_by", "deleted"}


def fail(message: str) -> None:
    print(f"database validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def create_blocks(sql: str):
    pattern = re.compile(r"CREATE\s+TABLE\s+([a-z_][a-z0-9_]*)\s*\(", re.I)
    for match in pattern.finditer(sql):
        depth = 1
        index = match.end()
        quote = None
        while index < len(sql) and depth:
            char = sql[index]
            if quote:
                if char == quote and sql[index - 1] != "\\":
                    quote = None
            elif char in ("'", '"', "`"):
                quote = char
            elif char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
            index += 1
        if depth:
            fail(f"unclosed CREATE TABLE {match.group(1)}")
        yield match.group(1), sql[match.end():index - 1]


def main() -> None:
    schema = SCHEMA.read_text(encoding="utf-8")
    seed = SEED.read_text(encoding="utf-8")
    if re.search(r"\bDROP\s+(?:DATABASE|TABLE)\b", schema, re.I):
        fail("schema must not drop database or tables")
    if "SET NAMES utf8mb4" not in schema or "SET time_zone = '+00:00'" not in schema:
        fail("schema must set utf8mb4 and UTC")
    blocks = dict(create_blocks(schema))
    if set(blocks) != EXPECTED_TABLES:
        fail(f"table set mismatch: expected {sorted(EXPECTED_TABLES)}, got {sorted(blocks)}")
    for table, body in blocks.items():
        columns = set()
        for line in body.splitlines():
            line = line.strip()
            column = re.match(r"([a-z_][a-z0-9_]*)\s+", line, re.I)
            if column and column.group(1).upper() not in {"PRIMARY", "CONSTRAINT", "UNIQUE", "KEY", "FOREIGN", "CHECK"}:
                columns.add(column.group(1).lower())
        missing = AUDIT_COLUMNS - columns
        if missing:
            fail(f"{table} missing audit columns: {sorted(missing)}")
        if not re.search(r"ENGINE\s*=\s*InnoDB", schema, re.I):
            fail(f"{table} must use InnoDB")
        if "deleted IN (0,1)" not in body:
            fail(f"{table} must constrain deleted to 0/1")
    foreign_tables = re.findall(r"REFERENCES\s+([a-z_][a-z0-9_]*)\s*\(", schema, re.I)
    unknown = sorted(set(foreign_tables) - EXPECTED_TABLES)
    if unknown:
        fail("foreign keys reference unknown tables: " + ", ".join(unknown))
    if re.search(r"\bSELECT\s+\*", schema, re.I):
        fail("schema contains SELECT *")
    if re.search(r"\bDROP\s+(?:DATABASE|TABLE)\b", seed, re.I):
        fail("seed must not drop database or tables")
    for marker in ("SET NAMES utf8mb4", "CREATE DATABASE IF NOT EXISTS nexushub", "USE nexushub"):
        if marker not in seed:
            fail(f"seed missing {marker}")
    if not re.search(r"INSERT\s+INTO\s+sys_user", seed, re.I):
        fail("seed must initialize a test user")
    if not re.search(r"INSERT\s+INTO\s+puzzle_version", seed, re.I):
        fail("seed must contain a puzzle version")
    print(f"Database artifacts passed: {len(blocks)} tables and seed checks")


if __name__ == "__main__":
    main()
