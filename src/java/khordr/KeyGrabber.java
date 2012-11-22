package khordr;

public class KeyGrabber {
    public static native void grab(KeyEventHandler sink);
    public static native void send(int keycode, int direction);

    static {
        System.loadLibrary("khordr");
    }
}
