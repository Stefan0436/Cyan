# NOTICE
The github repository mirrors to https://aerialworks.ddns.net/ASF/Cyan.git, you can fork Cyan on github, pull requests will be merged manually.

# Cyan Mod Loader -- The Compatible Modloader
Cyan is an upcoming modloader geared towards compatibility,<br/>
Currently, we are writing compatibility for Forge, Fabric and Paper.

# Project setup
Cyan needs itself to build, for this, you build LiteCyan, a bundle of libraries needed to run Cornflower.<br/>
On Linux, run: `chmod +x gradlew ; ./gradlew -c settings.lite.gradle installLiteLibs`<br/>
On Windows, run: `.\gradlew.bat -c settings.lite.gradle installLiteLibs`

After that, you can use the following commands to set up the development environment:<br/>
On Linux, run: `./gradlew eclipse jar createEclipseLaunches`<br/>
On Windows, run: `.\gradlew.bat eclipse jar createEclipseLaunches`<br/>

# Compiling
Use `./buildlocal.sh` on linux to quickly build the project, argument `--version` can be used to specify game version,
please note that CYAN is 1.16+, we cannot support < 1.15 because of mapping issues. Please note that 1.15 is is not compatbile with alpha 13, alpha 14 will re-implement 1.15 support. See [BUILDING](BUILDING.md) for more details on how to compile.

# AerialWorks Maven Server
Because we are writing a modloader, we have set up a maven server, here is the url:<br />
AerialWorks Maven Server: https://aerialworks.ddns.net/maven

# Licensing
| Project               | License             | Project description or acronym meaning                                                        | Copyright Notice                                       | Build Status                   |
| :-------------------: | :-----------------: | :-------------------------------------------------------------------------------------------: | :----------------------------------------------------: | :----------------------------: |
| CyanWrapper           | GPL v2              | CyanWrapper - Launch wrappers for the client and server, starts the modloader                 | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| CyanLoader            | GPL v2              | CyanLoader - Main mod loading system                                                          | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| CyanUtil              | LGPL v3             | Cyan Utilities Package - Contains the modloader API, dynamic class loader and more.           | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| FLUID                 | GPL v2              | Fluid Runtime Modification Engine - Allows for making changes to code without re-compiling.   | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| CyanComponents        | LGPL v3             | CyanComponents - Cyan component system - mostly automatic loading if properly implemented.    | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| CyanCore              | GPL v2              | CyanCore - Implementation of the CyanComponents system - also bootstraps the game and loader. | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| CCFG                  | LGPL v3             | Cyan Serializing Configuration API - Main configuration system for Cyan and its mods.         | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| MTK                   | LGPL v3             | Minecraft Toolkit - A set of libraries to help with modding, can interface with FLUID.        | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| MixinSupport          | LGPL v3             | Mixin Support for CYAN - Soon-to-become-library-mod                                           | Copyright (c) 2021<br/>AerialWorks Software Foundation | INACTIVE                       |
| Cornflower            | GPL v2              | Cornflower Gradle Plugin - Upcoming gradle plugin for building CYAN mods, contains APIs.      | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| ClassTrust API        | LGPL v3             | Cyan Software Protection Library for CTC                                                      | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| CyanWeb               | GPL v2              | Website for the Cyan mod loader                                                               | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |
| Cyan API (ModKit)     | LGPL v3             | Cyan Modding API - Version-shared api for creating mods.                                      | Copyright (c) 2021<br/>AerialWorks Software Foundation | Alpha Stage                    |

# Version Compatibility
| Cyan version | 1.12.X | 1.13.X | 1.14.X | 1.15.X | 1.16.X | 1.17.X |
| :----------: | :----: | :----: | :----: | :----: | :----: | :----: |
| 1.0.0.A12    | No     | No     | No     | Yes    | Yes    | No     |
| 1.0.0.A13    | No     | No     | No     | No     | Yes    | No     |
| 1.0.0.A14*   | No     | No     | No     | Yes    | Yes    | Yes    |

If the '*' is present, the version is not ready for release

# Modloader compatibility
| Cyan version | Forge 1.15  | Fabric 1.15 | Paper 1.15   | Forge 1.16 | Fabric 1.16 | Paper 1.16 | Forge 1.17   | Fabric 1.17 | Paper 1.17  |
| :----------: | :---------: | :---------: | :----------: | :--------: | :---------: | :--------: | :----------: | :---------: | :---------: |
| 1.0.0.A12    | 31.2.50     | 0.11.3      | Unsupported  | 36.1.13    | 0.11.3      | 634        | Unsupported  | Unsupported | Unsupported |
| 1.0.0.A13    | Unsupported | Unsupported | Unsupported  | 36.1.23    | 0.11.3      | 703        | Unsupported  | Unsupported | Unsupported |
| 1.0.0.A14    | 31.2.50     | 0.11.3      | Unsupported  | 36.1.30    | 0.11.3      | 775        | Unknown      | 0.11.3      | Unknown     |

# Projects used
Please note that not a single project is actually distributed, the mappings are being generated (downloaded and re-formatted) by CYAN on launch, only version information is used.

| Project                         | License             | Copyright Notice                                       | License/project page                                                                       |
| :-----------------------------: | :-----------------: | :----------------------------------------------------: | :----------------------------------------------------------------------------------------: |
| MinecraftForge                  | LGPL 2.1            | Copyright (c) 2016-2021<br/>MinecraftForge             | https://github.com/MinecraftForge/MinecraftForge/blob/1.16.x/LICENSE.txt                   |
| MCP Config<br/>(Forge)          | Modified ZLIB       | Copyright (c) 2018 Forge Development LLC, MCP Team     | https://github.com/MinecraftForge/MCPConfig/blob/master/LICENSE                            |
| Fabric-Loader                   | Apache 2.0          | Copyright (c) 2021 FabricMC and contributors.          | https://github.com/FabricMC/fabric-loader/blob/master/LICENSE                              |
| Fabric YARN                     | CC0-1.0             | Copyright (c) 2021 FabricMC and contributors.          | https://github.com/FabricMC/yarn/blob/21w10a/LICENSE                                       |
| Spigot BuildData                | Unknown             | Copyright (c) 2014-2020 SpigotMC Pty. Ltd.             | https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata                             |
| Minecraft Obfuscation Mappings  | Unknown             | Copyright (c) 2020 Microsoft Corporation               | https://launcher.mojang.com/v1/objects/374c6b789574afbdc901371207155661e0509e17/client.txt |
