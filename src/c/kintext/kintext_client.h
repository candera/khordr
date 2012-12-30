#include <IOKit/IOService.h>
#include <IOKit/IOUserClient.h>
#include "kintext.h"

class org_craigandera_driver_kintext_client : IOUserClient 
{
  OSDeclareDefaultStructors(org_craigandera_driver_kintext_client);

 protected:
  org_craigandera_driver_kintext* _provider;
  task_t _owningTask;
  
 public:
  virtual bool start(IOService* provider);
  virtual void stop(IOService* provider);

  virtual bool initWithTask(task_t owningTask,
                            void* securityID, 
                            UInt32 type,
                            OSDictionary* properties);
  virtual IOReturn clientClose();
  
};
