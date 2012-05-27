package interception;
import com.ochafik.lang.jnaerator.runtime.LibraryExtractor;
import com.ochafik.lang.jnaerator.runtime.MangledFunctionMapper;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public interface TestLibrary extends Library {
    public static final String JNA_LIBRARY_NAME = LibraryExtractor.getLibraryPath("test", true, TestLibrary.class);
    public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(TestLibrary.JNA_LIBRARY_NAME, MangledFunctionMapper.DEFAULT_OPTIONS);
    public static final TestLibrary INSTANCE = (TestLibrary)Native.loadLibrary(TestLibrary.JNA_LIBRARY_NAME, TestLibrary.class, MangledFunctionMapper.DEFAULT_OPTIONS);

    int DoSomething(int x);
}
