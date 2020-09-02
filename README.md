MCStatus
========

Android app to monitor the status of Minecraft servers

The MinecraftServer class is intended to be able to be used standalone if you wanted to include it in something else to use it to query the server.  Although not quite a direct port, most of the process was taken from https://github.com/xPaw/PHP-Minecraft-Query/blob/master/src/MinecraftPing.php . I wrote this because I couldn't find any open source solutions for this problem on Android, although several proprietary ones apparently exist with unresponsive developers.

Pretty sure this currently only works on version 1.7 and newer servers.

Features:

* Add, remove, and edit servers in the list to check
* Will list the server's MOTD (message of the day), how many users are connected, and (if supplied by the server) the list of connected users.

Right now you have to manually refresh (it'll also refresh if you close and reopen the app, and if you rotate the screen).  Eventually I want it to periodically update while the app it open (preference for how frequently perhaps?), and perhaps even check in the background and do notifications if someone connects, etc.
If you want to help, please do. :-)  Pull requests welcome.

Click on "releases" at the right sidebar of the Github page for downloads or click this link.

https://github.com/justdave/MCStatus/releases
