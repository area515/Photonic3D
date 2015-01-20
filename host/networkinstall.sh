#!/bin/bash

rm -r cwh
mkdir cwh
cd cwh
wget https://github.com/area515/Creation-Workshop-Host/raw/master/host/cwh-0.51.zip
unzip cwh-0.51.zip
chmod 777 start.sh
chmod 777 debug.sh
chmod 777 browseprinter.sh
rm cwh-0.51.zip
