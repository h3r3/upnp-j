package org.postronic.h3.upnp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.postronic.h3.upnp.impl.UPnPImplUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UPnPDeviceData {
    
    private final URL descriptionURL;
    private final Element deviceElem;
    private final List<UPnPDeviceData> deviceList = new ArrayList<UPnPDeviceData>();
    private final List<UPnPServiceData> serviceList = new ArrayList<UPnPServiceData>();
    
    public UPnPDeviceData(URL descriptionURL, Element deviceElem) {
        this.descriptionURL = descriptionURL;
        this.deviceElem = deviceElem;
        NodeList childNodes = deviceElem.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                Element childElement = (Element) childNode;
                String tagName = childElement.getTagName();
                if ("deviceList".equalsIgnoreCase(tagName)) {
                    NodeList childNodes2 = childElement.getChildNodes();
                    for (int j = 0; j < childNodes2.getLength(); j++) {
                        Node childNode2 = childNodes2.item(j);
                        if (childNode2 instanceof Element) {
                            Element childElement2 = (Element) childNode2;
                            if ("device".equalsIgnoreCase(childElement2.getTagName())) {
                                deviceList.add(new UPnPDeviceData(descriptionURL, childElement2));
                            }
                        }
                    }
                } else if ("serviceList".equalsIgnoreCase(tagName)) {
                    NodeList childNodes2 = childElement.getChildNodes();
                    for (int j = 0; j < childNodes2.getLength(); j++) {
                        Node childNode2 = childNodes2.item(j);
                        if (childNode2 instanceof Element) {
                            Element childElement2 = (Element) childNode2;
                            if ("service".equalsIgnoreCase(childElement2.getTagName())) {
                                serviceList.add(new UPnPServiceData(this, childElement2));
                            }
                        }
                    }
                }
            }
        }
    }
    
    public URL getDescriptionURL() {
        return descriptionURL;
    }
    
    public List<UPnPDeviceData> getDeviceList() {
        return Collections.unmodifiableList(new ArrayList<UPnPDeviceData>(deviceList));
    }
    
    public List<UPnPServiceData> getServiceList() {
        return Collections.unmodifiableList(new ArrayList<UPnPServiceData>(serviceList));
    }
    
    public String getDeviceType() {
        return UPnPImplUtils.getXmlParam(deviceElem, "deviceType");
    }
    
    public String getFriendlyName() {
        return UPnPImplUtils.getXmlParam(deviceElem, "friendlyName");
    }
    
    public URL getURLBase() {
        String urlBase = UPnPImplUtils.getXmlParam(deviceElem, "URLBase");
        if (urlBase != null) {
            try {
                return new URL(urlBase);
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

}
