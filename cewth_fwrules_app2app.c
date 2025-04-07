lxcewtsgv082:/prodlib/CEWMSTH/batch/FileWatcher $ cat cewth_fwrules_app2app.cfg
# Default values
INTERVAL 10             # sleep interval (seconds)
FROM_TIME 0000  # starting time for all files (hhmm)
MIN_SIZE 1              # minimum size for all files (bytes)
MIN_DETECT 3    # number of iteration for all files
WAIT_TIME 0             # Time limit for all files (minutes)

# TH
ON_FILEWATCH /sftp/ftpgebth/GEB/CEW/in/DMP/* CREATE 4 3 0 0000 1
THEN
DO_CMD /prodlib/CEWMSTH/batch/scripts/ftp/runDMPINCEW.sh
END_ON

# Function   : To stop the file watcher at certain time or ad-hoc for implementations
#              by creating a dumth file
# Frequency  : Schedule or ad-hoc
# From Time  : Default

ON_FILEWATCH /prodlib/CEWMSTH/logs/cewth_fw_app2app_dummy.dat CREATE 0 0 0 0 0
THEN
DO_CMD rm -f /prodlib/CEWMSTH/logs/cewth_fw_app2app_dummy.dat > /dev/null 2>&1
DO_EXIT 0
END_ON
