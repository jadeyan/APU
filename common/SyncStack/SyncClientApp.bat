@echo off

set SYNC_CLIENT_APP_JARS=".\bin\classes;.\bin\cp-syncml.jar;.\bin\cp-syncml-test.jar;.\lib\commons-codec-1.3.jar;.\lib\commons-httpclient-2.0.jar;.\lib\commons-lang-2.3.jar;.\lib\commons-logging-1.1.jar"

REM uncomment the options you want to apply
set JVM_OPTIONS="-verbose:gc"
REM set JVM_OPTIONS="%JVM_OPTIONS% -Xms2m -Xmx2m"
REM set JVM_OPTIONS="%JVM_OPTIONS% -agentlib:hprof=heap=sites,depth=15"
REM set JVM_OPTIONS="%JVM_OPTIONS% -agentlib:hprof=cpu=times,depth=15"
REM set JVM_OPTIONS="%JVM_OPTIONS% -agentlib:hprof=file=C:\Temp\hprof.dump,format=b"

"%JAVA_HOME%\bin\java" %JVM_OPTIONS% -classpath "%SYNC_CLIENT_APP_JARS%" "net.cp.syncml.client.test.SyncClientApp" %*

@echo on