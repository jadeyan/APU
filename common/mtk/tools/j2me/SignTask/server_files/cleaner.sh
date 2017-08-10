#!/bin/sh

clear

function cleaner {

for I in `ls ./$1/*.ready.* 2>/dev/null`
do

COOKIE=`echo $I | cut -f5 -d'.'`

NOW=`date +%s`
OLD=`stat -c %Z $I`
AGE=`expr $NOW - $OLD`

if test $AGE -ge 120
then
echo deleting ./$1/\*.$COOKIE age: $AGE seconds
rm -f ./$1/*.$COOKIE 2>/dev/null
fi

done
}

while true
do

cleaner signing_outbox
cleaner remote_signing_outbox

usleep 300000 #sleep 0.3 seconds
done
