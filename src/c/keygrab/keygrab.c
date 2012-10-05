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

// This callback will be invoked every time there is a keystroke.
//
CGEventRef
myCGEventCallback(CGEventTapProxy proxy, CGEventType type,
                  CGEventRef event, void *refcon)
{
  bool suppress = false;

  if ((type == kCGEventKeyDown) ||
      (type == kCGEventKeyUp)) {

    // The incoming keycode.
    CGKeyCode keycode =
      (CGKeyCode)CGEventGetIntegerValueField(event, kCGKeyboardEventKeycode);

    printf("%d %s\n", keycode, type == kCGEventKeyUp ? "up" : "down");
    // printf("Event thread: %d\n", pthread_mach_thread_np(pthread_self()));


    /* // Switch 'a' for 'b' (keycode=0) and make 'z' into 'a' (keycode=6). */
    /* if (keycode == (CGKeyCode)0) { */
    /*   // keycode = (CGKeyCode)6; */
    /*   // Chuck a 'b' in there */
    /*   CGEventRef evt = CGEventCreateKeyboardEvent(NULL, (CGKeyCode) 1, type == kCGEventKeyDown); */
    /*   CGEventPost(kCGHIDEventTap, evt); */
    /*   CFRelease(evt); */
    /*   /\* CGEventRef evt2 = CGEventCreateKeyboardEvent(NULL, (CGKeyCode) 1, type == kCGEventKeyDown); *\/ */
    /*   /\* CGEventPost(kCGHIDEventTap, evt2); *\/ */
    /*   /\* CFRelease(evt2); *\/ */
    /*   suppress = true; */
    /* } */
    /* else if (keycode == (CGKeyCode)6) */
    /*     keycode = (CGKeyCode)0; */

    /* // Set the modified keycode field in the event. */
    /* CGEventSetIntegerValueField( */
    /*     event, kCGKeyboardEventKeycode, (int64_t)keycode); */

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
    printf("EventFlagsChanged: 0x%x (%s %s %s %s %s %s %s %s)\n",
           flags,
           (flags & NX_DEVICELCTLKEYMASK) ? "lcontrol" : "",
           (flags & NX_DEVICERCTLKEYMASK) ? "rcontrol" : "",
           (flags & NX_DEVICELSHIFTKEYMASK) ? "lshift" : "",
           (flags & NX_DEVICERSHIFTKEYMASK) ? "rshift" : "",
           (flags & NX_DEVICELCMDKEYMASK) ? "lcommand" : "",
           (flags & NX_DEVICERCMDKEYMASK) ? "rcommand" : "",
           (flags & NX_DEVICELALTKEYMASK) ? "lalt" : "",
           (flags & NX_DEVICERALTKEYMASK) ? "ralt" : "");

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

  // Create an event tap. We are interested in key presses.
  eventMask = ((1 << kCGEventKeyDown) |
               (1 << kCGEventKeyUp) |
               (1 << kCGEventFlagsChanged));
  eventTap = CGEventTapCreate(kCGSessionEventTap, kCGHeadInsertEventTap, 0,
                              eventMask, myCGEventCallback, NULL);
  if (!eventTap) {
    fprintf(stderr, "failed to create event tap\n");
    exit(1);
  }

  // Create a run loop source.
  runLoopSource = CFMachPortCreateRunLoopSource(
                                                kCFAllocatorDefault, eventTap, 0);

  // Add to the current run loop.
  CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource,
                     kCFRunLoopCommonModes);

  // Enable the event tap.
  CGEventTapEnable(eventTap, true);

  printf("Starting\n");

  printf("Main thread: %d\n", pthread_mach_thread_np(pthread_self()));

  // Set it all running.
  CFRunLoopRun();

  // In a real program, one would have arranged for cleaning up.

  exit(0);
}
