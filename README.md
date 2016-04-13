![Photonic3D](https://raw.githubusercontent.com/jmkao/Creation-Workshop-Host/master/host/resourcesnew/cwh/img/photonic3d-icon-sign-black.png)
======================

Photonic3D is a control server for DLP resin 3D printers with a web-based user interface. It has been designed to run well on a Raspberry Pi, but is cross-platform and the install distribution will also run on Linux, Windows, and MacOS.

The server is capable of printing a variety of formats, including:
 - `.cws` files created by Creation Workshop
 - STL files, which Photonic3D will slice incrementally
 - Zip of PNG images, which Photonic3D will scan recursively and print based on alphanumeric ordering (meaning both zero-padded and non-padded numbered PNG files will work correctly)
 - Single GIF, JPEG, or PNG image, which Photonic3D will extrude and print a 3D relief
 - Block of text, which Photonic3D will extrude using a system or user uploaded font


Raspberry Pi Usage
-------------------------------------------------------------------------------  
If you have a brand new dedicated Raspberry Pi, there is a flashable Jessie Lite based image that you can use to quickly get started. Take a look at the [area515/OctoPi repo on Github](https://github.com/area515/OctoPi) for the download link and installation instructions.

If you have an existing running Raspberry Pi with Raspbian, you can add Photonic3D to it with an easy bootstrap script that will download and install everything you need. This path is [documented in our wiki](https://github.com/area515/Creation-Workshop-Host/wiki/Raspberry-Pi-Manual-Setup-Instructions), and there is a [YouTube video that demonstrates it](https://www.youtube.com/watch?v=ng1Sj2ktWhU).


User Interface Examples
-------------------------------------------------------------------------------  
![image](https://cloud.githubusercontent.com/assets/2078819/14433486/58291f82-ffc3-11e5-8bc3-f7b8313dbc18.png)
![image](https://cloud.githubusercontent.com/assets/2078819/14433580/a984246c-ffc3-11e5-94ff-7214f6bd0635.png)


Use with Creation Workshop
-------------------------------------------------------------------------------  
Photonic3D can run `.cws` scene files saved from Creation Workshop which contain PNG files. When printed, these files will obey the GCode and imaging commands inside of the files instead of the slicing profiles configured within Photonic3D. This process is 
[depicted in this YouTube video](https://www.youtube.com/watch?v=J3HTCkxlKcw).


Feature List
-------------------------------------------------------------------------------  
1. Print STL files without performing pre-slice stage.
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
16. Automatic updates via online installs or manual updates through offline installs
17. [Script](https://github.com/area515/Creation-Workshop-Host/blob/master/host/bin/browseprinter.sh)(or a native Windows app)to find CWH based printers on the local network with zero network setup. (DLNA/UPNP support)
18. Capability of building printer configurations with simulated Serial ports and displays.
19. Two clicks and a password for Linux Wifi management to support a turnkey hardware solution.
20. Automatic projector model detection through serial (Plug and play for Acer & Viewsonic models).
21. 3d firmware printer port detection.
22. Hardware compatibility test suite.
23. Experiment and override printing options after a print is already in progress.
24. Take still pictures of the build at the click of a button.
25. Print material detection framework implemented with a computer vision inspection portal.
26. Supports Windows, OSX and Linux OSs. (Service support on Linux)
27. Supports dynamic direct slice-on-the-fly 3d printing of (.mazecube, .stl, .jpg/.png/.gif, .cws/.zip) files.

Linux Installation Quickstart
-------------------------------------------------------------------------------
If you are experienced with Linux, you can quickly install Photonic3D with the following commands:

Latest stable:
```
sudo wget https://github.com/area515/Creation-Workshop-Host/raw/master/host/bin/start.sh
sudo chmod 777 start.sh
sudo ./start.sh
```

Latest development:
```
sudo wget https://github.com/WesGilster/Creation-Workshop-Host/raw/master/host/bin/newstart.sh
sudo chmod 777 newstart.sh
sudo ./newstart.sh WesGilster
```

Photonic3D will be installed into `/opt/cwh` and an init script will be added to `/etc/init.d` to launch the server on boot.

Windows Installation Quickstart
------------------------------------------
* Download the latest stable or development version: 
 * [Stable repo Github Releases](https://github.com/area515/Creation-Workshop-Host/releases) or [Development repo Github Releases](https://github.com/WesGilster/Creation-Workshop-Host/releases)  
* Unzip the zip file into the directory of your choice.
* Double click on `start.bat`.

Client Tools
----------------------------------------------------------------------
There are also a number of tools designed to run on your client desktop that are bundled with the installation.

* Download the latest stable or development version: 
 * [Stable repo Github Releases](https://github.com/area515/Creation-Workshop-Host/releases) or [Development repo Github Releases](https://github.com/WesGilster/Creation-Workshop-Host/releases)  
* Unzip the zip file into the directory of your choice.

The *browseprinter* tool will help you find the Photonic3D server on your network, in case you have it installed a machine with a dynamic IP address and you don't know what what is.
* Linux: Run `sudo browseprinter.sh` at the command line
* Windows: Run `browseprinter.bat`
