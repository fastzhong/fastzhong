lxcewtsgv082:/prodlib/CEWMSTH/batch/FileWatcher $ cat set_batch_env.sh
baseDir=/prodlib/CEWMSTH
export BASE_DIR=$baseDir
export CEWFW_ApptoApp_DIR=$BASE_DIR/batch/FileWatcher
export CEWFW_ApptoApp_LOGDIR=$BASE_DIR/logs
export CEWFW_ApptoApp_ServiceFlag=cewth_fw_app2app_dummy.dat
export today=`date '+%Y%m%d'`
export CEWFW_ApptoApp_LOGFILE=$CEWFW_ApptoApp_LOGDIR/"cewth_app2app_"$today.log
export CONTROLM=/home/ctmadm/ctm


export CEWFW_ApptoCD_DIR=$BASE_DIR/batch/FileWatcher
export CEWFW_ApptoCD_LOGDIR=$BASE_DIR/logs
export CEWFW_ApptoCD_ServiceFlag=cewth_fw_app2cd_dummy.dat
export CEWFW_ApptoCD_LOGFILE=$CEWFW_ApptoCD_LOGDIR/"cewstopfw_app2cd_"$today.loglxcewtsgv082:/prodlib/CEWMSTH/batch/FileWatcher 
