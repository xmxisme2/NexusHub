"""Validate the checked-in NexusHub API contract without third-party packages.

This is intentionally a small structural check that can run in a clean CI
runner.  The generator remains the source of truth; this script catches a
stale generated file, dangling schema references, and violations of the
current two-player API rules.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC_PATH = ROOT / "doc" / "api" / "openapi.json"


def iter_refs(value):
    if isinstance(value, dict):
        if "$ref" in value:
            yield value["$ref"]
        for child in value.values():
            yield from iter_refs(child)
    elif isinstance(value, list):
        for child in value:
            yield from iter_refs(child)


def fail(message: str) -> None:
    print(f"contract validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    try:
        spec = json.loads(SPEC_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"cannot load {SPEC_PATH}: {exc}")

    if spec.get("openapi") != "3.1.0":
        fail("OpenAPI version must be 3.1.0")
    paths = spec.get("paths")
    schemas = spec.get("components", {}).get("schemas")
    if not isinstance(paths, dict) or not paths:
        fail("paths must be a non-empty object")
    if not isinstance(schemas, dict) or not schemas:
        fail("components.schemas must be a non-empty object")

    refs = list(iter_refs(spec))
    valid_refs = {f"#/components/schemas/{name}" for name in schemas}
    dangling = sorted(set(refs) - valid_refs - {
        "#/components/schemas/ApiResponse",
    })
    if dangling:
        fail("dangling schema refs: " + ", ".join(dangling))

    operation_ids: set[str] = set()
    allowed_methods = {"post"}
    for path, path_item in paths.items():
        if not path.startswith("/api/"):
            fail(f"path is outside /api: {path}")
        methods = {method for method in path_item if method in {"get", "post", "put", "patch", "delete"}}
        if path == "/api/auth/csrf":
            if methods != {"get"}:
                fail("CSRF endpoint must be GET only")
        elif methods != allowed_methods:
            fail(f"business endpoint must be POST only: {path}")
        for method in methods:
            operation = path_item[method]
            operation_id = operation.get("operationId")
            if not isinstance(operation_id, str) or not re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", operation_id):
                fail(f"invalid operationId for {method.upper()} {path}")
            if operation_id in operation_ids:
                fail(f"duplicate operationId: {operation_id}")
            operation_ids.add(operation_id)
            response_codes = set(operation.get("responses", {}))
            required_codes = {"200", "400", "401", "403", "404", "409", "422", "429", "500"}
            if not required_codes <= response_codes:
                fail(f"{method.upper()} {path} is missing standard responses")
            response_200 = operation["responses"]["200"]
            if "application/json" not in response_200.get("content", {}):
                fail(f"{method.upper()} {path} has no JSON success response")
            if method == "post" and "requestBody" not in operation:
                fail(f"POST {path} must declare a JSON request body")

    # Rules frozen for CLASSIC_V1: two seats, no configurable disabled types,
    # no four-with-two moves, and no more than five displayed winning lines.
    game_state = schemas.get("GameState", {}).get("properties", {})
    if game_state.get("hands", {}).get("maxItems") != 2:
        fail("GameState.hands must have exactly two seats")
    if "disabledTypes" in game_state:
        fail("disabledTypes is not part of the frozen contract")
    move_types = schemas.get("Move", {}).get("properties", {}).get("type", {}).get("enum", [])
    if any(move_type in move_types for move_type in ("FOUR_TWO_SINGLE", "FOUR_TWO_PAIR")):
        fail("four-with-two move types must be absent")
    winning_lines = schemas.get("SolveResult", {}).get("properties", {}).get("winningLines", {})
    if winning_lines.get("maxItems") != 5:
        fail("SolveResult.winningLines.maxItems must be 5")

    print(f"OpenAPI contract passed: {len(paths)} paths, {len(schemas)} schemas")


if __name__ == "__main__":
    main()
