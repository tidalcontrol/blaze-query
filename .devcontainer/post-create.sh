#!/bin/bash
set -euo pipefail

# Validate required environment variables
missing=()
[[ -z "${CLAUDE_CODE_OAUTH_TOKEN:-}" ]] && missing+=("CLAUDE_CODE_OAUTH_TOKEN")
[[ -z "${GH_TOKEN:-}" ]] && missing+=("GH_TOKEN")
if [[ ${#missing[@]} -gt 0 ]]; then
  echo "ERROR: Missing required environment variable(s): ${missing[*]}" >&2
  echo "       Set them in .devcontainer/.env and rebuild the container." >&2
  exit 1
fi

sudo chown -R ubuntu:ubuntu /home/ubuntu/.m2 /home/ubuntu/.claude /commandhistory
sudo git config --system --add safe.directory '*'

# Fix SSH agent forwarding for host SSH agents with hardcoded socket paths (e.g.
# 1Password). ~/.ssh/config may contain IdentityAgent directives pointing to
# macOS-specific paths that do not exist inside the container. We create a config
# override that sets IdentityAgent /ssh-agent first (first value wins in SSH) and
# then includes the host config for all other settings.
#
# An ssh wrapper script is installed early in PATH so that every SSH operation —
# including interactive ssh, git, and any tool that shells out to ssh — uses the
# override config automatically without needing GIT_SSH_COMMAND or manual flags.
mkdir -p /home/ubuntu/.ssh-container /home/ubuntu/.local/bin
cat > /home/ubuntu/.ssh-container/config << 'EOF'
# Force use of the forwarded agent socket. Must come first so it wins over any
# IdentityAgent in the included configs that point to host-specific paths.
Host *
    IdentityAgent /ssh-agent

# Include 1Password's generated key-selection config explicitly using the
# container path. The main ~/.ssh/config references it via an absolute macOS
# path (/Users/…) which does not exist inside the container, so without this
# the per-host IdentitiesOnly/IdentityFile entries are silently skipped and SSH
# tries all agent keys in order, picking the wrong one.
Include /home/ubuntu/.ssh/1Password/config

Include /home/ubuntu/.ssh/config
EOF
cat > /home/ubuntu/.local/bin/ssh << 'EOF'
#!/bin/bash
exec /usr/bin/ssh -F /home/ubuntu/.ssh-container/config "$@"
EOF
chmod +x /home/ubuntu/.local/bin/ssh

[ -f /home/ubuntu/.claude/.claude.json ] || cp .devcontainer/claude.json /home/ubuntu/.claude/.claude.json
ln -sfn /home/ubuntu/.claude/.claude.json /home/ubuntu/.claude.json

# Merge mandatory settings into the Claude settings file, preserving any user customisations.
# This runs on every container start so new mandatory keys are always present even on existing volumes.
settings_file=/home/ubuntu/.claude/settings.json
[ -f "$settings_file" ] || echo '{}' > "$settings_file"
jq --slurpfile defaults .devcontainer/claude-settings.json \
  '$defaults[0] * .' "$settings_file" > /tmp/claude-settings-merged.json \
  && mv /tmp/claude-settings-merged.json "$settings_file"

# Install Claude Code plugins declared in claude-settings.json so they are ready
# on first launch without needing a manual /reload-plugins.
jq -r '.enabledPlugins | to_entries[] | select(.value == true) | .key' \
  .devcontainer/claude-settings.json \
  | while read -r plugin; do
      claude plugins install "$plugin" 2>/dev/null || true
    done

# Configure OpenSpec CLI (user-level only — workspace config lives in openspec/config.yaml in the repo)
mkdir -p /home/ubuntu/.config/openspec
cat > /home/ubuntu/.config/openspec/config.json << 'EOF'
{
  "profile": "custom",
  "delivery": "both",
  "workflows": [
    "propose",
    "explore",
    "new",
    "continue",
    "apply",
    "ff",
    "sync",
    "archive",
    "bulk-archive",
    "verify",
    "onboard"
  ],
  "featureFlags": {}
}
EOF
