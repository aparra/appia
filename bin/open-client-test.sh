#!/bin/bash

CP=classes:config/jgcs/open_group:.
for j in `find dist -name *.jar`; do
    CP=$CP:$j;
done

echo "Classpath: $CP";


java -cp $CP net.sf.appia.demo.jgcs.opengroup.ClientOpenGroupTest $@
