# Introduction
This guide will help set up automatic updates for your mod.

## You will need to have:
 - A command line (cmd on windows, powershell does not work well)
 - cURL (installed on most systems)
 - An ASF ModDev Account (read [SETUP-TRUST.md](SETUP-TRUST.md) for more information)
 - A secured Mod ID and Group ID (read [SETUP-TRUST.md](SETUP-TRUST.md) for more information)

# Step 1: assign the update server in your mod manifest
First, edit the mod `build.gradle` file like this:

```groovy
// File: build.gradle
// ...

// Edit the ModFile Manifest Configuration:
cmf {
	manifest {
		modfileManifest {
            // ...

            // Assigns the update server
            updateserver "https://aerialworks.ddns.net/cyan/trust/download"

            // ...
		}
	}

	archiveVersion = project.version
	archiveExtension = 'cmf'
	destinationDirectory = file("$buildDir/cmf")
}
// ...
```

The above tells Cyan to use `https://aerialworks.ddns.net/cyan/trust/download` as download base URL. You can use your own webserver for this.

The update information is streamed from the following paths:<br/>
Manifests are downloaded from: `/<group>/<id>/mod.channels.ccfg`<br/>
Channel files are downloaded from: `/<group>/<id>/channels/<channel>.ccfg`

# Step 2: writing the update manifest
Next, you will need to create a `mod.channels.ccfg` file:
```ccfg
# File: mod.channels.ccfg
# ...

# Channel name configuration
channels> {

    #
    # The default channel
    # Unless changed in the user config file, updates will be downloaded from here
    @default> 'stable'

    #
    # Format:
    # channel-key> 'name-in-updates.ccfg'
    #
    # Example channels:
    latest> 'latest'
    latestStable> 'stable'
    latestTesting> 'testing'

}

# ...
```
Save the configuration in a server directory for uploading.

# Step 3: creating the channel files
After that, create a folder named 'channels' and create the channel files:

```ccfg
#
# Version channel file
# If the file is named @fallback.ccfg, it will be used if the channel is not found.
#

# Version list (ignored for fallback)
versions> {

    # Latest 1.16.5 build
    1.16.5> '1.0-1.16.5'

    # Latest 1.17.1 build
    1.17.1> '1.0-1.17.1'

    # Latest 1.17 build
    1.17> '1.0-1.17'

}

# Template URLs for each game version
urls> {

    # Fallback URL, any game version not specified below, will be downloaded using this format.
    # The '%v' variable is replaced with the version specified by the 'versions' block. (no other variables present)
    @fallback> 'https://example.org/coremods/examplemod-%v.cmf'

    # Formats for specific game versions
    # You can use maven URLs as shown by the following examples
    # 1.16.5> 'http://example.org/maven/org/example/examplemod-1.16/%v/examplemod-%v.cmf'
    # 1.17> 'http://example.org/maven/org/example/examplemod-1.17-plus/%v/examplemod-1.17-plus-%v.cmf'
    # 1.17.1> 'http://example.org/maven/org/example/examplemod-1.17-plus/%v/examplemod-1.17-plus-%v.cmf'

}
```
Save the file by its channel name, using `@fallback` will create a fallback configuration.

# Step 4: publishing the files to the ASF server (optional)
1. Duplicate the `mod.channels.ccfg` file and name it `request.ccfg`.
2. Edit the request file as following:

```ccfg
# File: request.ccfg
# ...


# Mod group
group> org.example

# Mod ID
modid> examplemod


# A combined block of the channel files
channelFiles> {

    # Use the channel name specified in the 'channels' block
    # The following is en example file
    @fallback> {
        # Example fallback file:
        # Template URLs for each game version
        urls> {

            # Fallback URL, any game version not specified below, will be downloaded using this format.
            # The '%v' variable is replaced with the version specified by the 'versions' block. (no other variables present)
            @fallback> 'https://example.org/coremods/examplemod-%v.cmf'

        }
    }

}


# ...
```

3. Upload the file using the following command:

```bash
# Replace 'username' with your ModDev username
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/cyan/trust/set-version-document -u "username"
```
