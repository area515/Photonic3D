#!/bin/bash

cpu=`uname -m`

java  -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Djava.library.path=os/Linux/${cpu} -cp lib/*:. org.area515.resinprinter.server.Main &