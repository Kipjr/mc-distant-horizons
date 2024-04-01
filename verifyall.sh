#!/bin/bash

# Usage: ./verifyall.sh [forge|fabric|whatever to put before ":classes"]

if [ -n "$1" ]; then
    prefix="$1:"
fi
z
clear
trap "echo; exit" INT

declare -a completed_builds
for version in $(ls ./versionProperties/); do
    version=${version%".properties"}
    
    result=""
    if ./gradlew "$prefix"classes -PmcVer=$version; then
        result+="\e[1;32m"
        echo -ne "\e[1;32m"
    else
        result+="\e[1;31m"
        echo -ne "\e[1;31m"
    fi
    result+=$version
    result+="\e[0m"
    
    echo "#"
    echo "# $version"
    echo "#"
    echo -e "\e[0m"
    
    completed_builds+=($result)
done

./gradlew clean
./gradlew classes

echo
echo "Build results:"
echo -e "${completed_builds[*]}"
