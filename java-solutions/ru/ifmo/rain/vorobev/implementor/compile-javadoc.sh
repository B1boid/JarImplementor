#!/usr/bin/env bash

cd ../../../../../../
SOLUTION_PATH=$PWD
cd ../
ROOT=$PWD

RU_PATH=ru/ifmo/rain/vorobev/implementor
INFO_PATH=info/kgeorgiy/java/advanced/implementor
FILES_PATH=${ROOT}/java-advanced-2020/modules/info.kgeorgiy.java.advanced.implementor/${INFO_PATH}

javadoc -link "https://docs.oracle.com/en/java/javase/11/docs/api/" -d ${SOLUTION_PATH}/_javadoc -private -version \
 -author ${FILES_PATH}/Impler.java ${FILES_PATH}/JarImpler.java ${FILES_PATH}/ImplerException.java  \
 ${SOLUTION_PATH}/java-solutions/${RU_PATH}/JarImplementor.java