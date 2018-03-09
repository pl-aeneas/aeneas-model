#!/bin/sh

JRI_HOME=/home/acanino/Src/rJava/jri

#R_HOME=/usr/local/lib/R

R_SHARE_DIR=/home/acanino/Research/languages/R-3.4.3/share
export R_SHARE_DIR
R_INCLUDE_DIR=/home/acanino/Research/languages/R-3.4.3/include
export R_INCLUDE_DIR
R_DOC_DIR=/home/acanino/Research/languages/R-3.4.3/doc
export R_DOC_DIR

JRI_LD_PATH=${R_HOME}/lib:/usr/local/bin:/home/acanino/Src/jdk1.8.0_60/jre/lib/amd64/server:/home/acanino/Src/jdk1.8.0_60/jre/lib/amd64:/home/acanino/Src/jdk1.8.0_60/jre/../lib/amd64:/home/acanino/Research/jRAPL/libpfm/lib:/home/acanino/Src/jdk1.8.0_60/jre/lib/amd64/server/:/usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib
if test -z "$LD_LIBRARY_PATH"; then
  LD_LIBRARY_PATH=$JRI_LD_PATH
else
  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JRI_LD_PATH
fi
JAVA=/home/acanino/Src/jdk1.8.0_60/jre/bin/java

: ${CLASSPATH=.:build}

export LD_LIBRARY_PATH

if [ -z "$1" ]; then
    echo ""
    echo " Usage: run <class> [...]"
    echo ""
    echo " For example: ./run rtest"
    echo " Set CLASSPATH variable if other than .:examples is desired"
    echo ""
else
    ${JAVA} -Djava.library.path=$JRI_HOME:/home/acanino/Src/jdk1.8.0_60/jre/lib/amd64/server:/home/acanino/Src/jdk1.8.0_60/jre/lib/amd64:/home/acanino/Src/jdk1.8.0_60/jre/../lib/amd64:/home/acanino/Research/jRAPL/libpfm/lib:/home/acanino/Src/jdk1.8.0_60/jre/lib/amd64/server/:/usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib -cp ${CLASSPATH}:${JRI_HOME}/src/JRI.jar:${JRI_HOME}/JRI.jar:./lib/stoke.jar:./lib/commons-cli-1.4/commons-cli-1.4.jar $*
fi
