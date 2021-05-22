D=`pwd`
export CLASSPATH=$D/java-cup-11b.jar:$D/jflex-full-1.7.0.jar:$CLASSPATH

echo "Running java-CUP"
cd src/parse
java -cp $CLASSPATH java_cup.Main -interface -locations \
	-parser CUPParser -symbols CUPToken \
	PL0.cup

echo "Running JFlex"
java -cp $CLASSPATH jflex.Main PL0.flex

cd ../..
echo "Compiling PL0 compiler"
javac -cp $CLASSPATH -g -d bin -sourcepath src src/pl0/PL0_LALR.java
