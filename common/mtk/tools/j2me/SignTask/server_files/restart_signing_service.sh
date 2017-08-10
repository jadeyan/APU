#!/bin/sh

clear
echo Signing Service

if test "signer" != `whoami`
then
echo wrong user. this script must be run as user signer
exit
fi

echo Please enter keystore password
read -s -p "Password: " PASSWORD
echo
killall -s 9 autosigner.sh
killall -s 9 cleaner.sh
rm nohup.out 2>/dev/null
echo "$PASSWORD" | nohup ~/autosigner.sh >/dev/null &

echo Signing Service started, it is safe to exit this session

