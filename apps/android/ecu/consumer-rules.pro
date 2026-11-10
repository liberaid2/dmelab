# UniFFI and JNA use reflection and native registration.
-keep class com.sun.jna.** { *; }
-keep class dev.coldcat.dmelab.ffi.** { *; }
# JNA's unused desktop helpers reference AWT, which Android does not provide.
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
