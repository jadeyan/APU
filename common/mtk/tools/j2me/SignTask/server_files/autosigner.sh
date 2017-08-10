#!/bin/sh

# This script checks for new files that need to be signed, and signs them.
# It pulls in remote files for signing as well.

# If you supply a JAD, it must look like:   program.jad.1253639761686
# You must supply a JAR, it must look like: program.jar.1253639761686
# If a JAD+JAR are supplied, the JAD will be signed.
# If a JAR only is supplied, the JAR will be signed.
# 1253639761686 is the "cookie".
# The cookie must be unique for each signing request, but all related files in the same request must use the same cookie.
# You must supply a "ready file", it must look like: memova.ready.7.1253639761686
# Here 7 is the cert options, with bit 0 set for verisign, bit 1 for thawte, and bit 2 for globalsign.
# The "ready file" must be the LAST FILE UPLOADED for the signing request.

clear

#ARGS: signing_inbox signing_outbox
function processDirectory {

	# Process each "ready file" and associated JAD/JAR on local system
	for I in `ls ./$1/*.ready.* 2>/dev/null`
	do

		COOKIE=`echo $I | cut -f5 -d'.'`
		CERT_OPTS=`echo $I | cut -f4 -d'.'`

		JAR=`ls ./$1/*.jar.$COOKIE | head -n 1`
		JAD=`ls ./$1/*.jad.$COOKIE | head -n 1`

		if test -z $JAR
		then # no JAR present, skip this cookie
			echo no jar found for cookie $COOKIE
			rm -f $I
		fi

		if test -z $JAD
		then #no JAD present, sign the JAR
			echo Signing $JAR
			echo Cert options: $CERT_OPTS

			#signjar.sh will output something.jar.[cookie].signed
			echo "$PASSWORD" | ~/signjar.sh "$JAR" "$CERT_OPTS"

			#remove old unsigned JAR
			rm -f "$JAR"

			mv "$JAR.signed" "$JAR"
			cp "$JAR" ~/$2/
			cp $I ~/$2/

			rm -f "$JAR"
			rm -f $I
		else
			echo Signing $JAD
			echo Cert options: $CERT_OPTS

			#sign.sh will output something.jad.signed
			echo "$PASSWORD" | ~/signjad.sh "$JAR" "$JAD" "$CERT_OPTS"

			#remove old unsigned jad
			rm -f "$JAD"

			mv "$JAD.signed" "$JAD"
			cp "$JAD" ~/$2/
			cp $I ~/$2/

			rm -f "$JAR"
			rm -f "$JAD"
			rm -f $I
		fi

	done
}

#read the password to the keystore
read -s -p "Password: " PASSWORD

clear

echo
echo Started AutoSigner

./cleaner.sh &

#main loop
while true
do

	#process dropped directly onto this box
	processDirectory signing_inbox signing_outbox

	#copy files from i05 if they exist
	~/process_remote_files.sh

	#process the files copied from i05
	processDirectory remote_signing_inbox remote_signing_outbox

	usleep 250000 #sleep 0.25 seconds
done
