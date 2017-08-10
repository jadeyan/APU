@echo OFF

@REM
@REM Batch file allowing integration with the Medusa automated nightly build environment. 
@REM

set BASE_DIR=%CD%\..\..
setlocal enabledelayedexpansion


@REM determine whether we are doing a trunk, branch or tag build
svn info > info.txt
find "URL: " "info.txt" > output.txt
for /f "eol=- tokens=6 delims=/" %%i in (output.txt) do @SET BUILD_TYPE=%%i
for /f "eol=- tokens=7 delims=/" %%i in (output.txt) do @SET BUILD_NAME=%%i
del output.txt
del info.txt

if %BUILD_TYPE%==trunk (goto SETUP_TRUNK) else (goto SETUP_NON_TRUNK)


@REM determine the version number for trunk builds
:SETUP_TRUNK
set /a counter=0
for /f "tokens=1 delims=r\ " %%i in ('svn log --non-interactive --no-auth-cache http://gforge.cpth.ie/svn/androidsync/Android/trunk') do (
   if !counter! == 2 (goto SETUP_TRUNK_DONE)
   set REVISION=%%i
   set /a counter+=1
   )
:SETUP_TRUNK_DONE
set VERSION=9.9.999.%REVISION%
set VERSION_1=9
set VERSION_2=9
set VERSION_3=999
goto BUILD_PBC


@REM determine the version number for non-trunk builds
:SETUP_NON_TRUNK
for /f "tokens=1 delims=_-" %%x in ("%BUILD_NAME%") do set NAME_1=%%x
for /f "tokens=2 delims=_-" %%x in ("%BUILD_NAME%") do set VERSION_1=%%x
for /f "tokens=3 delims=_-" %%x in ("%BUILD_NAME%") do set VERSION_2=%%x
for /f "tokens=4 delims=_-" %%x in ("%BUILD_NAME%") do set VERSION_3=%%x
for /f "tokens=5 delims=_-" %%x in ("%BUILD_NAME%") do set VERSION_4=%%x

@REM determine whether we are doing a branch or tag build
if %BUILD_TYPE%==tags (goto SETUP_TAG) else (goto SETUP_BRANCH)


@REM determine the version number for branch builds
:SETUP_BRANCH
set /a counter=0
for /f "tokens=1 delims=r\ " %%i in ('svn log --non-interactive --no-auth-cache http://gforge.cpth.ie/svn/androidsync/Android/branches/%BUILD_NAME%') do (
   if !counter! == 2 goto SETUP_BRANCH_DONE
   set REVISION=%%i
   set /a counter+=1
   )
:SETUP_BRANCH_DONE
if "%VERSION_4%"=="" (set VERSION=%VERSION_1%.%VERSION_2%.999.%REVISION%) else (set VERSION=%VERSION_1%.%VERSION_2%.%VERSION_3%.99.%REVISION%)
goto BUILD_PBC


@REM determine the version number for tag builds
:SETUP_TAG
if "%VERSION_4%"=="" (set VERSION=%VERSION_1%.%VERSION_2%.%VERSION_3%) else (set VERSION=%VERSION_1%.%VERSION_2%.%VERSION_3%.%VERSION_4%)
goto BUILD_PBC


@REM do the actual build
:BUILD_PBC

echo Building PBC %VERSION% for Android...
set COMPONENT=PBC-ANDROID
set PBC_DIR=%BASE_DIR%\%COMPONENT%-%VERSION%
set SOURCE_DIR=%PBC_DIR%\%COMPONENT%-%VERSION%-source
set ESCROW_FILE=%PBC_DIR%\%COMPONENT%-%VERSION%-escrow.zip
set OUTPUT_FILE=%PBC_DIR%\%COMPONENT%-%VERSION%-win.zip
set LOG_FILE=%PBC_DIR%\%COMPONENT%-%VERSION%-win-build.log

@REM Setup the build settings - note that we explicitly override the SDK locations to be correct for the "mobile-build" machine
set ANT_OPTIONS="-Dsdk-folder=C:\Program Files\Android\android-sdk" "-Dwtk.home=C:\WTK2.5.2" "-Dbundle.enable=true"

if EXIST %PBC_DIR% rd /Q /S %PBC_DIR%
md %PBC_DIR%
if not %ERRORLEVEL%==0 goto ERROR_MAKE_DIR

cd %PBC_DIR% 1>>%LOG_FILE% 2>&1

