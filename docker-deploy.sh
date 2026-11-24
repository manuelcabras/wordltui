#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

set -a; . ./.env; set +a

IMAGE="ghcr.io/${GITHUB_USER,,}/wordltui-java-jar:latest"

docker image build -t "$IMAGE" .
echo "$GITHUB_PAT" | docker login ghcr.io -u "$GITHUB_USER" --password-stdin
docker push "$IMAGE"