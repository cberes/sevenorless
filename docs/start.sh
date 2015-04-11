NAME=sevenorless
BASEDIR=/opt/$NAME
LOGFILE=/var/log/$NAME.log
export PORT=8081

nohup java -Dsite.config=$BASEDIR/$NAME.conf -jar $BASEDIR/$NAME.jar >$LOGFILE 2>&1 &
echo $! > $1
