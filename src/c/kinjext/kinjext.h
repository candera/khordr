/* TODO: Copyright notice. License EPL, copyright Craig Andera 2012. */

#include <IOKit/IOService.h>

#include <IOKit/hidsystem/IOHIKeyboard.h>

class org_craigandera_driver_kinjext : public IOHIKeyboard
{
	OSDeclareDefaultStructors(org_craigandera_driver_kinjext)
		
public:
	virtual bool start(IOService* provider);
	virtual void stop(IOService* provider);
	
private:
	void sendKey(unsigned int keyCode, bool goingDown);
};
