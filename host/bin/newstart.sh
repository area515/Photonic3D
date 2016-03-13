#!/bin/bash

cpu=`uname -m`

if [ -z "$1" ]; then
	repo="area515/Creation-Workshop-Host"
else
	if [[ $1 =~ .*Creation-Workshop-Host.* ]]; then
		repo=$1
	else
		repo="$1/Creation-Workshop-Host"
	fi;
fi;

if [ "$2" == "TestKit" ]; then 
	downloadPrefix=cwh$2-
	installDirectory=/opt/cwh$2
else
	downloadPrefix=cwh-
	installDirectory=/opt/cwh
fi;

#Its pretty hard to keep these updated, let me know when they get too old
if [ "${cpu}" = "armv6l" -o "${cpu}" = "armv7l" ]; then 
	javaURL="http://download.oracle.com/otn-pub/java/jdk/8u73-b02/jdk-8u73-linux-arm32-vfp-hflt.tar.gz"
elif [ "${cpu}" = "i686" ]; then 
	javaURL="http://download.oracle.com/otn-pub/java/jdk/8u73-b02/jdk-8u73-linux-i586.tar.gz"
elif [ "${cpu}" = "x86_64" ]; then 
	javaURL="http://download.oracle.com/otn-pub/java/jdk/8u73-b02/jdk-8u73-linux-x64.tar.gz"
fi

if [ ! -f "/usr/lib/jni/librxtxSerial.so" ]; then
	echo Installing RxTx
	apt-get install --yes --force-yes librxtx-java
fi

#This application will always need to have the display set to the following
export DISPLAY=:0.0
xinitProcess=`ps -ef | grep grep -v | grep xinit`
if [ -z "${xinitProcess}" ]; then
    echo No X server running, starting and configuring one
    startx &
    xhost +x
fi

#Copy the zip file from the current directory into the cwh directory for offline install
mkdir -p ${installDirectory}
mv ${downloadPrefix}.*.zip ${installDirectory}

#install java if version is too old
javaInstalled=`which java`
if [ "$javaInstalled" = "" ]; then
	javaMajorVersion=0
	javaMinorVersion=0
else
	javaMajorVersion=`java -version 2>&1 | grep "java version" | awk -F[\".] '{print "0"$2}'`
	javaMinorVersion=`java -version 2>&1 | grep "java version" | awk -F[\".] '{print "0"$3}'`
fi

if [ "$javaMinorVersion" -lt 8 -a "$javaMajorVersion" -le 1 ]; then
	downloadJavaFile=`echo ${javaURL} | awk -F/ '{print $(NF)}'`
	echo Either Java is not installed, or an incorrect version of Java is installed. Installing from this URL: ${javaURL}
	mkdir -p /usr/lib/jvm
	cd /usr/lib/jvm
	rm ${downloadJavaFile}
	wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "${javaURL}"
	
	firstSnapshot=`ls -1`
	echo Unzipping and installing Java now
	tar xzf ${downloadJavaFile}
	secondSnapshot=`ls -1`
	javaInstallFile=`echo "$firstSnapshot"$'\n'"$secondSnapshot" | sort | uniq -u`
	
	if [ -z "${javaInstallFile}" ]; then
		echo "A new version of Java is available, please update this script with the proper download URLS from: http://www.oracle.com/technetwork/java/javase/downloads/index.html"
		exit
	fi
	
	ln -sf /usr/lib/jvm/${javaInstallFile}/bin/java /usr/bin/java
	ln -sf /usr/lib/jvm/${javaInstallFile}/bin/javac /usr/bin/javac
	ln -sf /usr/lib/jvm/${javaInstallFile}/bin/keytool /usr/bin/keytool		
	rm ${downloadJavaFile}
fi

#Determine if a new install is available
cd ${installDirectory}
LOCAL_TAG=$(grep repo.version build.number | cut -d = -f 2)
NETWORK_TAG=$(curl -s https://api.github.com/repos/${repo}/releases/latest | grep 'tag_name' | cut -d\" -f4)


if [ -f ${downloadPrefix}.*.zip ]; then
	OFFLINE_FILE=$(ls ${downloadPrefix}.*.zip)
	echo Performing offline install of ${OFFLINE_FILE}
	
	mv ${OFFLINE_FILE} ~
	rm -r ${installDirectory}
	mkdir -p ${installDirectory}
	cd ${installDirectory}
	mv ~/${OFFLINE_FILE} .
	unzip ${OFFLINE_FILE}
	chmod 777 *.sh
	rm ${OFFLINE_FILE}
elif [ "${NETWORK_TAG}" != "${LOCAL_TAG}" -o "$2" == "force" ]; then
	echo Installing latest version of ${downloadPrefix}: ${NETWORK_TAG}
	
	rm -r ${installDirectory}
	mkdir -p ${installDirectory}
	cd ${installDirectory}
	wget $(curl -s https://api.github.com/repos/${repo}/releases/latest | grep 'browser_' | cut -d\" -f4 | grep ${downloadPrefix})
	unzip ${downloadPrefix}*.zip
	chmod 777 *.sh
	rm ${downloadPrefix}*.zip
else
	echo No install required
	
fi

echo Turning off screen saver and power saving
xset s off         # don't activate screensaver
xset -dpms         # disable DPMS (Energy Star) features
xset s noblank     # don't blank the video device 

if [ ! -f "/etc/init.d/cwhservice" ]; then
	echo Installing CWH as a service
	cp ${installDirectory}/cwhservice /etc/init.d/
	chmod 777 /etc/init.d/cwhservice
	update-rc.d cwhservice defaults
fi

echo Determinging if one time install has occurred
performedOneTimeInstall=$(grep performedOneTimeInstall ~/3dPrinters/config.properties | awk -F= '{print $2}')
if [ -f "oneTimeInstall.sh" -a [${performedOneTimeInstall} != "true"] ]; then
	./oneTimeInstall.sh
fi

if [ -f "eachStart.sh" ]; then
	./eachStart.sh
fi

if [ "$2" == "debug" ]; then
	pkill -9 -f "org.area515.resinprinter.server.Main"
	echo "Starting printer host server($2)"
	java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Dlog4j.configurationFile=debuglog4j2.properties -Djava.library.path=/usr/lib/jni:os/Linux/${cpu} -cp lib/*:. org.area515.resinprinter.server.Main > log.out 2> log.err &
elif [ "$2" == "TestKit" ]; then
	pkill -9 -f "org.area515.resinprinter.test.HardwareCompatibilityTestSuite"
	echo Starting test kit
	java -Dlog4j.configurationFile=testlog4j2.properties -Djava.library.path=/usr/lib/jni:os/Linux/${cpu} -cp lib/*:. org.junit.runner.JUnitCore org.area515.resinprinter.test.HardwareCompatibilityTestSuite &
else
	pkill -9 -f "org.area515.resinprinter.server.Main"
	echo Starting printer host server
	java -Dlog4j.configurationFile=log4j2.properties -Djava.library.path=/usr/lib/jni:os/Linux/${cpu} -cp lib/*:. org.area515.resinprinter.server.Main > log.out 2> log.err &
fi
