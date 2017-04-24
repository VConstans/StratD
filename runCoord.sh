tnameserv -ORBInitialPort 1050 &
java -Xmx32m -Xms13m CoordinateurImpl localhost 1050 R $1 $1
pkill tnameserv
pkill java
