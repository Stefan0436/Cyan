# Introduction
This document helps set up the remote server for the trust container downloads.
This guide is not for setting up your own server, instead, this guide will help create a ASF ModDev account.

# Creating the account:
To create a account, you will need to have cURL installed (Command line utility)
Using cURL, you can send POST requests to our account server.

After sending the request, to verify you are not a bot, you will receive an activation mail.
Our servers do not really pass all checks, so our messages will end up in spam and are delayed for Gmail users. Outlook users receive the mail immediately, but it ends up 'junk email'

You can find the sources for the usermanager here: https://github.com/Stefan0436/UserManager.
The Cyan Trust Server module can  be found here: https://github.com/Stefan0436/CyanTrustServer,

## Sending the request:
Like any CYAN application, the account manager uses CCFG for requests, here is a template request:

```ccfg
# The group we want to use
group> 'moddev'

# The username to create
username> 'someusername'

# Password to use,
# Please keep it safe, destroy the request after sending it.
# It is best to use new passwords to be on the safe side.
password> 'somepassword'

# The receiver email address
# The owner of this mail address is sent the account activation key,
# The mail is always delayed for Gmail users, Outlook users receive it in 'junk'
ownerEmail> 'someone@example.com'
```

Save the request document to request.ccfg and use the following command to send it:

```bash
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/users/create
rm request.ccfg # Destroys the request
```

## After sending:
After the request has been sent, you will see the cancel code, please save it so you can cancel the request if needed.
The activation key will be sent to the `ownerEmail` address. It might take a while for it to be received, the key is valid for 30 days. Outlook users receive the mail in junk. Most will receive it in something like that or spam.

## Activating the user:
If you have received the activation code, you can use it to enable the account through curl and ccfg.
Activation request template:

```ccfg
# Activation key, excuse the caps, just an example
activationKey> 'THE-ACTIVATION-KEY-THAT-HAS-TEN-SEGMENTS-AND-SEVEN-CHARACTERS'
```

Sending the request:

```bash
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/users/activate
```

## Testing the account:
If you have activated the user, you can use the following command to test if it has been created correctly:

```bash
curl https://aerialworks.ddns.net/users/authenticate?group=moddev -u "username"
```

It should ask for a password and then return OK, if you get an HTML page, the password was incorrect or the accout failed to register. (never recorded to happen)


# Registering the mod group:
After creating a moddev account, you can register your mod group with the following request, after using this, you will own the group and all mods that are a part of it:

```ccfg
# The mod group to register
# Needs to be shared with your mods
group> 'org.example'
```

Sending the request:

```bash
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/cyan/trust/create-group -u "username"
```

It should return created if successful. You can create up to 5 groups with 10 mods in each.
This limit will be increased in the future if needed.

# Registering a mod id:
After registering the group, you can add mod ids with the following request:

```ccfg
# Mod group to use
group> 'org.example'

# Mod id to register, needs to be shared with the mod in question
modid> 'examplemod'
```

Sending the request:

```bash
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/cyan/trust/register-modid -u "username"
```

It should return 'ModID has been registered' if successful.

# Deleting mod ids:
If you want to delete mods, use the following request:

```ccfg
group> 'org.example'
modid> 'examplemod'
```

Sending the request:

```bash
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/cyan/trust/unregister-mod -u "username"
```

Should return 'Mod has been deleted' if successful. You can swap unregister-mod with delete-group to unregister groups and all their mods.

# Setting trust server locations:
The following request will set the trust server used to download the CTC file for the mod.
By setting it to the AerialWorks Server, it will secure the mod and the trust can then only be downloaded from there.

```ccfg
# Mod group to use
group> 'org.example'

# Mod id to use
modid> 'examplemod'

# Destination address, use 'null' to unset it.
location> 'https://aerialworks.ddns.net/cyan/trust/download'
```

Sending the request:

```bash
curl -X POST --data-binary @request.ccfg https://aerialworks.ddns.net/cyan/trust/set-server-location -u "username"
```

Should return OK if successful.

# Good to go
You are now ready, your mod has been registered and secured, you can now upload mod information files to it.
