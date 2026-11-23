#!/usr/bin/env python3
"""Check every Java file under api/src against the FloodWatch team style rules.

Usage: python3 scripts/check-style.py [paths...]
Exits with status 1 and lists each violation as file:line: message.
"""

import pathlib
import re
import sys

AUTHORS = "Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan"
CONTROL = re.compile(r"^\s*(\}\s*)?(if|else|for|while|try|catch|finally|do|switch|synchronized)\b")
TYPE_DECL = re.compile(r"^(\s*)(public |protected |private |static |final |abstract |sealed |non-sealed )*"
                       r"(class|interface|enum|record|@interface)\s+(\w+)")
METHOD_DECL = re.compile(r"^(\s+)((public|protected|private|static|final|abstract|synchronized|default|native)\s+)*"
                         r"(<[^>]+>\s+)?([\w.<>\[\],? ]+?)\s+(\w+)\s*(\(.*)?$")
CTOR_DECL = re.compile(r"^(\s+)((public|protected|private)\s+)?(\w+)\s*(\(.*)?$")
NOT_METHOD_START = ("return ", "new ", "throw ", "else", "case ", "yield ", "assert ")


class Checker:

    def __init__(self, path):
        self.path = path
        self.text = path.read_text(encoding="utf-8")
        self.lines = self.text.split("\n")
        self.errors = []
        self.type_names = set()

    def err(self, idx, msg):
        self.errors.append(f"{self.path}:{idx + 1}: {msg}")

    def blank(self, idx):
        return 0 <= idx < len(self.lines) and self.lines[idx].strip() == ""

    def blanks_before(self, idx):
        n, i = 0, idx - 1
        while i >= 0 and self.lines[i].strip() == "":
            n += 1
            i -= 1
        return n

    def blanks_after(self, idx):
        n, i = 0, idx + 1
        while i < len(self.lines) and self.lines[i].strip() == "":
            n += 1
            i += 1
        return n

    def run(self):
        self.check_header()
        self.check_whitespace_and_comments()
        self.check_end_of_file()
        self.check_control_braces()
        self.check_declarations()
        self.check_javadoc_spacing()
        return self.errors

    def check_header(self):
        name = self.path.name
        expected = ["/*", f" * File:    {name}", f" * Authors: {AUTHORS}"]
        for i, exp in enumerate(expected):
            if i >= len(self.lines) or self.lines[i] != exp:
                self.err(i, f"header line must be {exp!r}")
                return
        if len(self.lines) < 5 or not self.lines[3].startswith(" * Purpose: ") or self.lines[4] != " */":
            self.err(3, "header must have ' * Purpose: ...' then ' */'")
            return
        if self.blanks_after(4) != 2:
            self.err(4, "need exactly 2 blank lines after the file header")

    def check_whitespace_and_comments(self):
        in_block = False
        for i, line in enumerate(self.lines):
            if line != line.rstrip():
                self.err(i, "trailing whitespace")
            if "\t" in line:
                self.err(i, "tab character (use 4 spaces)")
            stripped = line.strip()
            if stripped.startswith("/*"):
                in_block = not stripped.endswith("*/")
                if not (stripped.startswith("/**") or i == 0):
                    self.err(i, "only the file header and Javadoc comments are allowed")
                continue
            if in_block:
                if stripped.endswith("*/"):
                    in_block = False
                continue
            code = re.sub(r'"(\\.|[^"\\])*"', '""', line)
            code = re.sub(r"'(\\.|[^'\\])*'", "''", code)
            if "//" in code:
                self.err(i, "inline // comment is not allowed")
            if "/*" in code:
                self.err(i, "block comment inside code is not allowed")
            indent = len(line) - len(line.lstrip(" "))
            if stripped and indent % 4 != 0 and not stripped.startswith(("+", "-", "*", ".", "?", ":", "&&", "||")):
                self.err(i, "indentation must be a multiple of 4 spaces")

    def check_end_of_file(self):
        if not self.text.endswith("}"):
            self.err(len(self.lines) - 1, "file must end right after its closing '}' (no trailing newline or blank lines)")

    def check_control_braces(self):
        for i, line in enumerate(self.lines):
            stripped = line.strip()
            if stripped.startswith("*") or stripped.startswith("/*"):
                continue
            if re.match(r"^\}\s*(else|catch|finally)\b", stripped):
                self.err(i, "'else'/'catch'/'finally' must start on its own line after '}'")
            m = CONTROL.match(line)
            if m and stripped.endswith("{") and not stripped.startswith("} "):
                self.err(i, f"Allman style: put '{{' of '{m.group(2)}' on its own line")
            if m and stripped != "{" and stripped.endswith("{") and stripped.startswith("} "):
                self.err(i, "Allman style: put '{' on its own line")

    def javadoc_above(self, idx):
        """Return (start, end) line indexes of the Javadoc above a declaration, skipping annotations."""
        i = idx - 1
        while i >= 0 and self.lines[i].strip().startswith("@"):
            i -= 1
        blanks = 0
        while i >= 0 and self.lines[i].strip() == "":
            blanks += 1
            i -= 1
        if i < 0 or self.lines[i].strip() != "*/":
            return None, None, blanks
        end = i
        while i >= 0 and not self.lines[i].strip().startswith("/**"):
            i -= 1
        return i, end, blanks

    def declaration_params(self, idx):
        """Read a declaration whose '(' is on the next line; return (params, closing_line_index)."""
        if idx + 1 >= len(self.lines) or self.lines[idx + 1].strip() != "(":
            return None, idx
        params, j = [], idx + 2
        while j < len(self.lines) and not self.lines[j].strip().startswith(")"):
            params.append(self.lines[j].strip())
            j += 1
        return params, j

    def check_param_block(self, idx, params, close_idx, kind):
        base = len(self.lines[idx]) - len(self.lines[idx].lstrip())
        if len(self.lines[idx + 1]) - len(self.lines[idx + 1].lstrip()) != base:
            self.err(idx + 1, "'(' must line up with the declaration")
        if len(self.lines[close_idx]) - len(self.lines[close_idx].lstrip()) != base:
            self.err(close_idx, "')' must line up with the declaration")
        closing = self.lines[close_idx].strip()
        if kind != "abstract" and not re.match(r"^\)( throws [\w., ]+)? \{$", closing):
            self.err(close_idx, "parameter block must close with ') {'")
        names, prev_type = [], None
        for k, p in enumerate(params):
            line_idx = idx + 2 + k
            if len(self.lines[line_idx]) - len(self.lines[line_idx].lstrip()) != base + 4:
                self.err(line_idx, "parameters must be indented one level")
            parts = [x.strip() for x in self.split_params(p.rstrip(",")) if x.strip()]
            types = []
            for part in parts:
                part = re.sub(r"@\w+(\([^)]*\))?\s*", "", part).replace("final ", "")
                mm = re.match(r"^(.+?)\s+(\w+)$", part)
                if not mm:
                    self.err(line_idx, f"cannot read parameter {part!r}")
                    continue
                types.append(mm.group(1).replace(" ", ""))
                names.append(mm.group(2))
            if len(set(types)) > 1:
                self.err(line_idx, "parameters of different types must be on separate lines")
            if types and prev_type == types[0]:
                self.err(line_idx, "parameters of the same type must stay on one line")
            if types:
                prev_type = types[-1]
            if k < len(params) - 1 and not p.endswith(","):
                self.err(line_idx, "missing ',' between parameter lines")
        return names

    def check_javadoc_tags(self, idx, start, end, names, returns_value, is_type):
        doc = "\n".join(self.lines[start:end + 1])
        documented = re.findall(r"@param\s+(<?\w+>?)", doc)
        for n in names:
            if n not in documented:
                self.err(start, f"Javadoc is missing @param {n}")
        for d in documented:
            if d not in names and not d.startswith("<"):
                self.err(start, f"Javadoc has @param {d} but there is no such parameter")
        if returns_value and "@return" not in doc:
            self.err(start, "Javadoc is missing @return")
        if is_type and "@author" not in doc:
            self.err(start, "class Javadoc is missing @author")

    def check_declarations(self):
        depth = 0
        type_stack = []
        for i, line in enumerate(self.lines):
            stripped = line.strip()
            if not stripped or stripped.startswith(("*", "/*", "//", "@", "import ", "package ")):
                depth += self.brace_delta(line)
                continue
            tm = TYPE_DECL.match(line)
            if tm and not stripped.startswith(NOT_METHOD_START):
                name = tm.group(4)
                self.type_names.add(name)
                if not re.match(r"^[A-Z][A-Za-z0-9]*$", name):
                    self.err(i, f"type name {name!r} must be PascalCase")
                start, end, blanks = self.javadoc_above(i)
                if start is None:
                    self.err(i, f"{tm.group(3)} {name} needs a Javadoc comment")
                else:
                    if blanks != 2:
                        self.err(end, "need exactly 2 blank lines after a Javadoc block")
                    if self.blanks_before(start) != 2:
                        self.err(start, "need exactly 2 blank lines before a class Javadoc")
                    names = []
                    if tm.group(3) == "record":
                        params, close_idx = self.declaration_params(i)
                        if params is None:
                            self.err(i, "record components must use the parameter layout")
                        else:
                            names = self.check_param_block(i, params, close_idx, "record")
                    self.check_javadoc_tags(i, start, end, names, False, True)
                open_idx = i
                while open_idx < len(self.lines) and not self.lines[open_idx].rstrip().endswith("{"):
                    open_idx += 1
                if open_idx < len(self.lines) and self.blanks_after(open_idx) != 2 and self.lines[open_idx + 1].strip() != "}":
                    self.err(open_idx, "need exactly 2 blank lines after the class opening line")
                type_stack.append((depth, name))
                depth += self.brace_delta(line)
                continue
            in_type_body = type_stack and depth == type_stack[-1][0] + 1
            if in_type_body and not stripped.startswith(NOT_METHOD_START) and "=" not in stripped.split("(")[0] \
                    and not stripped.endswith(";") or (in_type_body and self.is_abstract_method(i)):
                self.check_method(i, type_stack[-1][1])
            depth += self.brace_delta(line)
            while type_stack and depth <= type_stack[-1][0]:
                type_stack.pop()
            for cm in re.finditer(r"\b(static\s+final|final\s+static)\s+[\w<>\[\], ?]+\s+(\w+)\s*=", line):
                if not re.match(r"^[A-Z][A-Z0-9_]*$", cm.group(2)):
                    self.err(i, f"constant {cm.group(2)!r} must be UPPER_SNAKE_CASE")

    def is_abstract_method(self, idx):
        stripped = self.lines[idx].strip()
        if stripped.endswith(";") and "(" in stripped and "=" not in stripped and not stripped.startswith(NOT_METHOD_START):
            return METHOD_DECL.match(self.lines[idx]) is not None
        if idx + 1 < len(self.lines) and self.lines[idx + 1].strip() == "(" and not stripped.endswith(("{", ";", ",")):
            return True
        return False

    def check_method(self, i, type_name):
        line = self.lines[i]
        stripped = line.strip()
        if stripped in ("{", "}", "(", ")") or stripped.startswith((")", "}")):
            return
        if re.match(r"^[A-Z][A-Z0-9_]*(\([^)]*\))?(\s*,\s*[A-Z][A-Z0-9_]*(\([^)]*\))?)*\s*[;,]?$", stripped):
            return
        ctor = CTOR_DECL.match(line)
        is_ctor = ctor and ctor.group(4) == type_name
        mm = METHOD_DECL.match(line)
        if not is_ctor and not mm:
            return
        if not is_ctor and (not mm.group(5).strip() or mm.group(5).strip() in ("return", "new", "throw")):
            return
        name = type_name if is_ctor else mm.group(6)
        rest = (ctor.group(5) if is_ctor else mm.group(7)) or ""
        if not is_ctor and not re.match(r"^[a-z][A-Za-z0-9]*$", name):
            self.err(i, f"method name {name!r} must be camelCase")
        params, close_idx = self.declaration_params(i)
        names = []
        if params is None:
            inner = re.match(r"^\((.*)\)", rest)
            if inner is None and not rest:
                return
            if inner and inner.group(1).strip():
                self.err(i, "a method with parameters must put its name, '(' and parameters on separate lines")
            elif inner is not None and not re.search(r"\(\)( throws [\w., ]+)? \{$|\(\);$", stripped):
                self.err(i, "zero-parameter declaration must end with '() {' on one line")
        else:
            kind = "abstract" if self.lines[close_idx].strip().endswith(";") else "body"
            names = self.check_param_block(i, params, close_idx, kind)
        start, end, blanks = self.javadoc_above(i)
        if start is None:
            self.err(i, f"{'constructor' if is_ctor else 'method'} {name} needs a Javadoc comment")
            return
        if blanks != 2:
            self.err(end, "need exactly 2 blank lines after a Javadoc block")
        returns_value = not is_ctor and mm.group(5).strip().split()[-1] != "void"
        self.check_javadoc_tags(i, start, end, names, returns_value, False)

    def check_javadoc_spacing(self):
        for i, line in enumerate(self.lines):
            if line.strip().startswith("/**"):
                before = self.blanks_before(i)
                prev = i - before - 1
                prev_line = self.lines[prev].strip() if prev >= 0 else ""
                after_class_open = prev_line.endswith("{") and self.is_type_opening(prev)
                if not after_class_open and prev >= 0 and not prev_line.startswith(("import ", "package ")) \
                        and prev_line != "*/" and before != 1:
                    nxt = i
                    while nxt < len(self.lines) and self.lines[nxt].strip() != "*/":
                        nxt += 1
                    decl = nxt + self.blanks_after(nxt) + 1
                    while decl < len(self.lines) and self.lines[decl].strip().startswith("@"):
                        decl += 1
                    is_type = decl < len(self.lines) and TYPE_DECL.match(self.lines[decl]) is not None
                    if not is_type:
                        self.err(i, "need exactly 1 blank line before a Javadoc that follows code")
            if line.strip() == "*/" and i > 0:
                j = i
                while j >= 0 and not self.lines[j].strip().startswith(("/**", "/*")):
                    j -= 1
                if j >= 0 and self.lines[j].strip().startswith("/**") and self.blanks_after(i) != 2:
                    self.err(i, "need exactly 2 blank lines after a Javadoc block")

    @staticmethod
    def split_params(text):
        """Split a parameter line on commas that are not inside generic brackets."""
        parts, depth, current = [], 0, ""
        for ch in text:
            if ch == "<":
                depth += 1
            elif ch == ">":
                depth -= 1
            if ch == "," and depth == 0:
                parts.append(current)
                current = ""
            else:
                current += ch
        parts.append(current)
        return parts

    def is_type_opening(self, idx):
        """True when line idx is the '{' line that opens a class, enum, interface or record."""
        if TYPE_DECL.match(self.lines[idx]):
            return True
        if self.lines[idx].strip() == ") {":
            j = idx - 1
            while j >= 0 and self.lines[j].strip() != "(":
                j -= 1
            return j > 0 and TYPE_DECL.match(self.lines[j - 1]) is not None
        return False

    @staticmethod
    def brace_delta(line):
        code = re.sub(r'"(\\.|[^"\\])*"', '""', line)
        code = re.sub(r"'(\\.|[^'\\])*'", "''", code)
        return code.count("{") - code.count("}")


def main():
    root = pathlib.Path(__file__).resolve().parent.parent
    targets = [pathlib.Path(a) for a in sys.argv[1:]] or [root / "api" / "src"]
    files = []
    for t in targets:
        files.extend(sorted(t.rglob("*.java")) if t.is_dir() else [t])
    errors = []
    for f in files:
        errors.extend(Checker(f).run())
    for e in errors:
        print(e)
    print(f"{len(files)} files checked, {len(errors)} violations")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
