#!/bin/sh

cd "$(dirname "$0")"

JAVA_OPTS="-Xmx256M -XX:MaxRAM=256M -DuseRFCMimeType=false -DmaxRunningTime=45"

CLASSPATH="/opt/brouter/brouter.jar"
SEGMENTSPATH="/data/maps/segments4"
PROFILESPATH="/data/maps/profiles2"
CUSTOMPROFILESPATH="/opt/brouter/customprofiles"

java $JAVA_OPTS -cp $CLASSPATH btools.server.RouteServer "$SEGMENTSPATH" "$PROFILESPATH" "$CUSTOMPROFILESPATH" 17777 1 $BINDADDRESS
