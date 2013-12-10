#!/bin/bash
BASEDIR="$( cd "$( dirname "$0" )" && pwd )"
CONFDIR=$BASEDIR/../conf/gentics

if [ -e /Node/ ] ; then
  echo "Found GCN installation. Using jre from that installation." 
  export JAVA_HOME=/Node/java
fi
export JAVA_OPTS="$JAVA_OPTS -Dcom.gentics.portalnode.confpath=$CONFDIR "