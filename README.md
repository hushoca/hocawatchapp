About
-------------

HERE IS SOME SCREEN SHOTS OF THIS APP'S CURRENT STATE:


I am designing a smart watch as my final year project which consists of:

- Designing a full circuit
- Designing the android application so it can connect to a phone
- Firmware for the watch

This code is the second part of my project the Android application. This application is supposed to connect to the watch and send the requested data to the watch. WARNING: This is not a finished application yet. I am just posting the process I made over 1 week of period.

Features:
-------------

**Revision 3**
> - Still has the same abilities.
> - Code reduced down to 600 lines of code
> - Everything compressed down to 2 class files (Used to have 5 but increased the lines of code by around 300 because instead of starting dialogs from bluetooth broadcaster (which handled bluetooth intents) had to send intents to MainActivity to start dialogs)
> - Fixed bunch of bugs (Android is stupid...)

**Revision 1-2**
> - Can connect to a Bluetooth device (watch)
> - Can communicate with the Bluetooth device using the BluetoothSockets.
> - A nice dialog to show the list of devices found.
> - A warning dialog when no devices are found.
> - Requests to turn on Bluetooth if it is off.
> - Run a foreground service which will handle all the messaging.
> - Can send GPS and Time data to the watch.
> - AROUND 1200 LINES OF CODE.
 

AND OF COURSE!
Feel free to use source-code in anyway. Or modify the already built application. It's open-source do whatever you want!

![](http://www.mememaker.net/static/images/memes/4615246.jpg)
