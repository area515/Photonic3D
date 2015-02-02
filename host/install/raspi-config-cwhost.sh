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
          else
            echo "The pi user has been removed, can't set up boot to desktop"
          fi
        else
          echo "Do sudo apt-get install lightdm to allow configuration of boot to desktop"
          return 1
        fi
}


# Make sure only root can run our script
if [ $(id -u) -ne 0 ]; then
   printf "Script must be run as root. Try 'sudo raspi-config-cwhost'\n"
   exit 1
else
   echo "running as root"
   do_boot_behaviour
fi
