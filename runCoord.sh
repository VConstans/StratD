tnameserv -ORBInitialPort 1050 &
java -Xmx32m -Xms13m CoordinateurImpl localhost 1050 L A P $1 $2
pkill tnameserv
