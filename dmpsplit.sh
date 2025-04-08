
#!/bin/bash
# File: filewatcher.sh

# Load configuration
CONFIG_FILE="./filewatcher_config.conf"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# Create log file with date pattern from configuration
LOG_FILE_NAME=$(date +"$LOG_FILE_PATTERN")
LOG_FILE="$LOG_DIR/$LOG_FILE_NAME"

# Function to log messages with timestamp
log_message() {
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    local message="$1"
    echo "[$timestamp] $message" >> "$LOG_FILE"
    echo "[$timestamp] $message"
}

# Check and create required directories
check_directories() {
    if [ ! -d "$SRC_DIR" ]; then
        log_message "ERROR: Source directory does not exist: $SRC_DIR"
        exit 1
    fi

    for dir in "$NORMAL_DIR" "$LARGE_DIR" "$BACKUP_DIR" "$ERROR_DIR" "$LOG_DIR"; do
        if [ ! -d "$dir" ]; then
            log_message "Creating directory: $dir"
            mkdir -p "$dir"
            if [ $? -ne 0 ]; then
                log_message "ERROR: Failed to create directory: $dir"
                exit 1
            fi
        fi
    done
}

# Process a single file
process_file() {
    local filepath="$1"
    local filename=$(basename "$filepath")
    
    # Skip if not a regular file
    if [ ! -f "$filepath" ]; then
        return
    fi
    
    # Get the file size
    local file_size=$(stat -c %s "$filepath")
    
    # Determine destination based on size
    if [ "$file_size" -le "$SIZE_THRESHOLD" ]; then
        local dest_dir="$NORMAL_DIR"
        local size_type="normal"
    else
        local dest_dir="$LARGE_DIR"
        local size_type="large"
    fi
    
    # Copy the file to the appropriate destination
    log_message "Copying $filename ($file_size bytes) to $size_type directory..."
    cp "$filepath" "$dest_dir/"
    
    # Check if the copy was successful
    if [ $? -eq 0 ]; then
        log_message "Successfully copied $filename to $dest_dir/"
        
        # Move the file to the backup directory
        log_message "Moving $filename to backup directory..."
        mv "$filepath" "$BACKUP_DIR/"
        
        if [ $? -eq 0 ]; then
            log_message "Successfully moved $filename to $BACKUP_DIR/"
        else
            log_message "Failed to move $filename to $BACKUP_DIR/, moving to error directory instead..."
            mv "$filepath" "$ERROR_DIR/"
            
            if [ $? -eq 0 ]; then
                log_message "Successfully moved $filename to $ERROR_DIR/"
            else
                log_message "CRITICAL ERROR: Failed to move $filename to either backup or error directory"
            fi
        fi
    else
        log_message "Failed to copy $filename to $dest_dir/, moving to error directory..."
        mv "$filepath" "$ERROR_DIR/"
        
        if [ $? -eq 0 ]; then
            log_message "Successfully moved $filename to $ERROR_DIR/"
        else
            log_message "CRITICAL ERROR: Failed to copy file and failed to move to error directory"
        fi
    fi
}

# Main watch function using inotify
watch_directory_inotify() {
    log_message "Starting file watcher for $SRC_DIR using inotify..."
    
    # Make sure inotify-tools is installed
    if ! command -v inotifywait &> /dev/null; then
        log_message "ERROR: inotify-tools is not installed. Please install it first."
        log_message "On Debian/Ubuntu: sudo apt-get install inotify-tools"
        log_message "On CentOS/RHEL: sudo yum install inotify-tools"
        log_message "Falling back to polling mode..."
        watch_directory_polling
        return
    fi

    # Watch the directory for new files
    inotifywait -m -r "$SRC_DIR" -e close_write -e moved_to |
    while read -r directory events filename; do
        # Full path to the file
        local filepath="${directory}${filename}"
        process_file "$filepath"
        # Add a small delay between processing events
        sleep 1
    done
}

# Alternative watch function using polling (as fallback)
watch_directory_polling() {
    log_message "Starting file watcher for $SRC_DIR using polling mode..."
    
    while true; do
        # Find all regular files in the source directory
        find "$SRC_DIR" -type f | while read -r filepath; do
            process_file "$filepath"
        done
        
        # Wait before next polling cycle
        log_message "Sleeping for $POLL_INTERVAL seconds before next check..."
        sleep "$POLL_INTERVAL"
    done
}

# Main execution
check_directories
log_message "File watcher service started"
watch_directory_inotify

