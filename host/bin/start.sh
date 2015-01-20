#!/bin/bash

cpu=`uname -m`

java -Djava.library.path=os/Linux/${cpu} -cp lib/*:. org.area515.resinprinter.server.Main &