echo Checking out code... >> %LOG_FILE%
echo INFORMATION: Checking out code
if %BUILD_TYPE%==trunk (svn co --non-interactive --no-auth-cache -r%REVISION% http://gforge.cpth.ie/svn/androidsync/Android/trunk %COMPONENT%-%VERSION%-source) 1>>%LOG_FILE% 2>&1
if %BUILD_TYPE%==branches (svn co --non-interactive --no-auth-cache -r%REVISION% http://gforge.cpth.ie/svn/androidsync/Android/branches/%BUILD_NAME% %COMPONENT%-%VERSION%-source) 1>>%LOG_FILE% 2>&1
if %BUILD_TYPE%==tags (svn co --non-interactive --no-auth-cache http://gforge.cpth.ie/svn/androidsync/Android/tags/%BUILD_NAME% %COMPONENT%-%VERSION%-source) 1>>%LOG_FILE% 2>&1
if %ERRORLEVEL%==1 goto ERROR_CHECKOUT

@REM Override the PATH setup by Cygwin as it interferes with the build tools.
@REM This PATH should be exactly the same as the PATH seen in the standard Windows cmd 
@REM shell on the "mobile-build.eu.cp.net" machine. If a different build machine is being 
@REM used, you will need to edit this PATH appropriately.
@REM
set PATH=C:\Program Files\Common Files\Symbian\Tools;C:\Program Files\Java\jdk1.6.0_07\bin;C:\Program Files\CSL Arm Toolchain\bin;C:\Perl\bin\;C:\WINDOWS\system32;C:\WINDOWS;C:\WINDOWS\System32\Wbem;C:\Program Files\Support Tools\;C:\Program Files\SlikSvn\bin\;C:\Program Files\apache-ant-1.7.1\bin

@REM Explicitly set the JAVA_HOME variable.
@REM
set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_07

@REM Grab additional memory as the Android SDK seems to need it 
@REM
set ANT_OPTS=-Xmx500M

echo INFORMATION: Creating escrow
zip -r %ESCROW_FILE% %COMPONENT%-%VERSION%-source 1>>%LOG_FILE% 2>&1

cd %SOURCE_DIR% 1>>%LOG_FILE% 2>&1
IF %ERRORLEVEL% NEQ 0 GOTO ERROR_BUILD

echo INFORMATION: Building PBC client (ant release)
call ant %ANT_OPTIONS% release 1>>%LOG_FILE% 2>&1
IF %ERRORLEVEL% NEQ 0 GOTO ERROR_BUILD
copy %SOURCE_DIR%\bin\Memova.apk %SOURCE_DIR%\CP_Phone_Backup-%VERSION%-Memova.apk 1>>%LOG_FILE% 2>&1
IF %ERRORLEVEL% NEQ 0 GOTO ERROR_BUILD

echo INFORMATION: Building PBC client (ant debug)
call ant %ANT_OPTIONS% debug 1>>%LOG_FILE% 2>&1
IF %ERRORLEVEL% NEQ 0 GOTO ERROR_BUILD
copy %SOURCE_DIR%\bin\Memova.apk %SOURCE_DIR%\CP_Phone_Backup-%VERSION%-Memova-Debug.apk 1>>%LOG_FILE% 2>&1
IF %ERRORLEVEL% NEQ 0 GOTO ERROR_BUILD

cd %SOURCE_DIR% 1>>%LOG_FILE% 2>&1
echo INFORMATION: Creating output bundle
zip -r %OUTPUT_FILE% CP_Phone_Backup-* -x CP_Phone_Backup-*-Src.tar.gz 1>>%LOG_FILE% 2>&1

echo BUILD SUCCESSFUL
echo BUILD SUCCESSFUL >> %LOG_FILE%

goto DONE


:ERROR_MAKE_DIR
echo Can't make main directory
echo Can't make main directory >> %LOG_FILE%
echo BUILD FAILED >> %LOG_FILE%
goto DONE

:ERROR_CHECKOUT
echo Can't checkout %COMPONENT%
echo Can't checkout %COMPONENT% >> %LOG_FILE%
echo BUILD FAILED >> %LOG_FILE%
goto DONE

:ERROR_BUILD
echo Can't build %COMPONENT%
echo Can't build %COMPONENT% >> %LOG_FILE%
echo BUILD FAILED >> %LOG_FILE%
goto DONE


:DONE
cd %BASE_DIR%
