#!/bin/bash
# Gentics Demo Portal Install Script
#

NOWP=`/bin/date +%Y%m%d_%H-%M-%S`
SCRIPT="`readlink -f $0`" 
BASEDIR="`dirname "$SCRIPT"`"
INSTALLDIR=/opt/demoportal

if [ "$BASEDIR" == "/opt" ] ; then
  echo "Please extract the installer in /tmp or /var/tmp"
fi

CMSHOSTNAME=$1
PORTALHOSTNAME=$2
LICENSEKEY=$3


if [ "$1" == "help" ] ; then
  echo "Usage $0 [CMSHOSTNAME] [PORTALHOSTNAME] [LICENSEKEY]"
  exit 1
fi

# Generic error check and abort method
function handleError() {
  if [ "$1" != "0" ]; then
    echo -e "\n\nERROR: $2"
    echo -e "Aborting with errorcode $1 \n\n"
    exit 10
  fi
}

function readSettings() {
   if [ "$CMSHOSTNAME" == "" ] ; then 
     echo -e "\n * Please enter the backend hostname (eg. cms.gentics.com):"
     read CMSHOSTNAME
   fi

   if [ "$PORTALHOSTNAME" == "" ] ; then
     echo -e "\n * Please enter your frontend hostname (eg. portal.gentics.com):"
     read PORTALHOSTNAME
   fi

   if [ "$LICENSEKEY" == "" ] ; then
     echo -e "\n * Please enter your Gentics Portal.Node license key:"
     read LICENSEKEY
   fi

}

function startFrontend() {
  echo -e "\n * Starting frontend server"
    /etc/init.d/demoportal start
    handleError $? "Could not start the frontend server"
  echo "Done."
}

function installFiles() {

  if [ -d $INSTALLDIR ] ; then
    echo -e "\n * Found existing frontend installation - moving away to /opt/tomcat.old-$NOWP"
      mv $INSTALLDIR $INSTALLDIR-$NOWP
      handleError $? "Could not move old installation out of harms way."
    echo "Done."
  fi

  if [ -d $INSTALLDIR ] ; then 
    unlink $INSTALLDIR
    handleError $? "Could not unlink $INSTALLDIR"
  fi

  echo -e "\n *  Moving installation into place"
    mv demoportal $INSTALLDIR
    handleError $? "Could not move installation in place"
  echo "Done."
  
  echo -e "\n * Setting permission bits"
    chmod +x $INSTALLDIR/bin/*.sh
    handleError $? "Could not change permission for tomcat bin files."
  echo "Done."
  
  echo -e "\n * Extracting webapp"
    unzip $INSTALLDIR/webapps/GCN5_Portal.war -d $INSTALLDIR/webapps/GCN5_Portal
    handleError $? "Could not extract gcn5 portal zipfile"
  echo "Done."
}
  
function stopFrontend() {
  if [ -d $INSTALLDIR ] ; then 
    echo -e "\n * Found existing frontend installation - Stopping existing installation"
      $INSTALLDIR/bin/catalina.sh stop
      sleep 2
    echo "Done."
  fi
}

function enableAutostart() {
  echo -e "\n * Enabling autostart - this currently only works for debian squeeze and newer"
    STARTSCRIPT=/etc/init.d/demoportal
    if [ -L $STARTSCRIPT ] ; then
      echo -e "\n * Removing symlink $STARTSCRIPT
        unlink $STARTSCRIPT
        handleError $? "Could not remove file $STARTSCRIPT
      echo "Done."
    fi
    ln -s $INSTALLDIR/bin/tomcat.sh $STARTSCRIPT
    handleError $? "Could not create symlink"
    insserv demoportal
    handleError $? "Could not setup autostart"
  echo "Done."
}

function storeLicensekey() {
  echo -e "\n * Setting license key"
    LICENSEKEYFILE=$INSTALLDIR/conf/gentics/license.key
    echo $LICENSEKEY > $LICENSEKEYFILE
    handleError $? "Error while saving the licensekey to file $LICENSEKEYFILE"
    #chown node:node $LICENSEKEYFILE
    #handleError $? "Error while changing permissions for the licensekey at $LICENSEKEYFILE"
  echo "Done."
}

function configureFrontend() {
  sed -i "s/GCNBACKENDBASEPATH/http:\/\/$CMSHOSTNAME\//" $INSTALLDIR/conf/gentics/default.portal.xml
  handleError $? "Could not set cms hostname within default.portal.xml"
  sed -i "s/GCNAUTHURL/http:\/\/$CMSHOSTNAME\/.Node\//" $INSTALLDIR/conf/gentics/default.portal.xml
  handleError $? "Could not set cms hostname within default.portal.xml"
  sed -i "s/GCNBACKENDBASEPATH/http:\/\/$CMSHOSTNAME\//" $INSTALLDIR/webapps/GCN5_Portal/WEB-INF/web.xml
  handleError $? "Could not set cms hostname within web.xml"
}

function configureBackend() {
 if [ -e /Node/ ] ; then
   echo -e "\n * Configuring backend (GCN)"
     if [ ! -e /Node/apache/conf/sites-enabled/020-portal.conf ] ; then
       ln -s /Node/apache/conf/sites-available/portal.conf /Node/apache/conf/sites-enabled/020-portal.conf
       sed -i "s/ServerName.*/ServerName $gcn-testing.office/" /Node/apache/conf/sites-enabled/020-portal.conf
       handleError $? "Could not enable portal site"
       /Node/bin/nodectl restart apache
     fi
     handleError $? "Could not restart apache"
     sed -i "s/\($GCN5_PORTALURL=\"\).*\(\";\)/\1http:\/\/$PORTALHOSTNAME\/\2/" /Node/etc/node.conf
     handleError $? "Could not set portal url within node.conf" 
   echo "Done." 
 fi
}

readSettings
stopFrontend
installFiles
configureFrontend
storeLicensekey
enableAutostart
startFrontend
configureBackend
