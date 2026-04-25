# AndroidX Startup initializer - discovered via manifest merge + reflection.
-keep class com.atruedev.kmpuwb.adapter.KmpUwbInitializer {
    public <init>();
    public * create(android.content.Context);
    public java.util.List dependencies();
}
