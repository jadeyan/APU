#!/usr/bin/env bash

#
# Main build script allowing integration with the Medusa automated nightly build environment. 
#

UNAME=`uname`
if [ "$UNAME" != "CYGWIN_NT-5.1" ]; then
	echo "This script only supports windows (CYGWIN_NT-5.1)"
	exit 1
fi

if [ "$COMPUTERNAME" != "MOBILE-BUILD" ]; then
    echo "This script can only be run on mobile-build.eu.cp.net"
    exit 1
fi

dir=`dirname $0`
cd $dir
export PATH="/cygdrive/c/WINDOWS/system32/:/usr/local/bin:/usr/bin:$PATH"
export LD_LIBRARY_PATH=/usr/local/lib:$LD_LIBRARY_PATH

echo "Invoking windows build.bat"
./build.bat
if [ $? -ne 0 ]; then
    echo "build.bat reported failure"
    exit 1
fi

exit 0