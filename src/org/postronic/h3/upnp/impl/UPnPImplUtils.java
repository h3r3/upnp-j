package org.postronic.h3.upnp.impl;

import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class UPnPImplUtils {
    
    private UPnPImplUtils() { }
    
    public static String getXmlParam(Element element, String paramName) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                Element childElement = (Element) childNode;
                if (paramName.equalsIgnoreCase(childElement.getTagName())) {
                    return childElement.getTextContent();
                }
            }
        }
        return null;
    }
    
    public static String getUserAgent(String productName, String productVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append("USER-AGENT: ");
        sb.append(System.getProperty("os.name").replace(" ", "-")).append("/").append(System.getProperty("os.version").replace(" ", "-"));
        sb.append(" UPnP/1.1 ");
        sb.append(productName.replace(" ", "-")).append("/").append(productVersion.replace(" ", "-"));
        return sb.toString();
    }
    
    public static String extractURNShortName(String urn) {
        String[] urnParts = urn.split(":");
        if (urnParts.length >= 2) {
            return urnParts[urnParts.length - 2];
        } else {
            return null;
        }
    }
    
    public static String getPrettyPrintXML(Document doc) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            return xmlString;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static InetSocketAddress getDiscoveryInetSocketAddress(InetAddress inetAddress) {
        InetAddress discoveryInetAddress = null;
        try {
            if (inetAddress instanceof Inet4Address) {   
                discoveryInetAddress = Inet4Address.getByName("239.255.255.250");
            } else if (inetAddress instanceof Inet6Address) {
                discoveryInetAddress = Inet6Address.getByName("FF02::C");
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return new InetSocketAddress(discoveryInetAddress, 1900);
    }

}
