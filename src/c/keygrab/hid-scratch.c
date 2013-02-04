/* This code works with the IOHID library to get notified of keys.
   Still haven't figured out how to truly intercept with
   substitution. */

#include <IOKit/hid/IOHIDValue.h>
#include <IOKit/hid/IOHIDManager.h>

void myHIDKeyboardCallback(void *context, IOReturn result, void *sender,
        IOHIDValueRef value) {
  LOG("A");
  IOHIDElementRef elem = IOHIDValueGetElement(value);
  LOG("B");
  if (IOHIDElementGetUsagePage(elem) != 0x07)
    return;
  LOG("C");
  uint32_t scancode = IOHIDElementGetUsage(elem);
  LOG("D");
  if (scancode < 4 || scancode > 231)
    return;
  LOG("E");
  long pressed = IOHIDValueGetIntegerValue(value);
  printf("scancode: %d, pressed: %ld\n", scancode, pressed);
}

CFMutableDictionaryRef myCreateDeviceMatchingDictionary(UInt32 usagePage,
                                                        UInt32 usage) {
  LOG("1");
  CFMutableDictionaryRef ret = 
    CFDictionaryCreateMutable(kCFAllocatorDefault,
                              0, &kCFTypeDictionaryKeyCallBacks,
                              &kCFTypeDictionaryValueCallBacks);
  LOG("2");
  if (!ret)
    return NULL;

  CFNumberRef pageNumberRef = CFNumberCreate(kCFAllocatorDefault,
                                             kCFNumberIntType, &usagePage );
  LOG("3");
  if (!pageNumberRef) {
    CFRelease(ret);
    return NULL;
  }

  LOG("4");
  CFDictionarySetValue(ret, CFSTR(kIOHIDDeviceUsagePageKey), pageNumberRef);
  CFRelease(pageNumberRef);

  CFNumberRef usageNumberRef = CFNumberCreate(kCFAllocatorDefault,
                                              kCFNumberIntType, &usage);

  LOG("5")

  if (!usageNumberRef) {
    CFRelease(ret);
    return NULL;
  }

  LOG("6");

  CFDictionarySetValue(ret, CFSTR(kIOHIDDeviceUsageKey), usageNumberRef);
  CFRelease(usageNumberRef);

  LOG("7");

  return ret;
}

void whatever(void)
{
  LOG("a")
  IOHIDManagerRef hidManager = IOHIDManagerCreate(kCFAllocatorDefault,
        kIOHIDOptionsTypeNone);

  CFMutableDictionaryRef keyboard =
    myCreateDeviceMatchingDictionary(0x01, 6);
  CFMutableDictionaryRef keypad =
    myCreateDeviceMatchingDictionary(0x01, 7);

  CFMutableDictionaryRef matchesList[] = {
    keyboard,
    keypad,
  };
  CFArrayRef matches = CFArrayCreate(kCFAllocatorDefault,
                                     (const void **)matchesList, 2, NULL);
  IOHIDManagerSetDeviceMatchingMultiple(hidManager, matches);

  LOG("b")
  IOHIDManagerRegisterInputValueCallback(hidManager,
                                         myHIDKeyboardCallback, NULL);

  IOHIDManagerScheduleWithRunLoop(hidManager, CFRunLoopGetMain(),
                                  kCFRunLoopDefaultMode);

  LOG("c")
  IOHIDManagerOpen(hidManager, kIOHIDOptionsTypeNone);

  LOG("d")

}
