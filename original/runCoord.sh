tnameserv -ORBInitialPort 1050 &
sleep 2
java -Xmx32m -Xms13m CoordinateurImpl localhost 1050 R A P $1 $2
pkill java
pkill tnameserv
