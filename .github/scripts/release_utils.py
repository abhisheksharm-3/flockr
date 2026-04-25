import os
import re
import sys
import subprocess
from typing import Optional

VERSION_FILE = 'version.properties'
CHANGELOG_FILE = 'CHANGELOG.md'
RELEASE_NOTES_FILE = 'RELEASE_NOTES.md'


def run_command(command: str) -> str:
    try:
        result = subprocess.run(
            command, shell=True, check=True, capture_output=True, text=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command: {command}")
        print(f"Stderr: {e.stderr}")
        raise


def read_properties(filepath: str) -> dict:
    props = {}
    if not os.path.exists(filepath):
        return props
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            key, value = line.split('=', 1)
            value = value.strip().strip('"')
            props[key.strip()] = value
    return props


def read_release_notes() -> str:
    if os.path.exists(RELEASE_NOTES_FILE):
        with open(RELEASE_NOTES_FILE, 'r', encoding='utf-8') as f:
            return f.read().strip()
    return 'Minor updates and improvements.'


def parse_semver(version_str: str) -> tuple:
    cleaned = version_str.lstrip('v')
    parts = cleaned.split('.')
    try:
        return tuple(int(p) for p in parts)
    except ValueError:
        return (0, 0, 0)


def get_latest_git_tag() -> Optional[str]:
    try:
        return run_command("git describe --tags --abbrev=0").strip()
    except Exception:
        return None


def set_github_output(key: str, value: str):
    github_output = os.getenv('GITHUB_OUTPUT')
    if github_output:
        with open(github_output, 'a') as f:
            f.write(f"{key}={value}\n")
    else:
        print(f"WARNING: GITHUB_OUTPUT not set — output '{key}={value}' will not propagate")


def update_changelog(version_name: str, release_notes: str, date_str: str):
    header = f"## [{version_name}] - {date_str}\n\n"
    entry = f"{header}{release_notes}\n\n---\n\n"

    if os.path.exists(CHANGELOG_FILE):
        with open(CHANGELOG_FILE, 'r', encoding='utf-8') as f:
            content = f.read()
    else:
        content = "# Changelog\n\nAll notable changes to this project will be documented in this file.\n\n"

    if "## [" in content:
        parts = content.split("## [", 1)
        new_content = parts[0] + entry + "## [" + parts[1]
    else:
        match = re.search(r'^##\s', content, re.MULTILINE)
        if match:
            new_content = content[:match.start()] + entry + content[match.start():]
        else:
            new_content = content + "\n" + entry

    with open(CHANGELOG_FILE, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"Updated {CHANGELOG_FILE} for version {version_name}")


def main():
    if len(sys.argv) < 2:
        print("Usage: release_utils.py [check|update|extract-body]")
        sys.exit(1)

    command = sys.argv[1]

    props = read_properties(VERSION_FILE)
    version_name = props.get('VERSION_NAME')
    version_code = props.get('VERSION_CODE')

    if not version_name:
        print(f"Error: VERSION_NAME not found in {VERSION_FILE}")
        sys.exit(1)

    if command == "check":
        current_tag = get_latest_git_tag()
        target_tag = f"v{version_name}"

        print(f"Current git tag: {current_tag}")
        print(f"Target tag from properties: {target_tag}")

        if current_tag == target_tag:
            print("Version unchanged. No release needed.")
            set_github_output("should_release", "false")
        elif current_tag and parse_semver(version_name) <= parse_semver(current_tag):
            print(f"ERROR: {version_name} is not greater than the current tag {current_tag}. Update VERSION_NAME.")
            sys.exit(1)
        else:
            print("New version detected — release will proceed.")
            set_github_output("should_release", "true")
            set_github_output("version_name", version_name)
            set_github_output("version_code", version_code)

    elif command == "update":
        from datetime import datetime
        release_notes = read_release_notes()
        date_str = datetime.now().strftime("%Y-%m-%d")
        update_changelog(version_name, release_notes, date_str)

    elif command == "extract-body":
        release_notes = read_release_notes()
        with open('RELEASE_BODY.md', 'w', encoding='utf-8') as f:
            f.write(release_notes)
        print("Extracted release notes to RELEASE_BODY.md")

    else:
        print(f"Unknown command: {command}")
        sys.exit(1)


if __name__ == "__main__":
    main()
