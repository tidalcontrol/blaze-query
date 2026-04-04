# Devcontainer

A fully-configured development environment for this monorepo. Includes all tools needed to build, test, and run the
backend and frontend without any local installation beyond Docker.

## Why run Claude Code in a container?

Claude Code is an agentic AI that can read and write files, execute shell commands, run tests, and install packages —
all autonomously. That power is exactly why isolation matters.

**Safety by default.** Claude operates inside the container filesystem. If it does something unexpected — runs a
destructive command, modifies the wrong files, or installs something you didn't intend — the blast radius is limited to
the container. Your host machine, other projects, and system config are untouched.

**No host pollution.** Java, Node, pnpm, SonarQube CLI, Sentry CLI, and Claude itself are all inside the image. Nothing
gets installed globally on your machine.

**Reproducible for everyone.** Every developer (and every Claude session) starts from the same image with the same tool
versions. "Works on my machine" problems disappear.

**Parallel isolation with worktrees.** You can run separate Claude Code instances on different branches simultaneously,
each in its own container with its own filesystem. They can't interfere with each other, and you can let Claude work
autonomously on one branch while you code normally on another.

**Persistent Claude memory.** Claude's config and project memory are stored in a named Docker volume, so they survive
container rebuilds. Claude remembers context across sessions without that state living on your host.

## Claude Code configuration

The devcontainer ships a `claude-settings.json` that is merged into `~/.claude/settings.json` on every container start. It configures:

- **Auto memory** (`autoMemoryEnabled: true`): Claude persists project context across sessions in the named Docker volume.
- **No co-authorship** (`includeCoAuthoredBy: false`): Claude never appends `Co-Authored-By` trailers to commits.
- **Bypass permissions** (`permissions.defaultMode: "bypassPermissions"`): Claude runs without tool-call approval prompts. The container is the safety boundary, so this is safe. To disable, set `"defaultMode": "default"` in `~/.claude/settings.json` inside the container (your override will be preserved across rebuilds).

## What's included

| Tool                       | Version      |
|----------------------------|--------------|
| Java (Eclipse Temurin JDK) | 25           |
| Node.js                    | 24           |
| pnpm                       | 10           |
| GitHub CLI (`gh`)          | latest       |
| Sentry CLI                 | pinned       |
| SonarQube CLI              | latest       |
| Claude Code                | latest       |
| Git                        | latest (PPA) |

Maven dependencies, pnpm store, Claude config, and bash history are all persisted in named Docker volumes so they
survive container rebuilds.

## First-time setup

**1. Install the devcontainer CLI** (once, on your host):

```bash
pnpm install -g @devcontainers/cli
```

or

```bash
brew install devcontainers
```

**2. Copy the env file and fill in your tokens:**

```bash
cp .devcontainer/.env.example .devcontainer/.env
```

Then edit `.devcontainer/.env`:

| Variable                  | How to get it                                                   |
|---------------------------|-----------------------------------------------------------------|
| `CLAUDE_CODE_OAUTH_TOKEN` | Run `claude setup-token` locally and copy the output            |
| `GH_TOKEN`                | GitHub → Settings → Developer settings → Personal access tokens |
| `SENTRY_AUTH_TOKEN`       | Sentry → Settings → Developer settings → Personal Token         |
| `SONAR_TOKEN`             | SonarCloud → My Account → Security                              |

> `.devcontainer/.env` is gitignored. Never commit it.

**3. Start the container:**

```bash
.devcontainer/up.sh --workspace-folder .
```

This builds the Docker image on first run (takes a few minutes) and runs `post-create.sh` to configure Claude Code.
Subsequent starts reuse the cached image and volumes.

