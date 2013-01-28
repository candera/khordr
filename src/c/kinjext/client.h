/*
 *  client.h
 *  kinjext
 *
 *  Created by Craig Andera on 1/27/13.
 *  Copyright 2013 __MyCompanyName__. All rights reserved.
 *
 */

#include <IOKit/IOUserClient.h>
#include "kinjext.h"

class org_craigandera_driver_kinjext_client : public IOUserClient 
{
	OSDeclareDefaultStructors(org_craigandera_driver_kinjext_client)
	
private: 
	task_t _task; 
	org_craigandera_driver_kinjext* _driver;
	
public:
	virtual bool initWithTask (task_t owningTask, void* securityToken, 
							   UInt32 type, OSDictionary* properties); 
	virtual bool start (IOService* provider);
	virtual IOReturn clientClose (void);
	virtual void stop (IOService* provider); 
	virtual void free (void); 
};