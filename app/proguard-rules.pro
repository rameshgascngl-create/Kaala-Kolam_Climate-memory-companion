# Keep kotlinx.serialization metadata generated for the typed local state.
-keepattributes *Annotation*,InnerClasses
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
