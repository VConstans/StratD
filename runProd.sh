A=0

while [ $A -lt $1 ]
do
	java ProducteurImpl localhost 1050 &
	A=`expr $A + 1`
done
