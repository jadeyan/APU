Building the SyncML client
==========================
Note: Ensure the following software is installed and configured correctly:
    - Apache Ant version 1.7.1 or later.
    - Command-line SVN client (e.g. SlikSVN) available on the path.
    - Java Wireless Toolkit version 2.5.2 or later (optional).

1. To perform a development build, run the following command:
        ant
    
2. To perform a release build, run the following command:
        ant -Drevision=<RevisionNumber> -Dsvn.user=<SvnUser> [-Dsvn.password=<SvnPassword>] [-Djwt=<PathToJWT> [-Dpg=<PathToProguard>]]
   For example:
        ant "-Drevision=1" "-Dsvn.user=builder" "-Dsvn.password=password" "-Djwt=C:\Program Files\WTK2.5.2" "-Dpg=C:\Program Files\proguard"
   The SVN username/password are used to access the "http://gforge.cpth.ie" SVN server. If no SVN password 
   is provided, you must ensure that your command-line SVN client is already authenticated with the server.
   Note that the "-Djwt" option causes the source to be compiled against the Java Wireless Toolkit 
   (CLDC 1.0). This is just used to verify that the sources can be compiled in a J2ME environment.
   If the JWT is not installed, you can omit the "-Djwt" option to skip this step. In addition,
   the "-Dpg" option can be supplied to have the JWT classes processed (optimised, shrunk, etc) by ProGuard.

3. The following output will be created:
        .\cp-syncml-bin-java.tar.gz - package containing the SyncML client binaries and API javadocs.
        .\cp-syncml-src-java.tar.gz - package containing the SyncML client source.

        
Binary distribution
===================
The binary distribution is contained in the "cp-syncml-bin-java.tar.gz" file and contains the following
components:
        .\bin\cp-syncml.jar -          Java archive containing the SyncML client API.
        .\bin\cp-syncml-test.jar -     Java archive containing test applications.
        .\docs\cp-syncml-docs.tar.gz - Tarball containing Javadocs of the SyncML client API.
        .\lib\* -                      Any additional libraries needed to use the client API or test applications.

        
The test application
====================
A test application is provided which makes use of the SyncML client API to synchronise 
vCard contacts and files/folders with the Critical Path SyncML server. This test application
also provides some good example usage of the SyncML client API, particularly in the area of
how the required interfaces could be implemented.

To view the supported options of the test application, run the following command:
    .\SyncClientApp.bat

To use the application to sync contacts with the SyncML server, perform the following steps:
    - Create a directory where the vCard files should be placed (e.g. "C:\Temp\vcards").
    - Place some vCards in this directory and/or create contacts using the Messaging UI.
    - Run the following command:
        .\SyncClientApp.bat -dev-id desktop1 -h 10.128.10.158 -p 9080 -u user1@sync.com -w xxxxx -contact-dir C:\Temp\vcards

To use the application to sync content (files/folders) with the SyncML server, perform the following steps:
    - Create a base directory (e.g. "C:\Temp\files").
    - Create a "data" sub-directory where the files/folders should be placed (i.e. "C:\Temp\files\data").
    - Place some files in this directory and/or upload files using the Messaging UI.
    - Run the following command:
        ./SyncClientApp.bat -dev-id desktop1 -h 10.128.10.158 -p 9080 -u user1@sync.com -w xxxxx -content-dir C:\Temp\files     
