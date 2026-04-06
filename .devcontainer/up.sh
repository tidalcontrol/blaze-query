#!/usr/bin/env bash
# Cross-platform wrapper around `devcontainer up`.
#
# Normalises SSH_AUTH_SOCK before handing off to `devcontainer up` so that SSH
# agent forwarding works regardless of the Docker backend in use:
#
#   macOS + Docker Desktop  →  /run/host-services/ssh-auth.sock (VM socket)
#   macOS + Colima          →  socket reported by `colima ssh`
#   Linux / other           →  SSH_AUTH_SOCK passed through unchanged
#
# Usage: same as `devcontainer up`, e.g.
#   .devcontainer/up.sh --workspace-folder .
#   .devcontainer/up.sh --mount-git-worktree-common-dir

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ ! -f "$SCRIPT_DIR/.env" ]]; then
  echo "ERROR: .devcontainer/.env not found." >&2
  echo "       Copy the example file and fill in your tokens:" >&2
  echo "         cp .devcontainer/.env.example .devcontainer/.env" >&2
  exit 1
fi

if [[ "$(uname -s)" == "Darwin" ]]; then
  if command -v colima &>/dev/null && colima status &>/dev/null 2>&1; then
    # Colima exposes the host SSH agent via a socket inside its data directory.
    # This path is accessible from Docker containers; the launchd socket is not.
    COLIMA_SOCK="$(colima ssh -- bash -c 'echo $SSH_AUTH_SOCK')"

    if [[ -n "$COLIMA_SOCK" ]]; then
      export SSH_AUTH_SOCK=$COLIMA_SOCK
      echo "Colima detected: setting SSH_AUTH_SOCK to $SSH_AUTH_SOCK"
    elif [[ ! -S "${SSH_AUTH_SOCK:-}" ]]; then
      echo "WARNING: SSH_AUTH_SOCK is not set or not a socket, and no Colima agent socket found." >&2
      echo "         SSH agent forwarding will not work inside the container." >&2
      echo "         Run \`colima start --ssh-agent\` and retry, or set SSH_AUTH_SOCK manually." >&2
    fi
  else
    # Docker Desktop on macOS exposes the SSH agent at a fixed VM socket path.
    DOCKER_DESKTOP_SOCK="/run/host-services/ssh-auth.sock"
    export SSH_AUTH_SOCK=$DOCKER_DESKTOP_SOCK
    echo "Docker Desktop detected: setting SSH_AUTH_SOCK to $SSH_AUTH_SOCK"
  fi
fi

exec devcontainer up "$@"
