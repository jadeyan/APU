Building the client
===================
Note: Ensure the following software is installed and configured correctly:
    - Apache Ant version 1.7.1 or later.
    - Android SDK (http://developer.android.com/sdk/index.html) with SDK Platform Android 2.2 API 8
    - Java Wireless Toolkit version 2.5.2 or later.
    - Command-line SVN client (e.g. SlikSVN) available on the path.

1. Edit the "build.properties" file with the correct paths to the Android SDK and the Java Wireless Toolkit.

2. Edit the "<AndroidSDK>\platforms\android-8\tools\dx.bat" file and uncomment the "REM set javaOpts=-Xmx256M" line
   to avoid out-of-memory exceptions when building.

3. Go to the build (trunk) directory

4. To build the application for the target device:
        ant -Dsvn.user=<SvnUser> [-Dsvn.password=<SvnPassword>] release
   Note that the SVN username/password are used to access the "http://gforge.cpth.ie" SVN server. If 
   no SVN password is provided, you must ensure that your command-line SVN client is already authenticated 
   with the server.
   For example:
        ant -Dsvn.user=builder -Dsvn.password=password release

5. The following output will be created:
        - bin/CP_Phone_Backup-Android-Cupcake.apk - the application package.
          

Building the client (pre-bundled)
=================================
Pre-bundling means that all the resources are inserted into the package at build time, instead of being downloaded one 
at a time from the server.

1. Edit the "assets/features.properties" file and ensure all properties have the correct values (i.e. the values that the 
   application will actually be deployed with).
        
2. Perform the build as described above, but specified an additional build property of "bundle.enable=true".
   For example:
        ant -Dsvn.user=builder -Dsvn.password=password -Dbundle.enable=true release
   If your are using the "build.bat" file to build, "bundle.enable=true" is set by default.


Installing the distribution
===========================
If the application has been built pre-bundled, the application can be installed directly by transferring it to the phone 
via Bluetooth or a USB/serial cable. Otherwise, it should be deployed using CP Download Manager.


Running the distribution (emulator)
===================================
Once the application has been built as described above, you can run in in the Android emulator as follows:

1. Ensure that the Android SDK tools (i.e. "<AndroidSDK>/tools") are available on the PATH.

2. Using the Android SDK Manager, create a virtual device with the appropriate capabilities (if one doesn't already 
   exist). Select the desired virtual device and select "Start" and "Launch". Wait for the emulator to full start (which
   may take a long time).

3. To view output logging, run "ddms.bat" from the command line.

4. Install and run the application by running "install_run.bat" from the command line.
 