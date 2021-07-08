# Welcome:
Hello and Welcome to the Cyan Modding Community!
This guide will help you set up the mod project template you have just downloaded.

We assume you have read the license document, if you want to add another license alongside the modding license,
you may rename it CYAN-MODDING-LICENSE.txt (not anything else) and add the license document you wish to use.



# The basics:
Lets get started, regular mods (modular modifications) in cyan's case, cannot make changes to the game code, that is what coremods are for. Cyan mods are compatible with multiple modloaders.

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
Building the CMF file is not too difficult, but it can take a long as it needs to remap the mod code for EVERY platform.

If you want to build the mod, run the following command depending on your operating system:


### For Linux:

```bash
./gradlew build
```

### For Windows PowerShell:

```
./gradlew build
```

The build outputs in `build/cmf`
