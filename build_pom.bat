
REM Version information
SET "NUM_VER=15.1.48"
SET "SED_PATH=C:\Program Files (x86)\GnuWin32\bin"

"%SED_PATH%\sed.exe" -e "s/NUMBER_VERSION/%NUM_VER%/g" -e "s/JDK_VERSION/%JDK_VER%/g" pom.template > pom.xml || exit /b
