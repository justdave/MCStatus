MCStatus
========

Android app to monitor the status of Minecraft servers

The MinecraftServer class is intended to be able to be used standalone if you wanted to include it in something else to use it to query the server.  Although not a direct port, most of the process was taken from https://github.com/xPaw/PHP-Minecraft-Query/blob/master/MinecraftServerPing.php

Pretty sure this currently only works on version 1.7 and newer servers.

This is relatively incomplete still...
Adding new servers works, removing servers works, editing existing ones is not yet implemented.
Right now you have to manually refresh (it'll also refresh if you close and reopen the app, and if you rotate the screen).  Eventually I want it to periodically update while the app it open (preference for how frequently perhaps?), and perhaps even check in the background and do notifications if someone connects, etc.
If you want to help, please do. :-)  Pull requests welcome.
I also need a real app icon.