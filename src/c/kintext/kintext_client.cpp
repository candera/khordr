#include "kintext_client.h"
#include <IOKit/IOLib.h>

#define super IOUserClient

OSDefineMetaClassAndStructors(org_craigandera_driver_kintext_client, IOUserClient)

bool org_craigandera_driver_kintext_client::initWithTask(task_t owningTask,
                                                  void* securityID, 
                                                  UInt32 type,
                                                  OSDictionary* properties)
{
  if (!owningTask) {
    IOLog("kintext_client:: No owning task\n");
    return false;
  }

  if (!super::initWithTask(owningTask, securityID, type, properties)) {
    IOLog("kintext_client:: super's initWithTask failed\n");
    return false;
  }

  _owningTask = owningTask;

  // TODO: We're going to want this eventually, since the ability to
  // hook and create keyboard events at the hardware level should
  // require administrator privileges. But it's a pain in the ass at
  // this point, so comment it out.
  // IOReturn ret = clientHasPrivilege(securityToken, kIOClientPrivilegeAdministrator);
  // if (ret == kIOReturnSuccess) {
  //   _taskIsAdmin = true;
  // }

  IOLog("kintext_client:: initWithTask successful\n");
  return true;
}

bool org_craigandera_driver_kintext_client::start(IOService* provider)
{
  if (!super::start(provider)) {
    IOLog("kintext_client:: super failed to start\n");
    return false;
  }

  _provider = OSDynamicCast(org_craigandera_driver_kintext, provider);
  if (!_provider) {
    IOLog("kintext_client:: Provider was not the right type\n");
    return false;
  }

  IOLog("kintext_client:: start successful\n");
  return true;
}

void org_craigandera_driver_kintext_client::stop(IOService* provider)
{
  IOLog("kintext_client::stop\n");
  super::stop(provider);
}

IOReturn org_craigandera_driver_kintext_client::clientClose()
{
  IOLog("kintext_client:: clientClose\n");
  terminate();
  return kIOReturnSuccess;
}
