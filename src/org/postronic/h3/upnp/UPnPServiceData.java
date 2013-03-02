package org.postronic.h3.upnp;

import java.net.URL;

import org.postronic.h3.upnp.impl.UPnPImplUtils;
import org.w3c.dom.Element;

public class UPnPServiceData {
    
    private final UPnPDeviceData device;
    private final Element serviceElem;
    
    public UPnPServiceData(UPnPDeviceData device, Element serviceElem) {
        this.device = device;
        this.serviceElem = serviceElem;
    }
    
    public String getServiceType() {
        return UPnPImplUtils.getXmlParam(serviceElem, "serviceType");
    }
    
    public URL getControlURL() {
        String controlURLStr = UPnPImplUtils.getXmlParam(serviceElem, "controlURL");
        URL baseURL = device.getURLBase();
        if (baseURL == null) { baseURL = device.getDescriptionURL(); }
        try {
            return new URL(baseURL, controlURLStr);
        } catch (Throwable e) {
            return null;
        }
    }

}
