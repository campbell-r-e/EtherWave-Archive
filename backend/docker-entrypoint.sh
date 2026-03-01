#!/bin/sh
# Fix ownership of the data directory when running with a host bind mount,
# then drop privileges to appuser before starting the application.
set -e

# Only chown if we started as root (prod uses named volumes and starts directly
# as appuser; field deploy uses bind mounts and needs this ownership fix).
if [ "$(id -u)" = "0" ]; then
    chown -R appuser:appgroup /app/data
    exec su-exec appuser "$@"
else
    exec "$@"
fi
