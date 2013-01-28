#include "kintext.h"
#include <IOKit/IOLib.h>

#define super IOService

OSDefineMetaClassAndStructors(org_craigandera_driver_kintext, IOService)

org_craigandera_driver_kintext* kintext;
KeyboardEventAction originalKeyboardEventAction;
KeyboardSpecialEventAction originalKeyboardSpecialEventAction;
OSArray* hookedKeyboards;

bool org_craigandera_driver_kintext::start(IOService* provider)
{
  bool res = super::start(provider);
  IOLog("kintext::start\n");

  if (!res) {
    IOLog("kintext:: Superclass initialization failed\n");
    return res;
  }

  kintext = this;
  hookedKeyboards = new OSArray();
  hookedKeyboards->initWithCapacity(1);

  originalKeyboardEventAction = NULL;
  originalKeyboardSpecialEventAction = NULL;

  _notifier = addMatchingNotification(
                gIOPublishNotification,
                serviceMatching("IOHIKeyboard"),
                (IOServiceMatchingNotificationHandler) &org_craigandera_driver_kintext::onKeyboardPublished,
                this);

  // TODO: Do I need to release the dictionary returned by serviceMatching? The
  // docs for addMatchingNotification indicate that it differs from addMatching
  // in its retention of the matching dictionary, but I'm not quite sure when
  // it's OK to release it.

  // TODO: Add a termination notification so we can remove keyboards
  // from the hooked list

  return res;
}

void org_craigandera_driver_kintext::stop(IOService* provider)
{
  // TODO: Clean up _notifier

  IOLog("kintext:: Setting kintext pointer to NULL\n");
  kintext = NULL;

  unsigned int hookedKeyboardCount = hookedKeyboards->getCount();
  for (int i = 0; i < hookedKeyboardCount; ++i) {
    IOHIKeyboard* keyboard = (IOHIKeyboard*) hookedKeyboards->getObject(0);
    if (originalKeyboardEventAction) {
      IOLog("kintext:: Putting _keyboardEventAction back the way it was\n");
      keyboard->_keyboardEventAction = originalKeyboardEventAction;
    }

    if (originalKeyboardSpecialEventAction) {
      IOLog("kintext:: Putting _keyboardSpecialEventAction back the way it was\n");
      keyboard->_keyboardSpecialEventAction = originalKeyboardSpecialEventAction;
    }

    hookedKeyboards->removeObject(0);
    IOLog("kintext:: Keyboard has been unhooked\n");
  }

  originalKeyboardEventAction = NULL;
  originalKeyboardSpecialEventAction = NULL;

  hookedKeyboards->release();

  IOLog("kintext::stop\n");
  super::stop(provider);
}

void hookedKeyboardSpecialEventAction
(
  OSObject * target,
  unsigned   eventType,
  unsigned   flags,
  unsigned   key,
  unsigned   flavor,
  UInt64     guid,
  bool       repeat,
  AbsoluteTime ts)
{
  // if ((eventType==NX_SYSDEFINED)&&(!flags)&&(key==NX_NOSPECIALKEY))  // only sign of a logout (also thrown when sleeping)
  //   logService->clearKeyboards();

  IOLog("kintext:: Received keybaord special event: %d %d\n", key, eventType);

  if (originalKeyboardSpecialEventAction) {
    (*originalKeyboardSpecialEventAction)(target,eventType,flags,key,flavor,guid,repeat,ts);
  }
}


