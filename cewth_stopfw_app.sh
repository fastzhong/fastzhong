# lxcewtsgv082:/prodlib/CEWMSTH/batch/FileWatcher $ cat cewth_stopfw_app2cd.sh
#!/bin/ksh
set -x
. $( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )/set_batch_env.sh
cewdt=`date +'%Y/%m/%d %H:%M:%S'`
fwrunning=`ps -ef| grep ctmfw | grep cewth_fwrules_app2cd.cfg | wc -l`
if [ $fwrunning -eq 0 ]; then
        print "$cewdt FILE WATCHER NOT RUNNING . . ." >> $CEWFW_ApptoCD_LOGFILE
else
        print "$cewdt FILE WATCHER RUNNING . . ." >> $CEWFW_ApptoCD_LOGFILE
        touch $CEWFW_ApptoCD_LOGDIR/$CEWFW_ApptoCD_ServiceFlag
        chmod 755 $CEWFW_ApptoCD_LOGDIR/$CEWFW_ApptoCD_ServiceFlag
        print "$cewdt FILE WATCHER STOPPED . . ." >> $CEWFW_ApptoCD_LOGFILE
fi
