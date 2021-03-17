# Building CYAN
Cyan is not too hard to build on linux, but it is for windows.<br />
Either way, the dependencies needed to build are the same.<br />
Please make sure your environment has been set up.

# Dependencies
1. Java JDK 9 or greater (recommended to use JDK 15)
2. Gradle 6.8 (or use the supplied wrapper)

# Linux build script dependencies
1. jq 1.6+
2. GNU Coreutils (latest)
3. GNU Bash 5.1.4+
4. CURL 7.75.0+

# Building on windows
If you want to build on windows, you will need the command prompt.<br />
Open it by pressing: WIN+R, type cmd, press enter and you have a command line.

# Building on linux, before doing any of the commands below
You will need to mark the bash script as executable, run: `chmod 755 buildlocal.sh gradlew`

# Building without any other modloader
For linux, the command would be `./buildlocal.sh`<br />
For windows, this is a bit tricky:
1. `gradlew.bat -PcurrentXML -PoverrideCyanLibraryURL="" -PresetLibSourceCache processResources`
2. `gradlew.bat -PcurrentXML -PoverrideCyanLibraryURL="" build gameData serverJar`<br />
3. `gradlew.bat -PcurrentXML -PresetLibSourceCache processResources`<br />

Both commands build for the version specified in config.gradle.

# Building for a specific minecraft version
Linux: `./buildlocal.sh --version insert-version-here` (replace 'insert-version-here' with the game version)<br />
For windows, its even trickier (you will be prompted for the version):

```batch
SET /P "gameversion=Game version: "
gradlew.bat -PoverrideGameVersion="%gameversion%" -PcurrentXML -PoverrideCyanLibraryURL="" -PresetLibSourceCache processResources
gradlew.bat -PoverrideGameVersion="%gameversion%" -PcurrentXML -PoverrideCyanLibraryURL="" build gameData serverJar`
gradlew.bat -PoverrideGameVersion="%gameversion%" -PcurrentXML -PresetLibSourceCache processResources
```

# Building for a specific modloader
First you will need to know which modloaders are supported, we currently support: Forge, Fabric and Paper.<br />
Modloader IDs: Forge=forge, Fabric=fabric, Paper=paper<br />
<br />
<br />
Building for linux is easy, you can combine the information with the game version argument if you want to.<br />
Command: `./buildlocal.sh --modloader insert-modloader-id-here`<br />
<br />
Building for a specific modloader version (DO NOT ATTEMPT THIS WITH PAPER): <br />
`./buildlocal.sh --modloader insert-modloader-id-here --modloader-version insert-version-here`<br />
<br />

## What about windows?
Windows is REALLY tricky for this, if you want to go down this rabbit hole, visit the following page.<br />
[Building for windows targeting a modloader and version (RABBIT HOLE)](BUILDING-WINDOWS-TARGETING-MODLOADER.md)
