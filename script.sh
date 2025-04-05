#!/bin/ksh
#============================================
# Error Code Description
#============================================
# 0 - Success
# 1 - Config file not specified
# 2 - Config file not exist
# 3 - FTP Failed
# 4 - Data folder not exist
# 5 - Data folder is empty
# 6 - Data file read permission denied
# 7 - Data file integrity check failed
# 8 - Files transfer to target server failed
#============================================
# End of Error Code Description
#============================================
. ./cewenv.sh

# get parameter

configfile=$1

# check parameter

if [ -z "$configfile" ]
then
   print `date +"%Y/%m/%d %H:%M:%S"` "Config file not specified" \
         >> $FTPLOGFILE
   exit 1
fi

if [ ! -a "$CEWFTPDIR/$configfile" -o ! -r "$CEWFTPDIR/$configfile" ]
then
   print `date +"%Y/%m/%d %H:%M:%S"` $configfile "Config file not exists" \
         >> $FTPLOGFILE
   exit 2
fi

serveripfile=`grep    -y "TServer"    $CEWFTPDIR/$configfile | cut -d" " -f3`

echo "server ip file name = $serveripfile"

if [ -z "serveripfile" ]
then
   print `date +"%Y/%m/%d %H:%M:%S"` "Server Ip file not specified" \
         >> $FTPLOGFILE
   exit 1
fi

if [ ! -a "$CEWFTPDIR/$serveripfile" -o ! -r "$CEWFTPDIR/$serveripfile" ]
then
   print `date +"%Y/%m/%d %H:%M:%S"` $serveripfile "Server Ip file not exists" \
         >> $FTPLOGFILE
   exit 2
fi

# retrieve info from config file

rtnfile=`grep   -y "Returnfile" $CEWFTPDIR/$configfile | cut -d" " -f3`
stsfile=`grep   -y "Statfile"   $CEWFTPDIR/$configfile | cut -d" " -f3`
ipaddr=`grep    -y "TServer"    $CEWFTPDIR/$serveripfile | cut -d" " -f3`
userid=`grep    -y "TUserid"    $CEWFTPDIR/$configfile | cut -d" " -f3`
subdir=`grep    -y "TSubdir"    $CEWFTPDIR/$configfile | cut -d" " -f3`
fromdir=`grep    -y "TFromdir"   $CEWFTPDIR/$configfile | cut -d" " -f3`
folderpath=`grep -y "FileFolder"  $CEWFTPDIR/$configfile | cut -d" " -f3`
backupfld=`grep -y "TBackupFolder" $CEWFTPDIR/$configfile | cut -d" " -f3`

# output the ftp statement to a temporary file

print "cd $subdir" > $LOGDIR/tmp$$

# Check for existance for folder

if [ ! -d "${folderpath}" ]
then
   print `date +"%Y/%m/%d %H:%M:%S"` $folderpath "Folder does not exists" >> $FTPLOGFILE
   exit 4
fi

filecount=`ls ${folderpath} | wc -l`
if [ "$filecount" -le 0 ]
then
   print `date +"%Y/%m/%d %H:%M:%S"` $folderpath "No files to transfer" >> $FTPLOGFILE
   exit 5
fi

#Check for each file in the folder path and transfer file
for i in `ls ${folderpath}`
do
   if [ ! -r $folderpath/$i ]
   then
    print `date +"%Y/%m/%d %H:%M:%S"` $folderpath/$i "Data file read permission denied" >> $FTPLOGFILE
    continue;
   fi

   var=`ls -ltr $folderpath/$i | awk -F" " '{print $5}'`
   sleep 3
   var1=`ls -ltr $folderpath/$i | awk -F" " '{print $5}'`

   if [ $var -ne $var1 ]
   then
    print `date +"%Y/%m/%d %H:%M:%S"` $folderpath/$i "Data file integrity failed. Skipping FTP for this file" >> $FTPLOGFILE
    echo "SKIPPING"
    continue;
   fi

   print "lcd $fromdir" >> $LOGDIR/tmp$$
   print "put $i" >> $LOGDIR/tmp$$
   print "chmod 775 $i" >> $LOGDIR/tmp$$
done

print "bye" >> $LOGDIR/tmp$$

cat $LOGDIR/tmp$$ >> $LOGDIR/$stsfile

# issue the sftp command and pipe the output to status file

/usr/bin/sftp -b $LOGDIR/tmp$$ $userid@$ipaddr > $LOGDIR/$stsfile 2>&1

# check the ftp transfer status
cat $LOGDIR/$stsfile | grep -y "Uploading" | wc -l | read okcount
cat $LOGDIR/$stsfile | grep -y "Permission Denied" | wc -l | read notokcount
okcount=`expr $okcount - $notokcount`


if [ "$okcount" -ne "$filecount" ]
then
   print 8 > $LOGDIR/$rtnfile
else
   print 0 > $LOGDIR/$rtnfile
fi

#Performing clean up/ backup of the files transferred.
for i in `cat $LOGDIR/tmp$$ | grep ^put | awk -F" " '{print $2}'`
do
mv ${folderpath}/$i ${backupfld}
done
rm -f $LOGDIR/tmp$$
exit 0
