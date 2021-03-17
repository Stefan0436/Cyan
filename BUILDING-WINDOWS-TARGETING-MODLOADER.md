# Building for windows and targeting specific modloader.
As stated in BUILDING, this process is a big rabbit hole, know what you are getting yourself into.

# First steps
Choose the modloader id, here you have the graph of CYAN internal modloader ids, you will also need the inherits-from id<br />
(replace [version] with the modloader version, replace [game] with the game version after the next step):

| Modloader               | ID Format               | Inherits-from format               |
| :---------------------: | :---------------------: | :--------------------------------: |
| Forge                   | forge-[version]         | [game]-forge-[version]             |
| Fabric                  | fabric-loader-[version] | fabric-loader-[version]-[game]     |
| Paper                   | paper-[version]         | [game]-paper-[version]             |

# Getting the version information
Now that you have the CYAN ID and inherits-from id for the modloader, you will need to retrieve the modloader version.

### Forge
1. For forge, head over to [files.minecraftforge.net](https://files.minecraftforge.net) (open in new tab)
2. Select the game version
3. Select the latest, DON'T DOWNLOAD
4. Take the numbers after the game version; formatted like: [game] - [version], you will want the version, something like 36.1.0

### Fabric
1. For fabric, use the following link to get the version, replace [game] with the game version:<br />
   Metadata URL: `https://meta.fabricmc.net/v2/versions/loader/[game]`
2. You have a JSON file in the browser, you'll wan't the first 'version' node (line 7, numbers with dots, formatted: `"version": "[version]"`)
3. There you go, you have the fabric version

### Paper, DO NOT USE AN OLDER VERSION FOR THIS
1. Head over to [Paper's API Interface (uses swagger)](https://papermc.io/api/docs/swagger-ui/index.html?configUrl=/api/openapi/swagger-config#/projects-controller/projectVersion) (open in new tab, you will be redirected to the right api interface)
2. Select 'Try it out' (in the /v2/projects/{project}/versions/{version} section)
3. Fill out the form, enter paper as id, and the game version as version
4. Select execute
5. Scroll down to the response body, another json file
6. In the response body, scroll down to the end
7. Pick the latest build, the 3rd line from the bottom
8. Note it and close the ui

<br />

# Preparing the modloader command line arguments
1. Now that you have the modloader version and game version, fill out both the id and inherits from templates.
2. Create the gradle arguments: `-PsetModLoader=[modloader-id] -PsetInheritsFromVersion=[inherits-from]`<br />
   (replace [modloader-id] with the modloader id, replace [inherits-from] with the inherits-from id)

# Selecting the right launch wrappers
CYAN is based on launch wrappers, they are the pieces of code bootstrapping the game and modloader, each modloader has their own.<br />
To help, we have a graph for selecting the right launch wrappers.

| Modloader               | Client launch wrapper     | Server launch wrapper       |
| :---------------------: | :-----------------------: | :-------------------------: |
| Forge                   | CyanForgeClientWrapper    | CyanForgeServerWrapper      |
| Fabric                  | CyanFabricClientWrapper   | CyanFabricServerWrapper     |
| Paper                   | NONE, SERVER ONLY         | CyanPaperServerWrapper      |

# Creating the launch wrapper arguments
1. Now that you have the launch wrappers, you will need to create the gradle arguments for using them.<br />
   (replace [launch-wrapper-client] with the client wrapper, replace [launch-wrapper-server] with the server wrapper)
2. Client wrapper (only use if the modloader has one): `-PoverrideLaunchWrapperClient=[launch-wrapper-client]`
3. Server wrapper (only use if the modloader has one): `-PoverrideLaunchWrapperServer=[launch-wrapper-server]`

# Combine your arguments and create the gradle commands
1. Fill out the following template: `gradlew.bat -PcurrentXML -PoverrideCyanLibraryURL="" [modloader-arguments] [wrapper-arguments]`
2. Add the following if you want to use a different game version: `-PoverrideGameVersion=[game]` (replace [game] with the game version)
3. Take it as a template and fill out the following commands (replace [base-command] with your new command):

```batch
[base-command] -PresetLibSourceCache processResources
[base-command] build serverJar [extra-task]
[base-command] -PresetLibSourceCache build gameData serverJar
```

4. Replace [extra-task] with gameData if the client wrapper is supported, remove it otherwise.
5. Run the commands

