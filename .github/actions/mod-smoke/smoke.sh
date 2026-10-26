#!/usr/bin/env bash
# Start-up test for one mod on one loader, run from the mod's repo root:
# starts its dev server, starts a dev client that joins it, and passes once
# the client has loaded the mod, joined, and stayed connected for STAY_SECONDS.
# That catches crashes at start-up, mixin or registration failures and
# players being kicked on join.
#
#   smoke.sh fabric|neoforge|forge MOD_ID
#
# SMOKE_SECONDS caps the whole run (default 60). When time is up, everything
# started here is killed; there is no waiting for a clean shutdown.
# Logs land in build/smoke/<loader>/. Exit code 0 means PASS.
set -u
LOADER="${1:?usage: smoke.sh fabric|neoforge|forge MOD_ID}"
MOD_ID="${2:?usage: smoke.sh fabric|neoforge|forge MOD_ID}"
LIMIT="${SMOKE_SECONDS:-60}"
STAY="${STAY_SECONDS:-15}"
OUT="build/smoke/$LOADER"
mkdir -p "$OUT"
rm -f "$OUT"/*.log

# Dev run folders are git-ignored: write what an unattended run needs.
SERVER_DIR="$LOADER/runs/server"
CLIENT_DIR="$LOADER/runs/client"
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

./gradlew ":$LOADER:runServer" --no-daemon < /dev/null > "$OUT/server.log" 2>&1 &
SPID=$!
# Quick play connects once, so give the server a head start; the client
# takes longer to reach its title screen anyway.
until grep -q 'Done (' "$OUT/server.log" 2>/dev/null || [ $SECONDS -ge $((END - LIMIT / 2)) ]; do sleep 1; done
./gradlew ":$LOADER:runClient" --no-daemon -PquickPlay=localhost:25565 > "$OUT/client.log" 2>&1 &
CPID=$!

RESULT="TIMEOUT after ${LIMIT}s"
JOINED=""
while [ $SECONDS -lt $END ]; do
  if grep -q "lost connection" "$OUT/server.log"; then
    RESULT="FAIL: $(grep 'lost connection' "$OUT/server.log" | tail -1 | cut -c1-200)"; break
  fi
  if ! kill -0 $SPID 2>/dev/null; then RESULT="FAIL: server exited"; break; fi
  if ! kill -0 $CPID 2>/dev/null; then RESULT="FAIL: client exited"; break; fi
  if [ -z "$JOINED" ] && grep -q "joined the game" "$OUT/server.log"; then JOINED=$SECONDS; fi
  if [ -n "$JOINED" ] && [ $((SECONDS - JOINED)) -ge "$STAY" ]; then
    if grep -q "\[Mycel/$MOD_ID\] Registered" "$OUT/client.log"; then RESULT="PASS"
    else RESULT="FAIL: the client never registered $MOD_ID"; fi
    break
  fi
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
