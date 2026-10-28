#!/usr/bin/env python3
"""Uploads release jars to Modrinth as one version per loader.

Shared by every MyCelium repo (see action.yml). Required projects are given by
slug and resolved to ids at run time. A loader whose version already exists on
the project for the same Minecraft version is skipped, so re-running a release
never uploads twice. The token
comes from the MODRINTH_TOKEN environment variable and is never printed.

Each --file is LOADER=PATH, with LOADER one of fabric, forge, neoforge, or
"all" for Mycel's universal jar (one version tagged with every loader).

--environment is Modrinth's side setting, which lives on each version:
client_only, server_only_client_optional, client_or_server_prefers_both, ...
Without it Modrinth copies the setting of the project's previous version.
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

API = "https://api.modrinth.com/v2"
USER_AGENT = "MyCelium-GG/modrinth-upload/1.0 (+https://wiki.mycelium-gg.org)"
LOADERS = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


def fail(message):
    print(f"::error::{message}")
    sys.exit(1)


def request(token, path, data=None, content_type=None):
    req = urllib.request.Request(API + path, data=data, method="POST" if data is not None else "GET")
    req.add_header("Authorization", token)
    req.add_header("User-Agent", USER_AGENT)
    if content_type:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            return json.loads(response.read() or b"null")
    except urllib.error.HTTPError as e:
        fail(f"Modrinth {req.get_method()} {path} failed: HTTP {e.code} {e.read().decode('utf-8', 'replace')[:500]}")


def changelog_for(path, version):
    """The CHANGELOG.md section for `version`, without its heading."""
    try:
        with open(path, encoding="utf-8") as f:
            text = f.read()
    except OSError:
        return None
    match = re.search(r"^## \[" + re.escape(version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)", text, re.S | re.M)
    return match.group(1).strip() if match else None


def multipart(metadata, file_path):
    boundary = uuid.uuid4().hex
    with open(file_path, "rb") as f:
        payload = f.read()
    name = os.path.basename(file_path)
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\n"
        f"Content-Type: application/json\r\n\r\n{json.dumps(metadata)}\r\n".encode("utf-8"),
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"jar\"; "
        f"filename=\"{name}\"\r\nContent-Type: application/java-archive\r\n\r\n".encode("utf-8"),
        payload,
        f"\r\n--{boundary}--\r\n".encode("utf-8"),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, help="Modrinth project id or slug")
    parser.add_argument("--name", required=True, help="display name, e.g. 'Better Pets'")
    parser.add_argument("--version", required=True)
    parser.add_argument("--mc", required=True, help="Minecraft version(s), space separated, e.g. 26.3 or \"26.1 26.1.1 26.1.2\"")
    parser.add_argument("--release-type", choices=["release", "beta", "alpha"], default="release")
    parser.add_argument("--environment", default="", help="Modrinth environment, e.g. client_only")
    parser.add_argument("--changelog", default="CHANGELOG.md")
    parser.add_argument("--require", action="append", default=[], help="slug of a required project, e.g. mycel")
    parser.add_argument("--fabric-require", action="append", default=[], help="extra required slug for Fabric files")
    parser.add_argument("--file", action="append", required=True, help="LOADER=PATH")
    parser.add_argument("--dry-run", action="store_true", help="resolve and print everything, upload nothing")
    args = parser.parse_args()

    token = os.environ.get("MODRINTH_TOKEN", "").strip()
    if not token:
        fail("MODRINTH_TOKEN is not set")

    files = []
    for spec in args.file:
        loader, _, path = spec.partition("=")
        if loader != "all" and loader not in LOADERS:
            fail(f"unknown loader '{loader}' in --file {spec}")
        if not os.path.isfile(path):
            fail(f"no such file: {path}")
        files.append((loader, path))

    project = request(token, "/project/" + urllib.parse.quote(args.project))
    ids = {}

    def project_id(slug):
        if slug not in ids:
            ids[slug] = request(token, "/project/" + urllib.parse.quote(slug))["id"]
        return ids[slug]

    mcs = args.mc.split()
    mc_label = mcs[0] if len(mcs) == 1 else f"{mcs[0]}-{mcs[-1]}"

    existing = request(token, f"/project/{project['id']}/version")
    # One Mycel/mod version can ship for several Minecraft versions, so a
    # loader only counts as uploaded for the same Minecraft version.
    taken = {(v["version_number"], loader, mc) for v in existing for loader in v["loaders"] for mc in v["game_versions"]}

    notes = changelog_for(args.changelog, args.version)
    if notes is None:
        print(f"::warning::no '## [{args.version}]' section in {args.changelog}; using a short note")
        notes = f"{args.name} {args.version}. See CHANGELOG.md in the repository."

    for loader, path in files:
        loaders = list(LOADERS) if loader == "all" else [loader]
        if any((args.version, l, mc) in taken for l in loaders for mc in mcs):
            print(f"{args.name} {args.version} for {', '.join(loaders)} on {mc_label} is already on Modrinth; skipping")
            continue
        required = [s for s in args.require + (args.fabric_require if loader == "fabric" else []) if s]
        label = "Fabric, Forge, NeoForge" if loader == "all" else LOADERS[loader]
        metadata = {
            "project_id": project["id"],
            "name": f"{args.name} {args.version} ({label}, {mc_label})",
            "version_number": args.version,
            "changelog": notes,
            "dependencies": [{"project_id": project_id(s), "dependency_type": "required"} for s in required],
            "game_versions": mcs,
            "version_type": args.release_type,
            "loaders": loaders,
            "featured": True,
            "file_parts": ["jar"],
            "primary_file": "jar",
        }
        if args.environment:
            metadata["environment"] = args.environment
        if args.dry_run:
            print(f"[dry run] {os.path.basename(path)} -> {project['slug']}")
            print(json.dumps({k: v for k, v in metadata.items() if k != "changelog"}, indent=2))
            continue
        body, content_type = multipart(metadata, path)
        result = request(token, "/version", body, content_type)
        print(f"Uploaded {os.path.basename(path)} as Modrinth version {result.get('id')}")
    if args.dry_run:
        print("--- changelog ---\n" + notes)


if __name__ == "__main__":
    main()
