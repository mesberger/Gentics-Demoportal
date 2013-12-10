#!/bin/sh
# Tomcat Startup Script

### BEGIN INIT INFO
# Provides: tomcat
# Required-Start: $network $local_fs $remote_fs $syslog $node
# Required-Stop:  $network $local_fs $remote_fs $syslog
# Default-Start: 2 3 4 5
# Default-Stop: 1
# Short-Description: Gentics Portal.Node Demo Portal Server
### END INIT INFO



CATALINA_HOME=/opt/demoportal; export CATALINA_HOME
#JAVA_HOME=/Node/java/; export JAVA_HOME
TOMCAT_OWNER=root; export TOMCAT_OWNER

start() {
        echo -n "Starting Tomcat:  "
        su $TOMCAT_OWNER -c $CATALINA_HOME/bin/startup.sh
        sleep 2
}
stop() {
        echo -n "Stopping Tomcat: "
        su $TOMCAT_OWNER -c $CATALINA_HOME/bin/shutdown.sh
}

# See how we were called.
case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  restart)
        stop
        start
        ;;
  *)
        echo $"Usage: tomcat {start|stop|restart}"
        exit
esac

