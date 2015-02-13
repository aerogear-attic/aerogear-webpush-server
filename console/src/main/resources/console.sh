#!/bin/bash
for i in `ls lib`
do
    TLS_EXT="$i"
done

CMD="java -Xbootclasspath/p:./lib/$TLS_EXT -jar webpush-console-dist.jar"
echo "Executing: "
echo $CMD
$CMD
