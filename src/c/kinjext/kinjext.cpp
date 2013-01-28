#include "kinjext.h"
#include <IOKit/IOLib.h>

#define super IOHIKeyboard

OSDefineMetaClassAndStructors(org_craigandera_driver_kinjext, IOHIKeyboard)

bool org_craigandera_driver_kinjext::start(IOService* provider) 
{
	bool res = super::start(provider);
	IOLog("kinjext::start\n");
	IOLog("kinjext::start: provider class is: %s\n", provider->getName());
    return res;	
}

void org_craigandera_driver_kinjext::stop(IOService* provider)
{
	IOLog("kinjext::stop\n");
	IOLog("kinjext::stop: provider class is: %s\n", provider->getName());
}

void org_craigandera_driver_kinjext::sendKey(unsigned int keyCode, bool goingDown)
{
}