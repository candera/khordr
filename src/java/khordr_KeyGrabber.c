#include "khordr_KeyGrabber.h"
#include <ApplicationServices/ApplicationServices.h>

#define LOG(x) printf(x); fflush(stdout);

#define KG_UP 0
#define KG_DOWN 1

#define KG_LCONTROL 59
#define KG_RCONTROL 59
#define KG_LSHIFT 56
#define KG_RSHIFT 56
#define KG_LCOMMAND 55
#define KG_RCOMMAND 55
#define KG_LALT 58
#define KG_RALT 58

jobject eventSink = NULL;
jmethodID onKeyEvent = NULL;
JNIEnv* jnienv = NULL;
CFRunLoopRef runLoop = NULL;

bool ReportKey(int keycode, int direction)
{
  fprintf(stdout, "callback: reporting event - %d, %d\n", keycode, direction);
  fflush(stdout);
  return (bool) (*jnienv)->CallBooleanMethod(jnienv, eventSink, onKeyEvent, keycode, direction, 0);
}

bool ReportIf(int flags, int previousFlags, int mask, int keycode)
{
  int xorFlags = flags ^ previousFlags;
  if ((xorFlags & mask) == mask) {
    return ReportKey(keycode, (flags & mask) == 0 ? KG_UP : KG_DOWN);
  }
}

CGEventRef
myCGEventCallback(CGEventTapProxy proxy, CGEventType type,
                  CGEventRef event, void *refcon)
{
  static CGEventFlags previousFlags = 0;
  bool allow = false;

  if ((type == kCGEventKeyDown) ||
      (type == kCGEventKeyUp)) {

    LOG("callback: key up/down\n");
    CGKeyCode keycode = (CGKeyCode)CGEventGetIntegerValueField(event, kCGKeyboardEventKeycode);
    int direction = type == kCGEventKeyDown ? KG_DOWN : KG_UP;
    allow = ReportKey(keycode, direction);
  }
  else if (type == kCGEventFlagsChanged) {
    CGEventFlags flags = CGEventGetFlags(event);

    // TODO: Don't forget about capslock Hmm: Looks like capslock
    // might not be possible to catch using this code. Might need to
    // investigate IOHIDLib instead.

    // Report the event if at least one of them is allowed

    LOG("callback: flags changed\n");
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICELCTLKEYMASK,   KG_LCONTROL);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICERCTLKEYMASK,   KG_RCONTROL);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICELSHIFTKEYMASK, KG_LSHIFT);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICERSHIFTKEYMASK, KG_RSHIFT);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICELCMDKEYMASK,   KG_LCOMMAND);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICERCMDKEYMASK,   KG_RCOMMAND);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICELALTKEYMASK,   KG_LALT);
    allow = allow || ReportIf(flags, previousFlags, NX_DEVICERALTKEYMASK,   KG_RALT);

    if (allow) {
      previousFlags = flags;
    }
  }

  if (allow) {
    LOG("callback: Allowing event\n");
    return event;
  }
  else {
    LOG("callback: Suppressing event\n");
    return NULL;
  }
}

JNIEXPORT void JNICALL Java_khordr_KeyGrabber_grab
(JNIEnv *env,
 jclass kgcls,
 jobject sink)
{
  jclass cls = (*env)->GetObjectClass(env, sink);
  onKeyEvent = (*env)->GetMethodID(env, cls, "onKeyEvent", "(II)Z");
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
  runLoopSource = CFMachPortCreateRunLoopSource(kCFAllocatorDefault, eventTap, 0);

  // Add to the current run loop.
  runLoop = CFRunLoopGetCurrent();
  CFRunLoopAddSource(runLoop, runLoopSource,
                     kCFRunLoopCommonModes);

  // Enable the event tap.
  CGEventTapEnable(eventTap, true);

  // Set it all running. This will block.
  CFRunLoopRun();
}


JNIEXPORT void JNICALL Java_khordr_KeyGrabber_send
  (JNIEnv *env, jclass kgcls, jint keycode, jint direction)
{
  // LOG("sending key\n")
  static CGEventFlags flags = 0;

  CGEventSourceRef eventSource = CGEventSourceCreate(kCGEventSourceStateHIDSystemState);
  CGEventRef evt = CGEventCreateKeyboardEvent
    (eventSource,
     (CGKeyCode) keycode,
     direction == KG_DOWN);

  if (direction == KG_UP) {
    if (keycode == KG_LSHIFT) {
      flags &= ~NX_DEVICELSHIFTKEYMASK;
    }
    else if (keycode == KG_LALT) {
      flags &= ~NX_DEVICERALTKEYMASK;
    }
    else if (keycode == KG_LCOMMAND) {
      flags &= ~NX_DEVICELCMDKEYMASK;
    }
    else if (keycode == KG_LCONTROL) {
      flags &= ~NX_DEVICELCTLKEYMASK;
    }
  }
  else {
    if (keycode == KG_LSHIFT) {
      flags |= NX_DEVICELSHIFTKEYMASK;
    }
    else if (keycode == KG_LALT) {
      flags |= NX_DEVICERALTKEYMASK;
    }
    else if (keycode == KG_LCOMMAND) {
      flags |= NX_DEVICELCMDKEYMASK;
    }
    else if (keycode == KG_LCONTROL) {
      flags |= NX_DEVICELCTLKEYMASK;
    }
  }

  printf("send: flags now 0x%x\n", (unsigned int) flags);
  fflush(stdout);

  if (keycode == KG_LSHIFT || keycode == KG_LCONTROL ||
      keycode == KG_LALT || keycode == KG_LCOMMAND) {
    CGEventPost(kCGSessionEventTap, evt);
    /* if (direction == KG_DOWN) { */
    /*   LOG("send: not sending modifier\n"); */
    /* } */
    /* else { */
    /* } */
  }
  else {
    CGEventSetFlags(evt, flags);
    CGEventPost(kCGSessionEventTap, evt);
  }
  CFRelease(evt);
  CFRelease(eventSource);

}

JNIEXPORT void JNICALL Java_khordr_KeyGrabber_stop
  (JNIEnv *env, jclass kgcls)
{
  LOG("stop: shutting down\n");
  // I have no idea if this is correct: should I be stopping the run
  // loop from a different thread? Do I need to call
  // CFRetain/CFRelease?
  CFRunLoopStop(runLoop);
}
