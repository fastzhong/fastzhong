# lxcewtsgv082:/prodlib/CEWMSTH/batch/FileWatcher $ cat cewth_startfw_app2cd.sh
#!/bin/ksh

#------------------------------------------------------------------------------
# Func : Start the file watcher for CEW
# Desc : This program is activated via CTRL-M.
#        Start the file watcher using CTMFW command with input as rules file
#------------------------------------------------------------------------------
# Input parameters:
#------------------------------------------------------------------------------
# History:
# Date       Author ID  Description
# ---------- ---------  ------------
#
#------------------------------------------------------------------------------

. $( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )/set_batch_env.sh

cewdt=`date +'%Y/%m/%d %H:%M:%S'`
fwrunning=`ps -ef| grep ctmfw | grep cewth_fwrules_app2cd.cfg | wc -l`
if [ $fwrunning -eq 0 ]; then
        echo "$cewdt CEW  ApptoCD FILE WATCHER STARTING. . ." >> $CEWFW_ApptoCD_LOGFILE
        echo "From $CEWFW_ApptoCD_DIR" >> $CEWFW_ApptoCD_LOGFILE
        $CONTROLM/exe/ctmfw -input $CEWFW_ApptoCD_DIR/"cewth_fwrules_app2cd.cfg"
        echo "$cewdt CEW  ApptoCD FILE WATCHER STARTED. . ." >> $CEWFW_ApptoCD_LOGFILE
else
        echo "$cewdt [Error]-FILE WATCHER ALREADY RUNNING . . ." >> $CEWFW_ApptoCD_LOGFILE
fi
