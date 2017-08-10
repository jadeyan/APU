#!/bin/sh

JAR="$1"
JAD="$2"

#Certificate options. Should be a number
#bit 0 = Verisign
#bit 1 = Thawte
#bit 2 = GlobalSign

OPT="$3"

if test -z $JAR
then
	echo usage: $0 JAR JAD [CERT_OPTS]
	exit
fi

if test -z $JAD
then
	echo usage: $0 JAR JAD [CERT_OPTS]
        exit
fi



read -s -p "Password: " PASSWORD

echo

java -jar JadTool.jar -addjarsig -keystore CriticalPathKeyStore -alias criticalpathcodesigningkey -storepass $PASSWORD  -keypass $PASSWORD -jarfile $JAR -inputjad $JAD -outputjad $JAD.signed

CHAIN=1

#bit 0 set - VeriSign
if [ "$OPT" = "1" ] || [ "$OPT" = "3" ] || [ "$OPT" = "5" ] || [ "$OPT" = "7" ]
then
java -jar JadTool.jar -addcert -alias criticalpathcodesigningkey -keystore CriticalPathKeyStore -chainnum $CHAIN -inputjad $JAD.signed -outputjad $JAD.signed
CHAIN=`expr $CHAIN + 1`
fi
#bit 1 set - Thawte
if [ "$OPT" = "2" ] || [ "$OPT" = "3" ] || [ "$OPT" = "6" ] || [ "$OPT" = "7" ]
then
java -jar JadTool.jar -addcert -alias criticalpathcodesigningkey_thawte -keystore CriticalPathKeyStore -chainnum $CHAIN -inputjad $JAD.signed -outputjad $JAD.signed
CHAIN=`expr $CHAIN + 1`
fi

#bit 2 set - GlobalSign
if [ "$OPT" = "4" ] || [ "$OPT" = "5" ] || [ "$OPT" = "6" ] || [ "$OPT" = "7" ]
then
java -jar JadTool.jar -addcert -alias criticalpathcodesigningkey_globalsign -keystore CriticalPathKeyStore -chainnum $CHAIN -inputjad $JAD.signed -outputjad $JAD.signed
fi

echo Done

