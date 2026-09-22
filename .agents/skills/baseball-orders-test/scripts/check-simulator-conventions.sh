#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="${1:-$(cd "${script_dir}/../../../.." && pwd)}"

python3 - "${repo_dir}" <<'PY'
from pathlib import Path
import re
import sys

root = Path(sys.argv[1]).resolve()
simulator = root / "apps/simulator"
if not simulator.is_dir():
    sys.exit(f"Simulator directory not found: {simulator}")

# Check top-level and named member Java declarations. This source check does not
# replace javac or behavioral tests.
def source_text(path):
    text = path.read_text(encoding="utf-8")
    return re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)

def declaration_text(text):
    # Keep positions and newlines while ignoring braces inside literals/comments.
    token = re.compile(r'''/\*.*?\*/|//[^\n]*|""".*?"""|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*' ''', re.S | re.X)
    return token.sub(lambda match: re.sub(r"[^\n]", " ", match.group()), text)

def member_declarations(text):
    cleaned = declaration_text(text)
    pattern = re.compile(
        r"(?m)^[ \t]*(?P<mods>(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static)[ \t]+)*)"
        r"(?P<kind>class|interface|record|enum)[ \t]+(?P<name>\w+)"
        r"(?P<parents>[^{};]*)\{"
    )
    starts = {match.start(): match for match in pattern.finditer(cleaned)}
    accepted = {}
    scopes = []
    result = []
    for position, char in enumerate(cleaned):
        match = starts.get(position)
        if match and all(scope is not None for scope in scopes):
            enclosing = tuple(scopes)
            accepted[match.end() - 1] = (match, enclosing)
            result.append((match, enclosing))
        if char == "{":
            declaration = accepted.get(position)
            scopes.append(declaration[0].group("name") if declaration else None)
        elif char == "}":
            if scopes:
                scopes.pop()
    return result

declarations = {}
simple_names = {}
fq_names = {}
errors = []
for module in ("domain", "application", "infrastructure"):
    main = simulator / module / "src/main/java"
    for path in main.rglob("*.java"):
        raw = path.read_text(encoding="utf-8")
        cleaned = declaration_text(raw)
        package = re.search(r"\bpackage\s+([\w.]+)\s*;", cleaned)
        if not package:
            continue
        package = package.group(1)
        imports = {value.rsplit(".", 1)[-1]: value
                   for value in re.findall(r"\bimport\s+(?!static\b)([\w.]+)\s*;", cleaned)}
        for match, enclosing in member_declarations(raw):
            name = match.group("name")
            type_names = (*enclosing, name)
            identity = (module, f"{package}.{'.'.join(type_names)}")
            if identity in declarations:
                errors.append(f"duplicate type declaration: {identity[1]} in {module}")
                continue
            parents = match.group("parents")
            extends = re.search(r"\bextends\s+([\w.]+)", parents)
            implements = re.search(r"\bimplements\s+([^{}]+)", parents)
            interfaces = [] if not implements else [part.strip().split("<", 1)[0]
                                                    for part in implements.group(1).split(",")]
            declarations[identity] = {
                "module": module, "path": path, "package": package,
                "imports": imports, "name": name, "type_names": type_names, "enclosing": enclosing,
                "kind": match.group("kind"),
                "abstract": "abstract" in match.group("mods").split(),
                "extends": extends.group(1) if extends else None,
                "interfaces": interfaces,
            }
            simple_names.setdefault(name, []).append(identity)
            fq_names.setdefault(identity[1], []).append(identity)

def resolve(reference, owner):
    if not reference:
        return None
    first, *suffix = reference.split(".")
    possible = []
    if first in owner["imports"]:
        possible.append(".".join([owner["imports"][first], *suffix]))
    if reference.startswith(owner["package"] + "."):
        possible.append(reference)
    for length in range(len(owner["type_names"]), -1, -1):
        prefix = ".".join((owner["package"], *owner["type_names"][:length]))
        possible.append(f"{prefix}.{reference}")
    for fq_name in possible:
        candidates = fq_names.get(fq_name, [])
        if candidates:
            break
    else:
        candidates = simple_names.get(reference, []) if "." not in reference else []
    if len(candidates) > 1:
        local = [candidate for candidate in candidates if candidate[0] == owner["module"]]
        candidates = local if len(local) == 1 else candidates
    if len(candidates) > 1:
        errors.append(f"ambiguous parent type {reference} in {owner['path'].relative_to(root)}")
        return None
    return candidates[0] if candidates else None

