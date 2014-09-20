#!/bin/bash
#version=$(grep build.number build.version | cut -d = -s -f 2)
javadoc -d dist/javadoc -sourcepath src org.pcj
cd dist
rm PCJ-javadoc.zip
zip -r -9 PCJ-javadoc.zip javadoc
