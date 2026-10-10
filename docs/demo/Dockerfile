FROM ghcr.io/charmbracelet/vhs:v0.9.0
# The image's apt sources say "stable", which has moved on from the bookworm it was built on.
RUN sed -i 's/\bstable\b/bookworm/g' /etc/apt/sources.list.d/*.sources /etc/apt/sources.list 2>/dev/null; \
    apt-get update \
    && apt-get install -y --no-install-recommends fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*
