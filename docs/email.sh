#!/usr/bin/env bash
readonly name=sevenorless
readonly basedir=/opt/$name
readonly logfile=/var/log/$name-email.log
. $basedir/$name.conf

java -cp $basedir/$name.jar $name.emailer >$logfile 2>&1

