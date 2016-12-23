package org.area515.resinprinter.discover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.resource.DeviceDescriptorResource;
import org.fourthline.cling.model.resource.IconResource;
import org.fourthline.cling.model.resource.Resource;
import org.fourthline.cling.model.resource.ServiceControlResource;
import org.fourthline.cling.model.resource.ServiceDescriptorResource;
import org.fourthline.cling.model.resource.ServiceEventSubscriptionResource;
import org.fourthline.cling.model.types.DeviceType;

public class PhotonicDevice extends LocalDevice {
    public PhotonicDevice(DeviceIdentity identity, DeviceType type, DeviceDetails details,
            Icon[] icons, LocalService[] services) throws ValidationException {
    		super(identity, type, details, icons, services);
    }

	@Override
	public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();
        return errors;
	}
}
