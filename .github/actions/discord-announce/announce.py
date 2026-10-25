"""Posts a release to Discord. Standard library only; see action.yml."""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

MODS_JSON = "https://wiki.mycelium-gg.org/mods.json"
WIKI = "https://wiki.mycelium-gg.org/"
RED = 0xEF5350  # the wiki's brand red
LIMIT = 3900    # Discord caps an embed description at 4096 characters
USER_AGENT = "MyCelium-release (https://github.com/MyCelium-GG, 1.0)"


def gradle_property(name):
    try:
        with open("gradle.properties", encoding="utf-8") as f:
            for line in f:
                key, _, value = line.partition("=")
                if key.strip() == name:
                    return value.strip()
    except OSError:
        pass
    return ""


def changelog_for(path, version):
    """The CHANGELOG.md section for `version`, without its heading."""
    try:
        with open(path, encoding="utf-8") as f:
            text = f.read()
    except OSError:
        return None
    match = re.search(r"^## \[" + re.escape(version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)", text, re.S | re.M)
    return match.group(1).strip() if match else None


def for_discord(notes):
    """Keep a Changelog markdown, reshaped for Discord: headings become bold
    lines, wrapped bullet lines are joined."""
    lines = []
    for raw in notes.splitlines():
        line = raw.rstrip()
        heading = re.match(r"^#{3,}\s+(.*)", line)
        if heading:
            lines.append(f"**{heading.group(1)}**")
        elif line.startswith("  ") and lines and lines[-1].startswith("- "):
            lines[-1] += " " + line.strip()
        else:
            lines.append(line)
    text = re.sub(r"\n{3,}", "\n\n", "\n".join(lines)).strip()
    if len(text) > LIMIT:
        text = text[:LIMIT].rsplit("\n", 1)[0] + "\n…"
    return text


def lookup(mod_id):
    try:
        request = urllib.request.Request(MODS_JSON, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=30) as response:
            data = json.load(response)
    except (OSError, ValueError) as e:
        print(f"::warning::could not read {MODS_JSON}: {e}")
        return None
    for entry in [data.get("mycel", {})] + data.get("mods", []):
        if entry.get("id") == mod_id:
            return entry
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--mod-id", default="")
    parser.add_argument("--changelog", default="CHANGELOG.md")
    parser.add_argument("--repo", default="")
    args = parser.parse_args()

    webhook = os.environ.get("DISCORD_WEBHOOK", "").strip()
    if not webhook:
        print("::notice::DISCORD_WEBHOOK is not set; skipping the Discord announcement.")
        return 0

    mod_id = args.mod_id or gradle_property("mod_id")
    entry = lookup(mod_id) or {}
    name = entry.get("name") or gradle_property("mod_name") or mod_id
    download = entry.get("download") or (args.repo + "/releases/tag/" + args.version)
    docs = WIKI + entry["docs"] if entry.get("docs") else WIKI
    mc = gradle_property("minecraft_version") or "26.3"

    notes = changelog_for(args.changelog, args.version)
    description = for_discord(notes) if notes else f"See the changelog on {args.repo}."
    description += f"\n\n[Download]({download}) · [Docs]({docs})"
    if args.repo:
        description += f" · [Source]({args.repo})"

    payload = {
        "username": "MyCelium",
        "allowed_mentions": {"parse": []},
        "embeds": [{
            "title": f"{name} {args.version}",
            "url": download,
            "description": description,
            "color": RED,
            "footer": {"text": f"Minecraft {mc} · Fabric, Forge and NeoForge"},
        }],
    }
    request = urllib.request.Request(
        webhook, data=json.dumps(payload).encode("utf-8"), method="POST",
        headers={"Content-Type": "application/json", "User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            print(f"Announced {name} {args.version} on Discord (HTTP {response.status}).")
    except urllib.error.HTTPError as e:
        # A failed announcement must never fail a release that already shipped.
        print(f"::warning::Discord answered HTTP {e.code}: {e.read()[:300]!r}")
    except OSError as e:
        print(f"::warning::could not reach Discord: {e}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
