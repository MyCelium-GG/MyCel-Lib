#!/usr/bin/env python3
"""Uploads release jars to CurseForge through the upload API.

Shared by every MyCelium repo (see action.yml). Game versions are resolved by
name at run time, so nothing breaks when CurseForge adds versions. The API
token comes from the CURSEFORGE_TOKEN environment variable and is never printed.

Each --file is LOADER=PATH, with LOADER one of fabric, forge, neoforge, or
"all" for Mycel's universal jar (tagged with every loader).
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid

API = "https://minecraft.curseforge.com/api"
LOADERS = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}
ENVIRONMENTS = {"client": ["Client"], "server": ["Server"], "both": ["Client", "Server"]}


def fail(message):
    print(f"::error::{message}")
    sys.exit(1)


def request(token, path, data=None, content_type=None):
    req = urllib.request.Request(API + path, data=data, method="POST" if data is not None else "GET")
    req.add_header("X-Api-Token", token)
    req.add_header("User-Agent", "MyCelium-CurseForge-Upload/1.0 (+https://wiki.mycelium-gg.org)")
    if content_type:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")[:500]
        fail(f"CurseForge {path} answered HTTP {e.code}: {body}")


def resolve_versions(token, mc, java, loaders, environment):
    versions = request(token, "/game/versions")
    types = {t["id"]: t for t in request(token, "/game/version-types")}

    def find(name, type_check):
        for v in versions:
            vtype = types.get(v["gameVersionTypeID"], {})
            if v["name"] == name and type_check(vtype.get("slug", "")):
                return v["id"]
        fail(f"CurseForge has no game version '{name}'")

    ids = [find(name, lambda slug: slug.startswith("minecraft-")) for name in mc.split()]
    ids.append(find(f"Java {java}", lambda slug: slug == "java"))
    for loader in loaders:
        ids.append(find(LOADERS[loader], lambda slug: slug == "modloader"))
    for env in ENVIRONMENTS[environment]:
        ids.append(find(env, lambda slug: slug == "environment"))
    return ids


def mc_label(mc):
    names = mc.split()
    return names[0] if len(names) == 1 else f"{names[0]}-{names[-1]}"


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
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n"
        f"Content-Type: application/json\r\n\r\n{json.dumps(metadata)}\r\n".encode("utf-8"),
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
        f"filename=\"{os.path.basename(file_path)}\"\r\nContent-Type: application/java-archive\r\n\r\n".encode("utf-8"),
        payload,
        f"\r\n--{boundary}--\r\n".encode("utf-8"),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, help="numeric CurseForge project id")
    parser.add_argument("--name", required=True, help="display name, e.g. 'Better Pets'")
    parser.add_argument("--version", required=True)
    parser.add_argument("--mc", required=True, help="Minecraft version(s), space separated, e.g. 26.3 or \"26.1 26.1.1 26.1.2\"")
    parser.add_argument("--java", default="25")
    parser.add_argument("--environment", choices=sorted(ENVIRONMENTS), default="both")
    parser.add_argument("--release-type", choices=["release", "beta", "alpha"], default="release")
    parser.add_argument("--changelog", default="CHANGELOG.md")
    parser.add_argument("--require", action="append", default=[], help="slug of a required project, e.g. mycel")
    parser.add_argument("--fabric-require", action="append", default=[], help="extra required slug for Fabric files")
    parser.add_argument("--file", action="append", required=True, help="LOADER=PATH")
    parser.add_argument("--dry-run", action="store_true", help="resolve and print everything, upload nothing")
    args = parser.parse_args()

    token = os.environ.get("CURSEFORGE_TOKEN", "").strip()
    if not token:
        fail("CURSEFORGE_TOKEN is not set")
    if not args.project.isdigit():
        fail(f"project id must be numeric, got '{args.project}'")

    files = []
    for spec in args.file:
        loader, _, path = spec.partition("=")
        if loader != "all" and loader not in LOADERS:
            fail(f"unknown loader '{loader}' in --file {spec}")
        if not os.path.isfile(path):
            fail(f"no such file: {path}")
        files.append((loader, path))

    notes = changelog_for(args.changelog, args.version)
    if notes is None:
        print(f"::warning::no '## [{args.version}]' section in {args.changelog}; using a short note")
        notes = f"{args.name} {args.version}. See CHANGELOG.md in the repository."

    for loader, path in files:
        loaders = list(LOADERS) if loader == "all" else [loader]
        relations = [{"slug": slug, "type": "requiredDependency"} for slug in args.require]
        if loader == "fabric":
            relations += [{"slug": slug, "type": "requiredDependency"} for slug in args.fabric_require]
        label = "Fabric, Forge, NeoForge" if loader == "all" else LOADERS[loader]
        metadata = {
            "changelog": notes,
            "changelogType": "markdown",
            "displayName": f"{args.name} {args.version} ({label}, {mc_label(args.mc)})",
            "gameVersions": resolve_versions(token, args.mc, args.java, loaders, args.environment),
            "releaseType": args.release_type,
        }
        if relations:
            metadata["relations"] = {"projects": relations}
        if args.dry_run:
            print(f"[dry run] {os.path.basename(path)} -> project {args.project}")
            print(json.dumps({k: v for k, v in metadata.items() if k != "changelog"}, indent=2))
            continue
        body, content_type = multipart(metadata, path)
        result = request(token, f"/projects/{args.project}/upload-file", body, content_type)
        print(f"Uploaded {os.path.basename(path)} as CurseForge file {result.get('id')}")
    if args.dry_run:
        print("--- changelog ---\n" + notes)


if __name__ == "__main__":
    main()
