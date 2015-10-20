Creation-Workshop-Host
======================

What does CWH look like?
-------------------------------------------------------------------------------  
Old Version Looks like [this](https://github.com/area515/Creation-Workshop-Host/blob/master/host/cwh.png).  
The New Version Looks like [this](https://github.com/area515/Creation-Workshop-Host/blob/master/host/cwhNew.png).

Where are the instructions for installing on the Raspberry Pi?  
-------------------------------------------------------------------------------  
[Here](https://github.com/area515/Creation-Workshop-Host/wiki/Raspberry-Pi-Manual-Setup-Instructions).

Where is a video of how to use CWH with CWS and Zip files?  
-------------------------------------------------------------------------------  
[Here](https://www.youtube.com/watch?v=J3HTCkxlKcw).

Where is a video of how to setup CWH from scratch on the Raspberry Pi?
-------------------------------------------------------------------------------  
[Here](https://www.youtube.com/watch?v=ng1Sj2ktWhU).

How do I use this new version of the GUI you've started?  
-------------------------------------------------------------------------------  
Change the following line:  
```
hostGUI=resources
```  
in this file:  
```
[LocationWhereCWHIsInstalled]/config.properties
```  
to this:  
```
hostGUI=resourcesnew
```  
Once the new GUI has all of the functionality of the current version, we'll use this version automatically.

What features does CWH have?
-------------------------------------------------------------------------------  
1. Print STL files.
2. Load STL files directly from thingiverse or the internet.
3. Uses common xml Creation Workshop configuration files.
4. Custom printer mask overlays.
5. TLS encryption with Basic authentication.
6. Use of [FreeMarker](http://freemarker.org/) templating in configuration files.
7. Restful developer API for printer management.
8. Video Recording and playback of build.
9. Print Zip/CWS files exported from Creation Workshop.
10. Execution of custom gcode from GUI.
11. Managment of multiple printers with a single print host.
12. Plugin based notification framework.
13. Sophisticated javascript calculators that compute gradients, exposure time, lift speed and distance.
14. Notification of Printer events through webSockets.
15. Simple printer setup for Zip/CWS based printing.
16. Automatic updates.
17. [Script](https://github.com/area515/Creation-Workshop-Host/blob/master/host/bin/browseprinter.sh) to find CWH based printers on the local network with zero network setup.
18. Capability of building printer configurations with simulated Serial ports and displays.

Do you want to install the latest stable build?
-------------------------------------------------------------------------------
```
sudo wget https://github.com/area515/Creation-Workshop-Host/raw/master/host/bin/start.sh
sudo chmod 777 start.sh
sudo ./start.sh
```

Do you want to install the latest unstable daily development build?
-------------------------------------------------------------------------------
```
sudo wget https://github.com/WesGilster/Creation-Workshop-Host/raw/master/host/bin/start.sh
sudo chmod 777 start.sh
sudo ./start.sh WesGilster
```

Do you want to install under Windows?
------------------------------------------
* Download the latest version from: 
* https://github.com/area515/Creation-Workshop-Host/blob/master/host/cwh-X.XX.zip
 or
* https://github.com/WesGilster/Creation-Workshop-Host/blob/master/host/cwh-X.XX.zip
* Unzip the zip file into the directory of your choice.
* Double click on start.bat.

Do you want to use your web browser to automatically navigate to the running printer host without knowing anything about how your network is setup?
----------------------------------------------------------------------
* Download the latest version from:
https://github.com/area515/Creation-Workshop-Host/blob/master/host/cwhClient-X.XX.zip
 or
https://github.com/WesGilster/Creation-Workshop-Host/blob/master/host/cwhClient-X.XX.zip
* Unzip the zip file into the directory of your choice.
* If you are in Linux run this:
````````
	sudo browseprinter.sh
````````
If you are in windows double click this:
````````
	browseprinter.bat
````````
