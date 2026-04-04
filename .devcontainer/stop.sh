#!/usr/bin/env bash
# Stops the devcontainer for the current workspace (or a given workspace folder).
#
# Usage:
#   .devcontainer/stop.sh
#   .devcontainer/stop.sh --workspace-folder path/to/worktree

set -euo pipefail

WORKSPACE_FOLDER="."

while [[ $# -gt 0 ]]; do
  case "$1" in
    --workspace-folder)
      WORKSPACE_FOLDER="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

WORKSPACE_FOLDER="$(cd "$WORKSPACE_FOLDER" && pwd)"

CONTAINER_ID=$(docker ps -q --filter "label=devcontainer.local_folder=${WORKSPACE_FOLDER}")

if [[ -z "$CONTAINER_ID" ]]; then
  echo "No running devcontainer found for ${WORKSPACE_FOLDER}"
  exit 0
fi

echo "Stopping devcontainer for ${WORKSPACE_FOLDER}..."
docker stop "$CONTAINER_ID"
