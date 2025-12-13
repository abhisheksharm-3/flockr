import os
import re
import sys
import subprocess
from typing import Tuple, Optional

# --- Constants ---
VERSION_FILE = 'version.properties'
CHANGELOG_FILE = 'CHANGELOG.md'
GITHUB_ENV = os.getenv('GITHUB_ENV')

def run_command(command: str) -> str:
    """Run a shell command and return stdout, raising error on failure."""
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
    """Read a .properties file into a dictionary."""
    props = {}
    if not os.path.exists(filepath):
        return props
    
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                key, value = line.split('=', 1)
                # Remove quotes if present
                value = value.strip()
                if value.startswith('"') and value.endswith('"'):
                    value = value[1:-1]
                props[key.strip()] = value
    return props

def get_latest_git_tag() -> Optional[str]:
    """Get the latest git tag, or None if no tags exist."""
    try:
        # Get latest tag across all branches, sorted by version creation date (simplified)
        # Using git describe to get exactly the closest tag
        return run_command("git describe --tags --abbrev=0").strip()
    except Exception:
        return None

def update_changelog(version_name: str, release_notes: str, date_str: str):
    """Prepend new release note to CHANGELOG.md."""
    
    header = f"## [{version_name}] - {date_str}\n\n"
    # Format release notes: ensure they look good in markdown
    # If release notes contain literal "\n", replace with actual newlines
    formatted_notes = release_notes.replace('\\n', '\n')
    
    entry = f"{header}{formatted_notes}\n\n---\n\n"
    
    if os.path.exists(CHANGELOG_FILE):
        with open(CHANGELOG_FILE, 'r', encoding='utf-8') as f:
            content = f.read()
    else:
        content = "# Changelog\n\nAll notable changes to this project will be documented in this file.\n\n"
        
    # Find insertion point (after header if it exists, otherwise at top)
    # Heuristic: insert after the first few lines of header if present, or just after title
    # For now, simplistic approach: Find the first "## [" line and insert before it, 
    # or append if not found but file exists.
    
    if "## [" in content:
        # split at first occurrence
        parts = content.split("## [", 1)
        new_content = parts[0] + entry + "## [" + parts[1]
    else:
        # Maybe just a header? Append after "Changelog" title if possible
        if "# Changelog" in content:
             lines = content.splitlines()
             # Insert after header block (usually 2-5 lines)
             insert_idx = len(lines)
             for i, line in enumerate(lines):
                 if line.strip() == "":
                      # Start looking for real content
                      pass
             
             # Fallback: Just insert at top if we can't be smart, but user wants "clean"
             # Let's try: Header + \n\n + New Entry + \n + Old Content (minus Header)
             # Actually, easiest "Safe" way is to look for the first H2 "##"
             match = re.search(r'^##\s', content, re.MULTILINE)
             if match:
                 idx = match.start()
                 new_content = content[:idx] + entry + content[idx:]
             else:
                 # No existing releases, just append
                 new_content = content + "\n" + entry
        else:
            new_content = entry + content

    with open(CHANGELOG_FILE, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"Updated {CHANGELOG_FILE} for version {version_name}")

def set_github_output(key: str, value: str):
    """Set output for GitHub Actions."""
    if os.getenv('GITHUB_OUTPUT'):
        with open(os.getenv('GITHUB_OUTPUT'), 'a') as f:
            f.write(f"{key}={value}\n")
    print(f"::set-output name={key}::{value}") # Fallback

def main():
    if len(sys.argv) < 2:
        print("Usage: release_utils.py [check|update]")
        sys.exit(1)
        
    command = sys.argv[1]
    
    props = read_properties(VERSION_FILE)
    version_name = props.get('VERSION_NAME')
    version_code = props.get('VERSION_CODE')
    release_notes = props.get('RELEASE_NOTES', 'Minor updates and bug fixes.')
    
    if not version_name:
        print(f"Error: VERSION_NAME not found in {VERSION_FILE}")
        sys.exit(1)

    if command == "check":
        current_tag = get_latest_git_tag()
        target_tag = f"v{version_name}"
        
        print(f"Current git tag: {current_tag}")
        print(f"Target tag from properties: {target_tag}")
        
        if current_tag == target_tag:
            print("Version has not changed. No release needed.")
            set_github_output("should_release", "false")
        else:
            print("New version detected!")
            set_github_output("should_release", "true")
            set_github_output("version_name", version_name)
            set_github_output("version_code", version_code)
            

    elif command == "update":
        from datetime import datetime
        date_str = datetime.now().strftime("%Y-%m-%d")
        update_changelog(version_name, release_notes, date_str)
        
    elif command == "extract-body":
        # Write just the release notes to a file for GitHub Release
        # We can format it nicely here too
        formatted_notes = release_notes.replace('\\n', '\n')
        with open('RELEASE_BODY.md', 'w', encoding='utf-8') as f:
            f.write(formatted_notes)
        print(f"Extracted release notes to RELEASE_BODY.md")

    else:
        print(f"Unknown command: {command}")
        sys.exit(1)

if __name__ == "__main__":
    main()
