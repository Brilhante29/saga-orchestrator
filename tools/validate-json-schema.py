import json
import sys
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: validate-json-schema.py SCHEMA INSTANCE")
        return 2
    schema = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    instance = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
    errors = sorted(
        Draft202012Validator(schema, format_checker=FormatChecker()).iter_errors(instance),
        key=lambda error: list(error.absolute_path),
    )
    for error in errors:
        location = ".".join(map(str, error.absolute_path)) or "$"
        print(f"{location}: {error.message}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
