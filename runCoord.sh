tnameserv -ORBInitialPort 1050 &
java -Xmx32m -Xms13m CoordinateurImpl localhost 1050
pkill tnameserv