def is_concrete_subtype(identity, visited=None):
    visited = set() if visited is None else visited
    if identity in visited:
        return False
    visited.add(identity)
    decl = declarations[identity]
    if decl["kind"] == "interface" or decl["abstract"]:
        return False
    for reference in decl["interfaces"]:
        target = resolve(reference, decl)
        if target and declarations[target]["kind"] == "interface":
            return True
    parent = resolve(decl["extends"], decl)
    if parent:
        ancestor = declarations[parent]
        return ancestor["abstract"] or ancestor["kind"] == "interface" or is_concrete_subtype(parent, visited)
    return False

subtypes = 0
for identity, decl in sorted(declarations.items()):
    if not is_concrete_subtype(identity):
        continue
    subtypes += 1
    name = "".join(decl["type_names"])
    relative = Path(*decl["package"].split(".")) / f"{name}Test.java"
    test_file = simulator / decl["module"] / "src/test/java" / relative
    if not test_file.is_file():
        errors.append(f"{identity[1]}: dedicated test missing: {test_file.relative_to(root)}")
        continue
    test_text = source_text(test_file)
    if not re.search(rf"\bclass\s+{re.escape(name)}Test\b", test_text):
        errors.append(f"{identity[1]}: dedicated test class declaration missing: {test_file.relative_to(root)}")
    if not re.search(r"@(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b", test_text):
        errors.append(f"{identity[1]}: no JUnit test method in {test_file.relative_to(root)}")

keys = {
    "simulation.game-count": (1, None),
    "simulation.sqs.max-messages-per-poll": (1, 10),
    "simulation.sqs.long-poll-seconds": (0, 20),
}
resources = simulator / "infrastructure/src/main/resources"

def yaml_scalars(path):
    if not path.is_file():
        errors.append(f"profile file missing: {path.relative_to(root)}")
        return {}
    result = {}
    stack = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        match = re.match(r"^(\s*)([\w-]+):(?:\s*(.*))?$", line)
        if not match:
            continue
        indent, key, value = match.groups()
        while stack and stack[-1][0] >= len(indent):
            stack.pop()
        full_key = ".".join([part for _, part in stack] + [key])
        if value:
            result[full_key] = value.split(" #", 1)[0].strip().strip("'\"")
        else:
            stack.append((len(indent), key))
    return result

shared = yaml_scalars(resources / "application.yml")
for key in keys:
    if key not in shared:
        errors.append(f"application.yml: missing {key} fallback")
for profile in ("local", "dev", "prod"):
    path = resources / f"application-{profile}.yml"
    values = yaml_scalars(path)
    for key, (minimum, maximum) in keys.items():
        value = values.get(key)
        if value is None:
            errors.append(f"{path.name}: missing {key}")
            continue
        if not re.fullmatch(r"\d+", value):
            errors.append(f"{path.name}: {key} must be an explicit integer")
            continue
        number = int(value)
        if number < minimum or (maximum is not None and number > maximum):
            errors.append(f"{path.name}: {key}={number} outside {minimum}..{maximum or 'unbounded'}")

bindings = {
    "simulation.game-count": simulator / "application/src/main/java/com/example/baseballorders/simulator/application/usecase/SimulateGameUseCase.java",
    "simulation.sqs.max-messages-per-poll": simulator / "infrastructure/src/main/java/com/example/baseballorders/simulator/infrastructure/messaging/SqsSimulationScheduler.java",
    "simulation.sqs.long-poll-seconds": simulator / "infrastructure/src/main/java/com/example/baseballorders/simulator/infrastructure/messaging/SqsSimulationScheduler.java",
}
for key, path in bindings.items():
    if not path.is_file() or "${" + key + "}" not in path.read_text(encoding="utf-8"):
        errors.append(f"{path.relative_to(root)}: missing binding for {key}")

scheduler = bindings["simulation.sqs.max-messages-per-poll"]
if scheduler.is_file():
    text = source_text(scheduler)
    for constant in ("MAX_MESSAGES_PER_POLL", "LONG_POLL_SECONDS"):
        if re.search(rf"\b{constant}\s*=\s*\d+\s*;", text):
            errors.append(f"{scheduler.relative_to(root)}: {constant} remains a numeric constant")

if errors:
    for error in errors:
        print(f"FAIL: {error}", file=sys.stderr)
    sys.exit(1)
print(f"PASS: {subtypes} concrete interface/abstract subtypes have dedicated tests")
print("PASS: simulator operational numeric settings are bound and explicit in local/dev/prod")
PY
