FROM eclipse-temurin:25-jdk-noble

ENV TZ="Europe/Amsterdam"
ENV NODE_MAJOR=24
ENV PNPM_VERSION=10
# Install latest git (needed for worktree relative paths support)
RUN apt-get update && apt-get install -y --no-install-recommends software-properties-common \
    && add-apt-repository -y ppa:git-core/ppa \
    && apt-get update && apt-get install -y --no-install-recommends git \
    && apt-get purge -y software-properties-common \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install base development tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    procps \
    sudo \
    unzip \
    gnupg2 \
    jq \
    curl \
    wget \
    ca-certificates \
    openssh-client \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_MAJOR}.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install pnpm
RUN corepack enable && corepack prepare pnpm@${PNPM_VERSION} --activate

# Install GitHub CLI
RUN (type -p wget >/dev/null || apt-get install wget -y) \
    && mkdir -p -m 755 /etc/apt/keyrings \
    && out=$(mktemp) && wget -nv -O$out https://cli.github.com/packages/githubcli-archive-keyring.gpg \
    && cat $out | tee /etc/apt/keyrings/githubcli-archive-keyring.gpg > /dev/null \
    && chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
    && apt-get update && apt-get install -y gh \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Sentry CLI
RUN curl -sL https://sentry.io/get-cli/ | bash \
    && sentry-cli --version

# Install SonarQube CLI
RUN curl -o- https://raw.githubusercontent.com/SonarSource/sonarqube-cli/refs/heads/master/user-scripts/install.sh | bash

# Reuse existing ubuntu user (UID 1000) from base image
ARG USERNAME=ubuntu

RUN echo "${USERNAME} ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/${USERNAME} \
    && chmod 0440 /etc/sudoers.d/${USERNAME}

RUN mkdir /commandhistory \
    && touch /commandhistory/.bash_history \
    && chown -R ${USERNAME} /commandhistory

ENV DEVCONTAINER=true
ENV PATH="/home/ubuntu/.local/bin:/home/ubuntu/.local/share/sonarqube-cli/bin:${PATH}"
ENV HISTFILE=/commandhistory/.bash_history
ENV PROMPT_COMMAND="history -a"

# Switch to non-root user
USER ${USERNAME}

# Install OpenSpec
RUN sudo npm install -g @fission-ai/openspec

# Install Claude Code (native binary)
RUN curl -fsSL https://claude.ai/install.sh | bash

ENV SHELL=/bin/bash
