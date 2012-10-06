package khordr;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public interface ApplicationServicesLibrary extends Library {
    public static final ApplicationServicesLibrary INSTANCE = 
        (ApplicationServicesLibrary)
        Native.loadLibrary("ApplicationServices", ApplicationServicesLibrary.class);
}
