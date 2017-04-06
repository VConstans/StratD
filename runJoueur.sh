A=0

while [ $A -lt $1 ]
do
	java JoueurImpl localhost 1050 &
	A=`expr $A + 1`
done
