package khordr;

public interface KeyEventHandler {
    // Returns true if the key should be allowed through
    public boolean onKeyEvent(int key, int direction);
}
