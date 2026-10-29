#!/bin/zsh
# Restart the local test server in ~/mc-server (refuses if players are online).
# Usage: scripts/dev-server.sh [restart|stop|start|log]
cd ~/mc-server || exit 1
running() { pgrep -f 'java.*paper.jar' >/dev/null }  # not -x java: the Gradle daemon is a java process too
stop_server() {
  running || return 0
  echo list > console.in; sleep 2
  if ! tail -n 5 server.log | grep -q "There are 0 of"; then
    echo "Players are online (or the server did not answer); not stopping." >&2; exit 1
  fi
  echo stop > console.in
  for i in {1..90}; do running || return 0; sleep 1; done
  echo "Server did not stop in 90s" >&2; exit 1
}
start_server() { (nohup ./start.sh >/dev/null 2>&1 &); }
case "${1:-restart}" in
  stop) stop_server ;;
  start) start_server ;;
  restart) stop_server; start_server ;;
  log) tail -n 80 server.log ;;
esac
