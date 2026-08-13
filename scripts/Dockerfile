# Dockerfile to build llama.cpp and llmbridge for Android (optional)
# This image contains the Android NDK and necessary tools to cross-compile.
# Usage example:
# docker build -t llm-android-builder -f scripts/Dockerfile .
# docker run --rm -v $(pwd):/work llm-android-builder /work/scripts/build-llama-android.sh

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential cmake git wget unzip python3 python3-pip curl pkg-config \
    ca-certificates && rm -rf /var/lib/apt/lists/*

# Install Android NDK (choose desired version)
ENV NDK_VERSION=r25b
RUN mkdir -p /opt && \
    cd /opt && \
    wget -q https://dl.google.com/android/repository/android-ndk-${NDK_VERSION}-linux.zip && \
    unzip android-ndk-${NDK_VERSION}-linux.zip && \
    rm android-ndk-${NDK_VERSION}-linux.zip

ENV ANDROID_NDK=/opt/android-ndk-${NDK_VERSION}
ENV PATH=${PATH}:${ANDROID_NDK}

WORKDIR /work

# Default command helps running the helper script mounted from the host
CMD ["/bin/bash"]
