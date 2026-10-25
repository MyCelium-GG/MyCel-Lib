#!/usr/bin/env bash
# Start-up test for one loader: starts the example mod's dev server, starts a
# dev client that joins it, and passes once the server logs the client's reply
# packet ("saw the greeting"). That exercises loading, the update checker,
# both packet directions and joining with optional channels.
#
#   scripts/smoke.sh fabric|neoforge|forge
#
# SMOKE_SECONDS caps the whole run (default 60). When time is up, everything
# started here is killed; there is no waiting for a clean shutdown.
# Logs land in build/smoke/<loader>/. Exit code 0 means PASS.
set -u
LOADER="${1:?usage: scripts/smoke.sh fabric|neoforge|forge}"
LIMIT="${SMOKE_SECONDS:-60}"
cd "$(dirname "$0")/.."
OUT="build/smoke/$LOADER"
mkdir -p "$OUT"
rm -f "$OUT"/*.log

# Dev run folders are git-ignored: write what an unattended run needs.
SERVER_DIR="example-$LOADER/runs/server"
CLIENT_DIR="example-$LOADER/runs/client"
mkdir -p "$SERVER_DIR" "$CLIENT_DIR"
echo "eula=true" > "$SERVER_DIR/eula.txt"
props="$SERVER_DIR/server.properties"
touch "$props"
for kv in online-mode=false white-list=false enforce-whitelist=false; do
  key="${kv%%=*}"
  grep -q "^$key=" "$props" && sed -i "s/^$key=.*/$kv/" "$props" || echo "$kv" >> "$props"
done
opts="$CLIENT_DIR/options.txt"
touch "$opts"
for kv in onboardAccessibility:false skipMultiplayerWarning:true; do
  key="${kv%%:*}"
  grep -q "^$key:" "$opts" && sed -i "s/^$key:.*/$kv/" "$opts" || echo "$kv" >> "$opts"
done

kill_tree() { local c; for c in $(pgrep -P "$1"); do kill_tree "$c"; done; kill -9 "$1" 2>/dev/null; }
END=$((SECONDS + LIMIT))

./gradlew ":example-$LOADER:runServer" --no-daemon < /dev/null > "$OUT/server.log" 2>&1 &
SPID=$!
# Quick play connects once, so give the server a head start; the client
# takes longer to reach its title screen anyway.
until grep -q 'Done (' "$OUT/server.log" 2>/dev/null || [ $SECONDS -ge $((END - LIMIT / 2)) ]; do sleep 1; done
./gradlew ":example-$LOADER:runClient" --no-daemon -PquickPlay=localhost:25565 > "$OUT/client.log" 2>&1 &
CPID=$!

RESULT="TIMEOUT after ${LIMIT}s"
while [ $SECONDS -lt $END ]; do
  if grep -q "saw the greeting" "$OUT/server.log"; then RESULT="PASS"; break; fi
  if grep -q "lost connection" "$OUT/server.log"; then
    RESULT="FAIL: $(grep 'lost connection' "$OUT/server.log" | tail -1 | cut -c1-200)"; break
  fi
  if ! kill -0 $SPID 2>/dev/null; then RESULT="FAIL: server exited"; break; fi
  sleep 1
done
kill_tree $CPID
kill_tree $SPID

echo "$LOADER: $RESULT"
if [ "$RESULT" != PASS ]; then
  echo "--- last server lines"; tail -15 "$OUT/server.log"
  echo "--- client errors"; grep -E "Exception|Caused by|ERROR" "$OUT/client.log" | tail -15
  exit 1
fi
