This project is to build a secure camera app for android.  It is a project of
Guardian Project, Witness, and more.

Learn more at:
https://guardianproject.info/apps/obscuracam/


Building from the terminal
--------------------------

```
 git clone https://github.com/guardianproject/SecureSmartCam.git
 cd SecureSmartCam
 git submodule update --init --recursive
 ./setup-ant.sh
 ant clean debug
 ls -l bin/Obscura*.apk
```


QuickActions
------------

"The QuickActions dialog is not included in standard Android SDK, so we have
to create it manually. At first, i had no idea on how to create it so i
decided to download and read the Contact app source code from Android git. I
found that the QuickContact dialog uses private API call
(com.android.internal.policy.PolicyManager) that does not exists in standard
SDK. After posting question about it on google groups and stack overflow, i
got the solution for it from Qberticus (thanx Qberticus!)."
http://code.google.com/p/simple-quickactions/
http://www.londatiga.net/it/how-to-create-quickaction-dialog-in-android/
