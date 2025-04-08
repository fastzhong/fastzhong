#!/bin/bash
# File: filewatcher_service.sh

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILEWATCHER_SCRIPT="$SCRIPT_DIR/filewatcher.sh"
PID_FILE="$SCRIPT_DIR/filewatcher.pid"
CONFIG_FILE="$SCRIPT_DIR/filewatcher_config.conf"

# Source the configuration to get the log pattern
if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
    SERVICE_LOG_NAME="service_$(date +"$LOG_FILE_PATTERN")"
    LOG_FILE="$LOG_DIR/$SERVICE_LOG_NAME"
    
    # Ensure log directory exists
    mkdir -p "$LOG_DIR" 2>/dev/null
else
    # Default if config not found
    LOG_FILE="$SCRIPT_DIR/filewatcher_service.log"
fi

# Function to log messages with timestamp
log_service() {
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $1" >> "$LOG_FILE"
    echo "[$timestamp] $1"
}

# Start the filewatcher service
start_service() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            log_service "Filewatcher is already running with PID $pid"
            return
        else
            log_service "Removing stale PID file"
            rm -f "$PID_FILE"
        fi
    fi
    
    if [ ! -f "$FILEWATCHER_SCRIPT" ]; then
        log_service "ERROR: Filewatcher script not found at $FILEWATCHER_SCRIPT"
        exit 1
    fi
    
    if [ ! -f "$CONFIG_FILE" ]; then
        log_service "ERROR: Configuration file not found at $CONFIG_FILE"
        exit 1
    fi
    
    log_service "Starting filewatcher service..."
    nohup bash "$FILEWATCHER_SCRIPT" > /dev/null 2>&1 &
    echo $! > "$PID_FILE"
    log_service "Filewatcher service started with PID $!"
}

# Stop the filewatcher service
stop_service() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        log_service "Stopping filewatcher service with PID $pid..."
        if kill -15 "$pid" > /dev/null 2>&1; then
            log_service "Filewatcher service stopped"
            rm -f "$PID_FILE"
        else
            log_service "Failed to stop filewatcher service, possibly already stopped"
            rm -f "$PID_FILE"
        fi
    else
        log_service "No PID file found, filewatcher may not be running"
    fi
}

# Restart the filewatcher service
restart_service() {
    stop_service
    sleep 2
    start_service
}

# Check the status of the filewatcher service
status_service() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            log_service "Filewatcher service is running with PID $pid"
        else
            log_service "Filewatcher service is not running (stale PID file exists)"
        fi
    else
        log_service "Filewatcher service is not running"
    fi
}

# Main execution
case "$1" in
    start)
        start_service
        ;;
    stop)
        stop_service
        ;;
    restart)
        restart_service
        ;;
    status)
        status_service
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac

exit 0
