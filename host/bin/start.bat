@if (@CodeSection == @Batch) @then

@echo off
setlocal

rem // Get latest release file from github
%CD%\curl\bin\curl -outf https://api.github.com/repos/Draakie2017/photonic3d-dev/releases/latest

rem // Set location of downloaded release file (json)
set "latestRelease=utf"

rem // Find current repoversion from build.number
for /F "delims=" %%a in ('findstr /I "repo.version" build.number') do set "repoVersion=%%a"

rem // Cut off repo.version !WARNING! when this changes it will break the code!
set repoVersion=%repoVersion:~13%

rem // Jscript  call to parse JSON
for /f "delims=" %%I in ('cscript /nologo /e:JScript "%~f0" "%latestRelease%"') do set "%%~I"


echo Network TAG: %tag_name%
echo Local TAG : %repoVersion%

rem // Determing if updating is needed

IF "%tag_name%"=="%repoVersion%" (
	echo No update needed
	start "" http://localhost:9091
	java -Dlog4j.configurationFile=log4j2.properties -Djava.library.path=os/win64 -cp lib/*;. org.area515.resinprinter.server.Main > log.out 2> log.err
) ELSE (
	echo Download URL = %browser_download_url%
	%CD%\curl\bin\curl -L %browser_download_url% > Update.zip
	unzip -o Update
	start "" http://localhost:9091
	java -Dlog4j.configurationFile=log4j2.properties -Djava.library.path=os/win64 -cp lib/*;. org.area515.resinprinter.server.Main > log.out 2> log.err
)

goto :EOF

@end // end batch / begin JScript chimera

var fso = WSH.CreateObject('scripting.filesystemobject'),
    latestRelease = fso.OpenTextFile(WSH.Arguments(0), 1);

eval('obj = ' + latestRelease.ReadAll());
latestRelease.Close();

function walk(tree) {
    for (var i in tree) {
        if (typeof tree[i] === 'object') walk(tree[i]);
        else WSH.Echo(i + '=' + tree[i]);
    }
}

walk(obj);
