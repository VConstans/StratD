A=0

while [ $A -lt $1 ]
do
	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 petrole 50 &
#	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 or 50 &
#	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 eau 100 &
	A=`expr $A + 1`
done
