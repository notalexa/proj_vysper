#!/bin/bash

for f in bin/* ; do
echo "Patch `basename $f`"
(cd $f ; zip -r ../../vysper-0.7/lib/`basename $f`.jar  *) > /dev/null
done
echo "Copy binaries"
cp -r src/bin vysper-0.7

echo "Create vysper-jitsi-0.7.zip"
(cd vysper-0.7 ; zip -r ../vysper-jitsi-0.7.zip *) > /dev/null

echo "Patched."

