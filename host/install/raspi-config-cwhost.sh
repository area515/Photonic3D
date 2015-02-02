#!/bin/sh
# Part of raspi-config http://github.com/asb/raspi-config
#
# See LICENSE file for copyright and license details

disable_boot_to_scratch() {
  if [ -e /etc/profile.d/boottoscratch.sh ]; then
    rm -f /etc/profile.d/boottoscratch.sh
    sed -i /etc/inittab \
      -e "s/^#\(.*\)#\s*BTS_TO_ENABLE\s*/\1/" \
      -e "/#\s*BTS_TO_DISABLE/d"
    telinit q
  fi
}

disable_raspi_config_at_boot() {
  if [ -e /etc/profile.d/raspi-config.sh ]; then
    rm -f /etc/profile.d/raspi-config.sh
    sed -i /etc/inittab \
      -e "s/^#\(.*\)#\s*RPICFG_TO_ENABLE\s*/\1/" \
      -e "/#\s*RPICFG_TO_DISABLE/d"
    telinit q
  fi
}

do_boot_behaviour() {
        if [ -e /etc/init.d/lightdm ]; then
          if id -u pi > /dev/null 2>&1; then
            update-rc.d lightdm enable 2
            sed /etc/lightdm/lightdm.conf -i -e "s/^#autologin-user=.*/autologin-user=pi/"
            disable_boot_to_scratch
            disable_raspi_config_at_boot
            do_remove_lxpanel
            do_blank_desktop
            do_remove_trashcan
            do_autostart_cwhost
          else
            echo "The pi user has been removed, can't set up boot to desktop"
          fi
        else
          echo "Do sudo apt-get install lightdm to allow configuration of boot to desktop"
          return 1
        fi
}

do_remove_lxpanel() {
  #prevent lxpanel from loading
  sudo sed -i 's/@lxpanel --profile LXDE-pi/#@lxpanel --profile LXDE-pi'/ /etc/xdg/lxsession/LXDE-pi/autostart
}

do_blank_desktop() {
  #change desktop background to color and set color to black
  sudo sed -i 's/wallpaper_mode=center/wallpaper_mode=color'/ /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-0.conf
  sudo sed -i 's/desktop_bg=#c4c2c2/desktop_bg=#000000'/ /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-0.conf
}

do_remove_trashcan() {
  #remove the trashcan icon on the desktop
  sudo sed -i 's/show_trash=1/show_trash=0'/ /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-0.conf
}

do_autostart_cwhost() {
  #setup autostart for cwhost for when after startx and gui auto-login
  #must be after starx and gui auto-login to avoid awt-headless exception
  mkdir -p /home/pi/.config/autostart/cwhost.desktop
  echo "[Desktop Entry]" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Encoding=UTF-8" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Type=Application" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Name=cwhost" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Comment=" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Exec=/home/pi/start.sh" >> /home/pi/.config/autostart/cwhost.desktop
  echo "StartupNotify=false" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Terminal=false" >> /home/pi/.config/autostart/cwhost.desktop
  echo "Hidden=false" >> /home/pi/.config/autostart/cwhost.desktop

}

# Make sure only root can run our script
if [ $(id -u) -ne 0 ]; then
   printf "Script must be run as root. Try 'sudo raspi-config-cwhost'\n"
   exit 1
else
   echo "running as root"
   do_boot_behaviour
fi
