#!/bin/bash
			
rm -r cwh
mkdir cwh
cd cwh
wget https://github.com/WesGilster/Creation-Workshop-Host/raw/master/host/cwh-0.17.zip
unzip cwh-0.17.zip
chmod 777 cwh-0.17.zip
rm cwh-0.17.zip