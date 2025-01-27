@ECHO OFF

:: When setting the memory below make sure to include the amount of ram letter. M = MB, G = GB. Don't use 1GB for example, it's 1G ::

:: This is 64-bit memory ::
set memsixtyfour=2G

:: This is 32-bit memory - maximum 1.2G ish::
set memthirtytwo=1G

:: The path to the Java to use. Wrap in double quotes ("C:\Path\To\Java\bin\java"). Use "java" to point to system default install.
set javapath="java"

:: Don't edit past this point ::

if $SYSTEM_os_arch==x86 (
    echo OS is 32
    set mem=%memthirtytwo%
) else (
    echo OS is 64
    set mem=%memsixtyfour%
)

%javapath% -version

echo Launching %%SERVERJAR%% with arguments '%*' and '%mem%' max memory

:: add nogui to the end of this line to disable the gui ::
%javapath% -Xmx%mem% -XX:MaxPermSize=256M %%ARGUMENTS%% -jar %%SERVERJAR%% %*
PAUSE

