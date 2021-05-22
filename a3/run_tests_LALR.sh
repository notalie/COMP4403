# change the definition of PRE to set up new output directories
PRE=
CLASSPATH=../bin:../java-cup-11b.jar
#MAIN=pl0.PL0_RD
MAIN=pl0.PL0_LALR
export CLASSPATH

cd test-pgm
if [ ! -d ${PRE}errors ] ; then mkdir ${PRE}errors ; fi
if [ ! -d ${PRE}results ] ; then mkdir ${PRE}results ; fi
for i in test*.pl0
do
    cat $i
    echo '------------------------------------------'
    BASE=${i%.pl0}
    java ${MAIN} ${i} 2> "${PRE}errors/${BASE}.txt" | tee "${PRE}results/${BASE}.txt"
    cat "${PRE}errors/${BASE}.txt"
    echo '------------------------------------------'
done
