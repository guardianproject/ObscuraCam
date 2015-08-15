#!/bin/bash

if ! type -P android &> /dev/null; then
    echo "Error: 'android' utility is not in your path."
    echo "  Did you forget to setup the SDK?"
    exit 1
fi

projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' res/values/strings.xml`

android update project --path . --name $projectname --subprojects
