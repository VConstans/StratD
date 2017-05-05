A=0

while [ $A -lt $1 ]
do
	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 petrole 10 &
	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 or 100 &
#	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 eau 100 &
	A=`expr $A + 1`
done