void hookedKeyboardEventAction
(
  OSObject * target,
  unsigned   eventType,
  unsigned   flags,
  unsigned   key,
  unsigned   charCode,
  unsigned   charSet,
  unsigned   origCharCode,
  unsigned   origCharSet,
  unsigned   keyboardType,
  bool       repeat,
  AbsoluteTime ts)
{
  IOLog("kintext:: Received keyboard event:\n..target 0x%lx\n..eventType %d\n..flags 0x%x\n..key %d\n..charCode %d\n..charSet %d\n..origCharCode %d\n..origCharSet %d\n..keyboardType %d\n..repeat %s\n..ts %ld\n",
        (unsigned long int) target,
        eventType,
        flags,
        key,
        charCode,
        charSet,
        origCharCode,
        origCharSet,
        keyboardType,
        repeat ? "repeat" : "non-repeat",
        (long int) ts);

  unsigned int hookedKeyboardCount = hookedKeyboards->getCount();
  for (int i = 0; i < hookedKeyboardCount; ++i) {
    if (target == hookedKeyboards->getObject(i)) {
      IOLog("Event target is keyboard %d\n", i);
    }
  }

  if (originalKeyboardEventAction) {
    IOLog("Before forwarding keystroke\n");

    // HACK: Trying to get the kernel log to flush
    //IOSleep(100);

    if (key == 0) {
      // TODO: Which one should we use? When we send them through
      // keyboard 1, which is an instance of IOHidConsumer, it seems
      // to work, in the sense that events are sent. But the events
      // appear to be for "volume down", so I think the keycodes are
      // completely different from what I'm getting here. Also, we're
      // still crashing at unload time.
      // TODO: Make this a loop where we look for IOHIDConsumer
      // Update: I think we actually want the other one: IOHIDKeyboard
      IOHIKeyboard* keyboard = OSDynamicCast(IOHIKeyboard, hookedKeyboards->getObject(0));

      if (keyboard) {
        IOLog("Sending down key\n");
        // kHIDPage_KeyboardOrKeypad == 0x07
        keyboard->dispatchKeyboardEvent(2, true, ts);
        IOLog("Sending up key\n");
        keyboard->dispatchKeyboardEvent(2, false, ts);
        IOLog("Done sending down/up\n");
      }
    }
    (*originalKeyboardEventAction)(target,eventType,flags,key,charCode,charSet,origCharCode,origCharSet,keyboardType,repeat,ts);
  }
}

bool org_craigandera_driver_kintext::onKeyboardPublished(void *target,
                                                         void *ref,
                                                         IOService *newServ,
                                                         IONotifier* notifier)
{
  org_craigandera_driver_kintext* self = OSDynamicCast(org_craigandera_driver_kintext,
                                                       (OSMetaClassBase*)target);
  if (!self) {
    return false;
  }

  IOLog("kintext:: Keyboard publication notification handler called\n");

  IOHIKeyboard* keyboard = OSDynamicCast(IOHIKeyboard, newServ);
  if (!keyboard) {
    return false;
  }

  IOLog("kintext:: Keyboard is of class %s\n", keyboard->getMetaClass()->getClassName());

  IOLog("kintext:: Keyboard is named %s\n", keyboard->getName());

  if (!keyboard->_keyboardEventTarget) {
    IOLog("kintext:: No Keyboard event target\n");
    return false;
  }
  // event target must be IOHIDSystem

  IOService* targetServ = OSDynamicCast(IOService, keyboard->_keyboardEventTarget);
  if (targetServ) {
    IOLog("kintext:: Keyboard event target is %s\n", targetServ->getName());
  }

  if (!keyboard->_keyboardEventTarget->metaCast("IOHIDSystem")) {
    return false;
  }

  // We have a valid keyboard
  IOLog("kintext:: Found keyboard %lx\n", (unsigned long) keyboard );

  // TODO: This seems very wrong, as we're recording each keyboard
  // that we find, but assuming the original action fields are the
  // same value everywhere. It makes more sense to me that we'd record
  // not only the keyboard but the original values, and restore them
  // each as we go through the unregistration process. For now, we'll
  // do it like this, since logKext does the same thing, and my iOS-fu
  // is weak.
  hookedKeyboards->setObject(keyboard);

  originalKeyboardEventAction = keyboard->_keyboardEventAction;
  keyboard->_keyboardEventAction = (KeyboardEventAction) hookedKeyboardEventAction;

  originalKeyboardSpecialEventAction = keyboard->_keyboardSpecialEventAction;
  keyboard->_keyboardSpecialEventAction = (KeyboardSpecialEventAction) hookedKeyboardSpecialEventAction;

  IOLog("kintext:: Keyboard has been hooked for a total of %d.\n",
        hookedKeyboards->getCount());

  return true;
}
