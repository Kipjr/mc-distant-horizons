#!/bin/sh

MC_VER=$1
BUILD_TASK=$2

if [ -z "$BUILD_TASK" ]
then
    BUILD_TASK="build"
fi

publish_version()
{
    if [[ "$MC_VER" == "all" || "$1" == "$MC_VER" ]]
    then
        docker run --rm --name="dh-build-$1" \
            -v "/$(PWD)://home/gradle/project" \
            -w "//home/gradle/project" \
            gradle:8.5-alpine \
            gradle $BUILD_TASK -PmcVer="$1" --no-daemon --gradle-user-home ".gradle-cache/"

        cp ./fabric/build/libs/*$1.jar ./buildAllJars/fabric/
        # cp ./forge/build/libs/*$1.jar ./buildAllJars/forge/
        # cp ./Merged/*.jar ./buildAllJars/merged/
    fi
}


if [ -z "$MC_VER" ]
then
    echo "Build target is undefined! [all] [1.20.4] [1.20.2] [1.20.1] [1.19.4] [1.19.2] [1.18.2] [1.17.1] [1.16.5]"
    exit 1
fi

mkdir -p buildAllJars/fabric
# mkdir -p buildAllJars/forge
# mkdir -p buildAllJars/merged

publish_version 1.20.4
publish_version 1.20.2
publish_version 1.20.1
publish_version 1.19.4
publish_version 1.19.2
publish_version 1.18.2
publish_version 1.17.1
publish_version 1.16.5
