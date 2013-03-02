package org.postronic.h3.upnp;

import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UPnPDescriptionData {
    
    private final Document document;
    private UPnPDeviceData device;
    
    public UPnPDescriptionData(URL descriptionURL, Document document) {
        this.document = document;
        NodeList childNodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                Element childElem = (Element) childNode;
                if ("device".equalsIgnoreCase(childElem.getTagName())) {                    
                    device = new UPnPDeviceData(descriptionURL, childElem);
                    break;
                }
            }
        }
    }
    
    public UPnPDeviceData getDevice() {
        return device;
    }

}
