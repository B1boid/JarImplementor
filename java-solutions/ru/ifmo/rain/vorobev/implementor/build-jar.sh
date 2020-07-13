#!/bin/bash

cd ../../../../../../
SOLUTION_PATH=$PWD
cd ../
ROOT=$PWD

RU_PATH=ru/ifmo/rain/vorobev/implementor
INFO_PATH=info/kgeorgiy/java/advanced/implementor
FILES_PATH=${ROOT}/java-advanced-2020/modules/info.kgeorgiy.java.advanced.implementor/${INFO_PATH}
OUT_PATH=${SOLUTION_PATH}/_build/production

mkdir -p ${OUT_PATH}/${RU_PATH}
mkdir -p ${OUT_PATH}/${INFO_PATH}

cp ${SOLUTION_PATH}/java-solutions/${RU_PATH}/JarImplementor.java ${SOLUTION_PATH}/_build/production/${RU_PATH}
cp -R ${FILES_PATH}/* ${SOLUTION_PATH}/_build/production/${INFO_PATH}

cd ${SOLUTION_PATH}/_build

echo -e "Manifest-Version: 1.0\nMain-Class: ru.ifmo.rain.vorobev.implementor.JarImplementor" > production/MANIFEST.MF
javac production/${RU_PATH}/JarImplementor.java --source-path production
jar cfm production/_implementor.jar production/MANIFEST.MF -C production ru -C production info
