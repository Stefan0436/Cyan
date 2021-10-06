# Welcome:
Hello and Welcome to the Cyan Modding Community!
This guide will help you set up the coremod project template you have just downloaded.

We assume you have read the license document, if you want to add another license alongside the modding license,
you may rename it CYAN-MODDING-LICENSE.txt (not anything else) and add the license document you wish to use.



# The basics:
Lets get started, first let us explain a few basics about coremods:
Compiled coremods are made up of TWO files instead of one, the CCMF (Cyan Core Mod File) and a CTC (Cyan Trust Container)

The CTC file is usually downloaded from a remote server, but it needs to be present if you haven't set that up manually.
If you haven't set it up, and you do want a protected mod, please read the SETUP-TRUST.md document first.

Coremods are advanced mods that can make changes to the game code, with coremods, you have access to FLUID.
FLUID is Cyan's Runtime Modification Engine, normal mods don't have access to it.

If you are not planning to make changes to game code, you should download the regular ModKit.
If you are making a coremod, please keep non-modification code in a separate mod file. You can specify dependencies
in the mod manifest using the 'dependency' statement, see the cmf block in the buildfile for more info.


# Setting up the project:
To set up the project, you only need to run the following command(s) to get started:

For Linux:

```bash
chmod +x gradlew
./gradlew createEclipse
```

For Windows PowerShell:

```
./gradlew createEclipse
```

After running the commands, you can import the project in eclipse (as a Gradle project)

# Building your mod
Building the CCMF file is not too difficult, but it can take a long as it needs to remap the mod code for EVERY platform.

If you want to build the mod, run the following command depending on your operating system:


### For Linux:

```bash
./gradlew build
```

### For Windows PowerShell:

```
./gradlew build
```

The build outputs in `build/ccmf`
Please note that you CANNOT run the mod without it's trust file. The trust container can be found in the `build/ctcs` folder, please make sure you have the newest version of it or the modloader will complain about invalid trust.
