#include "kintext.h"
#include <IOKit/IOLib.h>

#define super IOService

OSDefineMetaClassAndStructors(org_craigandera_driver_kintext, IOService)

bool org_craigandera_driver_kintext::start(IOService* provider) 
{
	bool res = super::start(provider);
	IOLog("kintext::start\n");
	return res;
}

void org_craigandera_driver_kintext::stop(IOService* provider)
{
	IOLog("kintext::stop\n");
	super::stop(provider);
}


