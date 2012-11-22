#include "khordr_KeyGrabber.h"
#include <ApplicationServices/ApplicationServices.h>

#define LOG(x) printf(x); fflush(stdout);

#define KG_UP 0
#define KG_DOWN 1

#define KG_LCONTROL -1
#define KG_RCONTROL -2
#define KG_LSHIFT -3
#define KG_RSHIFT -4
#define KG_LCOMMAND -5
#define KG_RCOMMAND -6
#define KG_LALT -7
#define KG_RALT -8

jobject eventSink = NULL;
jmethodID onKeyEvent = NULL;
JNIEnv* jnienv = NULL;

void ReportKey(int keycode, int direction)
{
    (*jnienv)->CallVoidMethod(jnienv, eventSink, onKeyEvent, keycode, direction, 0);
}

void ReportIf(int flags, int previousFlags, int mask, int keycode)
{
  int xorFlags = flags ^ previousFlags;
  if ((xorFlags & mask) == mask) {
    ReportKey(keycode, (flags & mask) == 0 ? KG_UP : KG_DOWN);
  }
}



CGEventRef
myCGEventCallback(CGEventTapProxy proxy, CGEventType type,
                  CGEventRef event, void *refcon)
{
  static CGEventFlags previousFlags = 0;
  bool suppress = false;

  LOG(".")

  if ((type == kCGEventKeyDown) ||
      (type == kCGEventKeyUp)) {

    CGKeyCode keycode = (CGKeyCode)CGEventGetIntegerValueField(event, kCGKeyboardEventKeycode);
    int direction = type == kCGEventKeyDown ? KG_DOWN : KG_UP;
    ReportKey(keycode, direction);
  }
  else if (type == kCGEventFlagsChanged) {
    CGEventFlags flags = CGEventGetFlags(event);

    // TODO: Don't forget about capslock Hmm: Looks like capslock
    // might not be possible to catch using this code. Might need to
    // investigate IOHIDLib instead.

    ReportIf(flags, previousFlags, NX_DEVICELCTLKEYMASK,   KG_LCONTROL);
    ReportIf(flags, previousFlags, NX_DEVICERCTLKEYMASK,   KG_RCONTROL);
    ReportIf(flags, previousFlags, NX_DEVICELSHIFTKEYMASK, KG_LSHIFT);
    ReportIf(flags, previousFlags, NX_DEVICERSHIFTKEYMASK, KG_RSHIFT);
    ReportIf(flags, previousFlags, NX_DEVICELCMDKEYMASK,   KG_LCOMMAND);
    ReportIf(flags, previousFlags, NX_DEVICERCMDKEYMASK,   KG_RCOMMAND);
    ReportIf(flags, previousFlags, NX_DEVICELALTKEYMASK,   KG_LALT);
    ReportIf(flags, previousFlags, NX_DEVICERALTKEYMASK,   KG_RALT);

    previousFlags = flags;
  }

  return NULL;

}

JNIEXPORT void JNICALL Java_khordr_KeyGrabber_grab
(JNIEnv *env, 
 jclass kgcls,
 jobject sink)
{
  jclass cls = (*env)->GetObjectClass(env, sink);
  onKeyEvent = (*env)->GetMethodID(env, cls, "onKeyEvent", "(II)V");
  eventSink = (*env)->NewGlobalRef(env, sink);
  jnienv = env;

  CFMachPortRef      eventTap;
  CGEventMask        eventMask;
  CFRunLoopSourceRef runLoopSource;

  // Create an event tap. We are interested in key presses.
  eventMask = ((1 << kCGEventKeyDown) |
               (1 << kCGEventKeyUp) |
               (1 << kCGEventFlagsChanged)
               );
  eventTap = CGEventTapCreate(kCGSessionEventTap, kCGHeadInsertEventTap, 0,
                              eventMask, myCGEventCallback, NULL);

  if (!eventTap) {
    return;
  }

  // Create a run loop source.
  runLoopSource = CFMachPortCreateRunLoopSource(
                                                kCFAllocatorDefault, eventTap, 0);

  // Add to the current run loop.
  CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource,
                     kCFRunLoopCommonModes);

  // Enable the event tap.
  CGEventTapEnable(eventTap, true);

  // Set it all running. This will block.
  CFRunLoopRun();
}


JNIEXPORT void JNICALL Java_khordr_KeyGrabber_send
  (JNIEnv *env, jclass kgcls, jint keycode, jint direction)
{
  static CGEventFlags currentFlags = 0; 

  LOG("sending key\n")

  if (keycode < 0) {
    
    CGEventFlags flag = 0;

    switch (keycode) { 
      case KG_RCONTROL: flag = NX_CONTROLMASK; break;
      case KG_LCONTROL: flag = NX_CONTROLMASK; break;
      case KG_LSHIFT: flag = NX_SHIFTMASK; break;
      case KG_RSHIFT: flag = NX_SHIFTMASK; break;
      case KG_LCOMMAND: flag = NX_COMMANDMASK; break;
      case KG_RCOMMAND: flag = NX_COMMANDMASK; break;
      case KG_LALT: flag = NX_ALTERNATEMASK; break;
      case KG_RALT: flag = NX_ALTERNATEMASK; break;
      }      

    if (direction == KG_UP) {
      currentFlags &= ~flag;
    } else {
      currentFlags |= flag;
    }
  }
  else {
    CGEventRef evt = CGEventCreateKeyboardEvent
      (NULL, 
       (CGKeyCode) keycode, 
       direction == KG_DOWN);
    CGEventSetFlags(evt, currentFlags);
    LOG("Calling CGEventPost\n");
    CGEventPost(kCGSessionEventTap, evt);
    CFRelease(evt);
  }

}

