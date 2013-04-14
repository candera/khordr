// alterkeys.c
// http://osxbook.com
//
// Complile using the following command line:
//     gcc -Wall -o alterkeys alterkeys.c -framework ApplicationServices
//
// You need superuser privileges to create the event tap, unless accessibility
// is enabled. To do so, select the "Enable access for assistive devices"
// checkbox in the Universal Access system preference pane.

#include <ApplicationServices/ApplicationServices.h>

CGEventSourceRef myEventSource;
CGEventSourceStateID myEventSourceID;

void PrintFlags(CGEventFlags flags)
{
  printf("0x%x (%s %s %s %s %s %s %s %s)\n",
         (int) flags,
         (flags & NX_DEVICELCTLKEYMASK) ? "lcontrol" : "",
         (flags & NX_DEVICERCTLKEYMASK) ? "rcontrol" : "",
         (flags & NX_DEVICELSHIFTKEYMASK) ? "lshift" : "",
         (flags & NX_DEVICERSHIFTKEYMASK) ? "rshift" : "",
         (flags & NX_DEVICELCMDKEYMASK) ? "lcommand" : "",
         (flags & NX_DEVICERCMDKEYMASK) ? "rcommand" : "",
         (flags & NX_DEVICELALTKEYMASK) ? "lalt" : "",
         (flags & NX_DEVICERALTKEYMASK) ? "ralt" : "");
}


// This callback will be invoked every time there is a keystroke.
//
CGEventRef
myCGEventCallback(CGEventTapProxy proxy, CGEventType type,
                  CGEventRef event, void *refcon)
{
  bool suppress = false;

  // Don't intercept events from ourselves
  if (CGEventGetIntegerValueField(event, kCGEventSourceStateID) == myEventSourceID) {
    printf("Ignoring self-sent event ");
    if (type == kCGEventKeyDown) {
      printf("%ld down", CGEventGetIntegerValueField(event, kCGKeyboardEventKeycode));
    }
    else if (type == kCGEventKeyUp) {
      printf("%ld up", CGEventGetIntegerValueField(event, kCGKeyboardEventKeycode));
    }
    else if (type == kCGEventFlagsChanged) {
      printf(" flags changed ");
      PrintFlags(CGEventGetFlags(event));
    }
    else {
      printf(" other");
    }
    printf("\n");
    return event;
  }    
        
  if ((type == kCGEventKeyDown) ||
      (type == kCGEventKeyUp)) {

    // The incoming keycode.
    CGKeyCode keycode =
      (CGKeyCode)CGEventGetIntegerValueField(event, kCGKeyboardEventKeycode);

    printf("%d %s\n", keycode, type == kCGEventKeyUp ? "up" : "down");
    // printf("Event thread: %d\n", pthread_mach_thread_np(pthread_self()));


    if (keycode == (CGKeyCode)0 && type == kCGEventKeyDown) {
      CGEventRef modDown = CGEventCreateKeyboardEvent(myEventSource, (CGKeyCode) 59, true);
      CGEventRef keyDown = CGEventCreateKeyboardEvent(myEventSource, (CGKeyCode) 14, true);
      CGEventRef keyUp   = CGEventCreateKeyboardEvent(myEventSource, (CGKeyCode) 14, false);
      CGEventRef modUp   = CGEventCreateKeyboardEvent(myEventSource, (CGKeyCode) 59, false);
      //CGEventSetFlags(modDown, CGEventGetFlags(modDown) | kCGEventFlagMaskControl);
      //CGEventSetFlags(modUp,  CGEventGetFlags(modUp) | kCGEventFlagMaskControl);
      CGEventSetFlags(keyDown, CGEventGetFlags(keyDown) | kCGEventFlagMaskControl);
      CGEventSetFlags(keyUp,  CGEventGetFlags(keyUp) | kCGEventFlagMaskControl);
      CGEventPost(kCGHIDEventTap, modDown);
      CGEventPost(kCGHIDEventTap, keyDown);
      CGEventPost(kCGHIDEventTap, keyUp);
      CGEventPost(kCGHIDEventTap, modUp);
      CFRelease(modDown);
      CFRelease(keyDown);
      CFRelease(keyUp);
      CFRelease(modUp);
      suppress = true;
    }
  }

/* #define      NX_DEVICELCTLKEYMASK    0x00000001 */
/* #define      NX_DEVICELSHIFTKEYMASK  0x00000002 */
/* #define      NX_DEVICERSHIFTKEYMASK  0x00000004 */
/* #define      NX_DEVICELCMDKEYMASK    0x00000008 */
/* #define      NX_DEVICERCMDKEYMASK    0x00000010 */
/* #define      NX_DEVICELALTKEYMASK    0x00000020 */
/* #define      NX_DEVICERALTKEYMASK    0x00000040 */
/* #define NX_DEVICERCTLKEYMASK 0x00002000 */
  else if (type == kCGEventFlagsChanged) {
    CGEventFlags flags = CGEventGetFlags(event);
    printf("Event Flags changed: ");
    PrintFlags(flags);
    
  }

  // We must return the event for it to be useful.
  if (suppress)
    return NULL;
  else
    return event;
}

int
main(void)
{
  CFMachPortRef      eventTap;
  CGEventMask        eventMask;
  CFRunLoopSourceRef runLoopSource;

  myEventSource = CGEventSourceCreate(kCGEventSourceStateCombinedSessionState);
  myEventSourceID = CGEventSourceGetSourceStateID(myEventSource);

  // Create an event tap. We are interested in key presses.
  eventMask =
    CGEventMaskBit(kCGEventKeyDown)
    | CGEventMaskBit(kCGEventKeyUp)
    | CGEventMaskBit(kCGEventFlagsChanged)
    ;
  eventTap = CGEventTapCreate(kCGSessionEventTap,
                              kCGHeadInsertEventTap,
                              kCGEventTapOptionDefault,
                              eventMask,
                              myCGEventCallback,
                              NULL);

  //printf("eventTap: 0x%lx\n", (long) eventTap);
  if (!eventTap) {
    fprintf(stderr, "failed to create event tap: did you enable access for assistive devices?\n");
    exit(1);
  }

  // Create a run loop source.
    /* printf("CFMAchPortCreateRunLoopSource(0x%lx, 0x%lx, 0)\n",  */
    /*      (long) kCFAllocatorDefault,  */
    /*      (long) eventTap); */
  runLoopSource = CFMachPortCreateRunLoopSource(NULL, eventTap, 0);

  // Disable the event tap.
  CGEventTapEnable(eventTap, false);

  // Add to the current run loop.
  CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource,
                     kCFRunLoopCommonModes);


  // Enable the event tap.
  CGEventTapEnable(eventTap, true);

  /* printf("kCFRunLoopCommonModes: 0x%lx\n", (long) kCFRunLoopCommonModes); */

  printf("Starting\n");

  /* printf("Main thread: %d\n", pthread_mach_thread_np(pthread_self())); */

  // Set it all running.
  CFRunLoopRun();

  // In a real program, one would have arranged for cleaning up.

  exit(0);
}