> **macOS with Colima?** See [Platform notes](#platform-notes) below before starting.

**4. Open a shell inside the container:**

```bash
devcontainer exec --workspace-folder . bash
```

**5. Verify the environment:**

```bash
java -version      # should print 25.x
node --version     # should print 24.x
pnpm --version     # should print 10.x
gh auth status     # should show your GitHub account
claude --version   # should print the Claude Code version
```

## Platform notes

### macOS with Docker Desktop

`up.sh` automatically sets `SSH_AUTH_SOCK` to `/run/host-services/ssh-auth.sock`, the fixed socket path Docker Desktop exposes inside its VM. No extra setup needed.

### macOS with Colima

Docker runs inside Colima's Lima VM, so the standard macOS launchd SSH socket (the default `SSH_AUTH_SOCK`) is not
reachable from inside containers. Colima can expose the host SSH agent through its own socket instead.

**One-time setup:**

```bash
# Start Colima with SSH agent forwarding enabled (add to your usual colima start flags)
colima start --ssh-agent
```

Then always start the devcontainer via the wrapper script:

```bash
.devcontainer/up.sh --workspace-folder .
```

The script detects Colima and automatically sets `SSH_AUTH_SOCK` to `~/.colima/default/agent.sock` before the
container is created. If you use a non-default Colima profile, set `COLIMA_PROFILE` first:

```bash
COLIMA_PROFILE=myprofile .devcontainer/up.sh --workspace-folder .
```

## Daily use

The Maven local repository, pnpm store, Claude settings, and shell history all live in named Docker volumes — they
persist across container rebuilds. Your `~/.gitconfig` is bind-mounted read-only from your host so your name, email,
and signing config are available inside the container automatically.

To stop the container:

```bash
.devcontainer/stop.sh
```

To rebuild the container from scratch (e.g. after a Dockerfile change):

```bash
.devcontainer/up.sh --workspace-folder . --remove-existing-container
```

## Using with git worktrees

Git worktrees let you check out multiple branches simultaneously, each in its own directory. Combined with devcontainers
you can run a separate container per branch — useful for working on parallel features or reviewing a PR while keeping
your current branch untouched.

### A note on relative paths

Each worktree contains a `.git` file pointing back to the bare repo's object store. By default git writes this as an
**absolute path**, which breaks if you ever move the directory. Passing `--relative-paths` to `git worktree add` writes
a relative path instead — worktrees are then portable and survive being moved or renamed. This flag requires a recent
git version, which the devcontainer installs via the Ubuntu PPA.

### Recommended setup: bare clone

A bare clone has no working tree of its own, which makes it a clean hub for multiple worktrees.

```bash
# Clone once as a bare repo (the .git data lives directly inside the directory)
git clone --bare git@github.com:your-org/your-repo.git repo.git

# Add a worktree for each branch you want to work on (--relative-paths keeps worktrees portable)
git -C repo.git worktree add --relative-paths ../feat/my-feature feat/my-feature

# Or create a new branch at the same time
git -C repo.git worktree add --relative-paths -b feat/new-thing ../feat/new-thing main
```

Your directory layout will look like:

```
repo.git/          ← bare repo (git internals only, don't run devcontainer here)
feat/
  my-feature/      ← full working tree, run devcontainer from here
  new-thing/       ← full working tree, run devcontainer from here
```

### Starting a container for a worktree

Because `.devcontainer/` is part of the working tree, each worktree directory contains its own copy of the devcontainer
config and gets its own independent container. Run all commands from inside the worktree directory.

1. **Copy your `.env` file** into the worktree before starting it:
   ```bash
   cp path/to/existing/.devcontainer/.env .devcontainer/.env
   ```

2. Start the container:
   ```bash
   .devcontainer/up.sh --mount-git-worktree-common-dir
   ```
   The `--mount-git-worktree-common-dir` flag mounts the bare repo's shared git object store into the container so git
   works correctly inside it.

3. Open a shell or start Claude directly:
   ```bash
   devcontainer exec --mount-git-worktree-common-dir bash
   devcontainer exec --mount-git-worktree-common-dir claude
   ```

Each worktree gets its own set of named Docker volumes (keyed by `devcontainerId`), so Maven caches, pnpm stores, and
Claude settings don't collide between branches.

### Tips

- **Switching branches**: instead of `git checkout`, just exec into a different worktree's container. Each is fully
  independent.
- **Shared git history**: all worktrees share the same `.git` object store, so fetching in one worktree makes new
  commits visible in all others immediately.
- **Removing a worktree**: `git worktree remove feat/my-feature` (from inside `repo.git` or any other worktree). Clean
  up leftover Docker volumes with `docker volume prune` after removing containers.
- **Listing worktrees**: `git worktree list` from inside any worktree or the bare repo.
