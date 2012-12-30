/* TODO: Copyright notice. License EPL, copyright Craig Andera 2012. */

#include <IOKit/IOService.h>

/* BEGIN EGREGIOUS HACK */
#define private public
#define protected public
#include <IOKit/hidsystem/IOHIKeyboard.h>
#undef private
#undef protected
/* END EGREGIOUS HACK */


class org_craigandera_driver_kintext : public IOService	
{
	OSDeclareDefaultStructors(org_craigandera_driver_kintext)

private:
	IONotifier* _notifier;
	static bool onKeyboardPublished(void *target, void *ref, IOService *newServ, IONotifier* notifier);
	
public:
	virtual bool start(IOService* provider);
	virtual void stop(IOService* provider);
};
