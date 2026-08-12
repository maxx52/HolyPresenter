# VLCJ discovers these classes through ServiceLoader and JNA reflection.
# ProGuard cannot see those calls in bytecode and otherwise removes the
# providers from release installers while leaving META-INF/services behind.
-keep class uk.co.caprica.vlcj.** { *; }
-keep class com.sun.jna.** { *; }
