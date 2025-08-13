# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep React Native classes
-keep class com.facebook.react.** { *; }
-keep class com.reactnativeposprinter.** { *; }

# Keep Bluetooth classes
-keep class android.bluetooth.** { *; }

# Keep USB serial classes
-keep class com.hoho.android.usbserial.** { *; }