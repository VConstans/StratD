A=0

while [ $A -lt $1 ]
do
	java -Xmx32m -Xms16m ProducteurImpl localhost 1050 &
	A=`expr $A + 1`
done
