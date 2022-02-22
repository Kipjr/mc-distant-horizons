# <img src="https://gitlab.com/jeseibel/distant-horizons-core/-/raw/main/_logo%20files/LOD%20logo%20flat%20-%20with%20boarder.png" width="32"> Distant Horizons

> A mod that adds a Level of Detail System to Minecraft


# What is Distant Horizons?

This mod adds a Level Of Detail (LOD) system to Minecraft.\
This implementation renders simplified chunks outside the normal render distance\
allowing for an increased view distance without harming performance.

Or in other words: this mod lets you see farther without turning your game into a slide show.\
If you want to see a quick demo, check out a video covering the mod here:

<a href="https://www.youtube.com/watch?v=H2tnvEVbO1c" target="_blank">![Minecraft Level Of Detail (LOD) mod - Alpha 1.5](https://i.ytimg.com/vi_webp/H2tnvEVbO1c/mqdefault.webp)</a>

Architectury version: 3.4-SNAPSHOT\
Forge version: 37.1.0\
Fabric version: 0.13.2\
Fabric API version: 0.46.1+1.17

Modmenu version: 2.0.14

Notes:\
This version has been confirmed to work in Eclipse and Retail Minecraft.\
(Retail running forge version 1.17.1-37.1.0 and fabric version 1.17.1-0.12.6)


## Source Code Installation

See the Forge Documentation online for more detailed instructions:\
http://mcforge.readthedocs.io/en/latest/gettingstarted/

### Prerequisites

* A Java Development Kit (JDK) for Java 16 (recommended) or newer. Visit https://www.oracle.com/java/technologies/downloads/ for installers.
* Git or someway to clone git projects. Visit https://git-scm.com/ for installers.
* (Not required) Any Java IDE, for example Intellij IDEA and Eclipse. You may also use any other code editors, such as Visual Studio Code. (Optional)
  It's better to use IntelliJ IDEA since Eclipse is not supported by Architectury, but it still works.

**If using IntelliJ:**
1. open IDEA and import the build.gradle
2. refresh the Gradle project in IDEA if required

**If using Ecplise:**
Not supported...

Side note: invalidate caches and restart if required

## Compiling

**Using GUI**
1. Download the zip of the project and extract it
2. Download the core from https://gitlab.com/jeseibel/distant-horizons-core and extract into a folder called `core`
3. Open a command line in the project folder
4. Run the command: `./gradlew assemble`
5. Then run command: `./gradlew mergeJars`
6. The compiled jar file will be in the folder `Merged`


**If in terminal:**
1. `git clone -b 1.17.1 --recurse-submodules https://gitlab.com/jeseibel/minecraft-lod-mod.git`
2. `cd minecraft-lod-mod`
3. `./gradlew assemble`
4. `./gradlew mergeJars`
6. The compiled jar file will be in the folder `Merged`


## Other commands

`./gradlew --refresh-dependencies` to refresh local dependencies.

`./gradlew clean` to reset everything (this does not affect your code) and then start the process again.


## Note to self

The Minecraft source code is NOT added to your workspace in an editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

Source code uses Mojang mappings.

## Useful commands

Build only Fabric: `./gradlew fabric:assemble` or `./gradlew fabric:build`\
Build only Forge: `./gradlew fabric:assemble` or `./gradlew forge:build`\
Run the Fabric client (for debugging): `./gradlew fabric:runClient`\
Run the Forge client (for debugging): `./gradlew forge:runClient`

## Open Source Acknowledgements

XZ for Java (data compression)\
https://tukaani.org/xz/java.html

DHJarMerger (To merge multiple mod versions into one jar)\
https://github.com/Ran-helo/DHJarMerger
