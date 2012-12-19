package khordr;

public class KeyGrabber {
    public static native void grab(KeyEventHandler sink);
    public static native void send(int keycode, int direction);
    public static native void stop();

    static {
        System.loadLibrary("khordr");
    }
}
