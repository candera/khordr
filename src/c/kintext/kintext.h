/* TODO: Copyright notice. License EPL, copyright Craig Andera 2012. */

#include <IOKit/IOService.h>

class org_craigandera_driver_kintext : public IOService	
{
	OSDeclareDefaultStructors(org_craigandera_driver_kintext)
	
public:
	virtual bool start(IOService* provider);
	virtual void stop(IOService* provider);
};