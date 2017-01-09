#!/usr/bin/env bash
readonly name=sevenorless
readonly basedir=/opt/$name
readonly logfile=/var/log/$name.log
. $basedir/$name.conf

nohup java -jar $basedir/$name.jar >$logfile 2>&1 &
echo $! > $1

