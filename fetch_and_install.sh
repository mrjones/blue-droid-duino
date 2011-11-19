# For doing development remotely (I develop over ssh on a linode):
# - On the linode: Run 'ant debug-publish' (copies the APK to /var/www/droid/
# - On the local laptop: run ./fetch_and_install.sh to pull the APK off the server
#   (over HTTP), and install it to the device connected via USB.
curl -O http://linode.mrjon.es/droid/bluedroidduino-debug.apk && adb -s HT9CWP900096 install -r bluedroidduino-debug.apk
