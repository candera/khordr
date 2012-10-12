package khordr;
import com.ochafik.lang.jnaerator.runtime.LibraryExtractor;
import com.ochafik.lang.jnaerator.runtime.MangledFunctionMapper;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public interface ApplicationServicesLibrary extends Library {
    public static final ApplicationServicesLibrary INSTANCE =
        (ApplicationServicesLibrary)
        Native.loadLibrary("ApplicationServices", ApplicationServicesLibrary.class, MangledFunctionMapper.DEFAULT_OPTIONS);

    public static interface CGEventTapLocation {
        public static final int kCGHIDEventTap = 0;
        public static final int kCGSessionEventTap = 1;
        public static final int kCGAnnotatedSessionEventTap = 2;
    }

    public static interface CGEventTapPlacement {
        public static final int kCGHeadInsertEventTap = 0;
        public static final int kCGTailAppendEventTap = 1;
    }

    public static interface CGEventTapOptions {
        public static final int kCGEventTapOptionDefault = (int) 0x00000000;
        public static final int kCGEventTapOptionListenOnly = (int) 0x00000001;
    }

    public static interface CGEventType {
        public static final int NX_NULLEVENT = 0;       /* internal use */

        /* mouse events */

        public static final int NX_LMOUSEDOWN = 1;      /* left mouse-down event */
        public static final int NX_LMOUSEUP = 2;        /* left mouse-up event */
        public static final int NX_RMOUSEDOWN = 3;      /* right mouse-down event */
        public static final int NX_RMOUSEUP = 4;        /* right mouse-up event */
        public static final int NX_MOUSEMOVED = 5;      /* mouse-moved event */
        public static final int NX_LMOUSEDRAGGED = 6;   /* left mouse-dragged event */
        public static final int NX_RMOUSEDRAGGED = 7;   /* right mouse-dragged event */
        public static final int NX_MOUSEENTERED = 8;    /* mouse-entered event */
        public static final int NX_MOUSEEXITED = 9;     /* mouse-exited event */

        /* other mouse events
         *
         * event.data.mouse.buttonNumber should contain the
         * button number (2-31) changing state.
         */
        public static final int NX_OMOUSEDOWN = 25;     /* other mouse-down event */
        public static final int NX_OMOUSEUP = 26;       /* other mouse-up event */
        public static final int NX_OMOUSEDRAGGED = 27;  /* other mouse-dragged event */

        /* keyboard events */

        public static final int NX_KEYDOWN = 10;        /* key-down event */
        public static final int NX_KEYUP = 11;  /* key-up event */
        public static final int NX_FLAGSCHANGED = 12;   /* flags-changed event */

        /* composite events */

        public static final int NX_KITDEFINED = 13;     /* application-kit-defined event */
        public static final int NX_SYSDEFINED = 14;     /* system-defined event */
        public static final int NX_APPDEFINED = 15;     /* application-defined event */

        /* There are additional DPS client defined events past this point. */

        /* Scroll wheel events */

        public static final int NX_SCROLLWHEELMOVED = 22;

        /* Zoom events */
        public static final int NX_ZOOM = 28;

        /* tablet events */

        public static final int NX_TABLETPOINTER = 23;  /* for non-mousing transducers */
        public static final int NX_TABLETPROXIMITY = 24;  /* for non-mousing transducers */


        public static final int kCGEventNull = NX_NULLEVENT;
        public static final int kCGEventLeftMouseDown = NX_LMOUSEDOWN;
        public static final int kCGEventLeftMouseUp = NX_LMOUSEUP;
        public static final int kCGEventRightMouseDown = NX_RMOUSEDOWN;
        public static final int kCGEventRightMouseUp = NX_RMOUSEUP;
        public static final int kCGEventMouseMoved = NX_MOUSEMOVED;
        public static final int kCGEventLeftMouseDragged = NX_LMOUSEDRAGGED;
        public static final int kCGEventRightMouseDragged = NX_RMOUSEDRAGGED;
        public static final int kCGEventKeyDown = NX_KEYDOWN;
        public static final int kCGEventKeyUp = NX_KEYUP;
        public static final int kCGEventFlagsChanged = NX_FLAGSCHANGED;
        public static final int kCGEventScrollWheel = NX_SCROLLWHEELMOVED;
        public static final int kCGEventTabletPointer = NX_TABLETPOINTER;
        public static final int kCGEventTabletProximity = NX_TABLETPROXIMITY;
        public static final int kCGEventOtherMouseDown = NX_OMOUSEDOWN;
        public static final int kCGEventOtherMouseUp = NX_OMOUSEUP;
        public static final int kCGEventOtherMouseDragged = NX_OMOUSEDRAGGED;
        public static final int kCGEventTapDisabledByTimeout = 0xFFFFFFFE;
        public static final int kCGEventTapDisabledByUserInput = 0xFFFFFFF;
    }

    public static interface CGEventField {
        public static final int kCGMouseEventNumber = 0;
        public static final int kCGMouseEventClickState = 1;
        public static final int kCGMouseEventPressure = 2;
        public static final int kCGMouseEventButtonNumber = 3;
        public static final int kCGMouseEventDeltaX = 4;
        public static final int kCGMouseEventDeltaY = 5;
        public static final int kCGMouseEventInstantMouser = 6;
        public static final int kCGMouseEventSubtype = 7;
        public static final int kCGKeyboardEventAutorepeat = 8;
        public static final int kCGKeyboardEventKeycode = 9;
        public static final int kCGKeyboardEventKeyboardType = 10;
        public static final int kCGScrollWheelEventDeltaAxis1 = 11;
        public static final int kCGScrollWheelEventDeltaAxis2 = 12;
        public static final int kCGScrollWheelEventDeltaAxis3 = 13;
        public static final int kCGScrollWheelEventFixedPtDeltaAxis1 = 93;
        public static final int kCGScrollWheelEventFixedPtDeltaAxis2 = 94;
        public static final int kCGScrollWheelEventFixedPtDeltaAxis3 = 95;
        public static final int kCGScrollWheelEventPointDeltaAxis1 = 96;
        public static final int kCGScrollWheelEventPointDeltaAxis2 = 97;
        public static final int kCGScrollWheelEventPointDeltaAxis3 = 98;
        public static final int kCGScrollWheelEventInstantMouser = 14;
        public static final int kCGTabletEventPointX = 15;
        public static final int kCGTabletEventPointY = 16;
        public static final int kCGTabletEventPointZ = 17;
        public static final int kCGTabletEventPointButtons = 18;
        public static final int kCGTabletEventPointPressure = 19;
        public static final int kCGTabletEventTiltX = 20;
        public static final int kCGTabletEventTiltY = 21;
        public static final int kCGTabletEventRotation = 22;
        public static final int kCGTabletEventTangentialPressure = 23;
        public static final int kCGTabletEventDeviceID = 24;
        public static final int kCGTabletEventVendor1 = 25;
        public static final int kCGTabletEventVendor2 = 26;
        public static final int kCGTabletEventVendor3 = 27;
        public static final int kCGTabletProximityEventVendorID = 28;
        public static final int kCGTabletProximityEventTabletID = 29;
        public static final int kCGTabletProximityEventPointerID = 30;
        public static final int kCGTabletProximityEventDeviceID = 31;
        public static final int kCGTabletProximityEventSystemTabletID = 32;
        public static final int kCGTabletProximityEventVendorPointerType = 33;
        public static final int kCGTabletProximityEventVendorPointerSerialNumber = 34;
        public static final int kCGTabletProximityEventVendorUniqueID = 35;
        public static final int kCGTabletProximityEventCapabilityMask = 36;
        public static final int kCGTabletProximityEventPointerType = 37;
        public static final int kCGTabletProximityEventEnterProximity = 38;
        public static final int kCGEventTargetProcessSerialNumber = 39;
        public static final int kCGEventTargetUnixProcessID = 40;
        public static final int kCGEventSourceUnixProcessID = 41;
        public static final int kCGEventSourceUserData = 42;
        public static final int kCGEventSourceUserID = 43;
        public static final int kCGEventSourceGroupID = 44;
        public static final int kCGEventSourceStateID = 45;
        public static final int kCGScrollWheelEventIsContinuous = 8;
    };

    public interface CGEventTapCallback extends Callback {
        Pointer onevent(Pointer proxy, int type, Pointer event, Pointer refcon);
    }

    Pointer CGEventTapCreate(int tap, int place, int options, long eventsOfInterest,
                             CGEventTapCallback callback, Pointer refcon);

    Pointer CFMachPortCreateRunLoopSource(Pointer allocator, Pointer port, long order);

    void CFRunLoopAddSource(Pointer rl, Pointer source, Pointer mode);

    void CGEventTapEnable(Pointer tap, boolean enable);

    void CFRunLoopRun();

    long CGEventGetIntegerValueField(Pointer event, int field);

    Pointer CFRunLoopGetCurrent();
}
