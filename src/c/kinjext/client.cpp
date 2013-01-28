/*
 *  client.cpp
 *  kinjext
 *
 *  Created by Craig Andera on 1/27/13.
 *  Copyright 2013 __MyCompanyName__. All rights reserved.
 *
 */

#include "client.h"

// Define the superclass. 
#define super IOUserClient 

OSDefineMetaClassAndStructors(org_craigandera_driver_kinjext_client, IOUserClient) 

bool org_craigandera_driver_kinjext_client::initWithTask (task_t owningTask, void* securityToken, UInt32 type, OSDictionary* properties) 
{ 
	IOLog("kinjext_client::initWithTask\n");
	
	if (!owningTask)
		return false;
	
	if (! super::initWithTask(owningTask, securityToken , type, properties))
		return false;

	_task = owningTask; 
	
	// Optional:  Determine whether the calling process has admin privileges. 
	IOReturn ret = clientHasPrivilege(securityToken, kIOClientPrivilegeAdministrator); 
	if ( ret == kIOReturnSuccess ) {
		// m_taskIsAdmin = true; 
	} 
	
	return true; 
} 

bool org_craigandera_driver_kinjext_client::start (IOService* provider) 
{ 
	IOLog("kinjext_client::start\n");
	
	if (! super::start(provider)) 
		return false; 
	
	_driver = OSDynamicCast(org_craigandera_driver_kinjext, provider); 
	
	if (!_driver)
		return false; 
	
	return true; 
} 

void org_craigandera_driver_kinjext_client::stop(IOService* provider)
{
	IOLog("kinjext_client::stop\n");
	super::stop(provider);
}

void org_craigandera_driver_kinjext_client::free()
{
	IOLog("kinjext_client::free\n");
	super::free();
}

IOReturn org_craigandera_driver_kinjext_client::clientClose (void) 
{
	IOLog("kinjext_client::clientClose\n");
	terminate();
	return kIOReturnSuccess; 
}