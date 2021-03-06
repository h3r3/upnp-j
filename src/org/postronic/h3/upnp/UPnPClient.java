package org.postronic.h3.upnp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.postronic.h3.upnp.impl.UPnPDiscoveryImpl;
import org.postronic.h3.upnp.impl.UPnPDiscoveryImpl.Callback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UPnPClient {
    
    public Future<List<UPnPDiscoveryData>> discover(InetSocketAddress bindInetSocketAddress, UPnPUserAgent userAgent, final int timeoutSeconds, Callback discoveryCallback) throws IOException {
        UPnPDiscoveryImpl discovery = new UPnPDiscoveryImpl(bindInetSocketAddress);
        discovery.start(userAgent, timeoutSeconds, discoveryCallback);
        return discovery;
    }
    
    public UPnPDescriptionData describe(URL descriptionURL) throws IOException, ParserConfigurationException, SAXException {
        URLConnection urlConnection = descriptionURL.openConnection();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(urlConnection.getInputStream());
        //System.out.println(Utils.getPrettyPrintXML(doc));
        Element rootElem = doc.getDocumentElement();
        if (rootElem != null && "root".equalsIgnoreCase(rootElem.getNodeName())) {
            UPnPDescriptionData descriptionResponse = new UPnPDescriptionData(descriptionURL, doc);
            return descriptionResponse;
        } else {
            return null;
        }
    }
    
    public Map<String, String> control(UPnPServiceData service, UPnPUserAgent userAgent, String actionName, Map<String, String> inParams) {
        URLConnection urlConnection = null;
        HttpURLConnection con = null;
        try {
            URL controlURL = service.getControlURL();
            
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\"?>\r\n");
            sb.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n");
            sb.append("<s:Body>\r\n");
            sb.append("<u:").append(actionName).append(" xmlns:u=\"").append(service.getServiceType()).append("\">\r\n");
            if (inParams != null) {
                for (Map.Entry<String, String> entry : inParams.entrySet()) {
                    sb.append("<").append(entry.getKey()).append(">").append(entry.getValue()).append("</").append(entry.getKey()).append(">\r\n");
                }
            }
            sb.append("</u:").append(actionName).append(">\r\n");
            sb.append("</s:Body>\r\n");
            sb.append("</s:Envelope>\r\n");
            //System.out.println(sb);
            byte[] data = sb.toString().getBytes(Charset.forName("UTF-8"));
            
            urlConnection = controlURL.openConnection();
            if (urlConnection instanceof HttpURLConnection) {
                con = (HttpURLConnection) urlConnection;
                con.setRequestMethod("POST");
                int controlURLPort = controlURL.getPort();
                con.setRequestProperty("HOST", controlURL.getHost() + (controlURLPort > 0 ? ":" + controlURLPort : ""));
                con.setRequestProperty("CONTENT-LENGTH", Integer.toString(data.length));
                con.setRequestProperty("CONTENT-TYPE", "text/xml; charset=\"utf-8\"");
                if (userAgent != null) {
                    con.setRequestProperty("USER-AGENT", userAgent.toString());
                }
                con.setRequestProperty("SOAPACTION", service.getServiceType() + "#" + actionName);
                con.setUseCaches (false);
                con.setDoInput(true);
                con.setDoOutput(true);
                OutputStream os = con.getOutputStream();
                os.write(data);
                os.flush();
                os.close();
                // Get response  
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(con.getInputStream());
                con.getInputStream().close();
                //System.out.println(Utils.getPrettyPrintXML(doc));
                Map<String, String> outParams = new LinkedHashMap<String, String>();
                NodeList childNodes = doc.getDocumentElement().getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    if (childNode instanceof Element) {
                        Element childElement = (Element) childNode;
                        if (childElement.getTagName().toUpperCase().endsWith("BODY")) {
                            NodeList childNodes2 = childElement.getChildNodes();
                            for (int j = 0; j < childNodes2.getLength(); j++) {
                                Node childNode2 = childNodes2.item(j);
                                if (childNode2 instanceof Element) {
                                    Element childElement2 = (Element) childNode2;
                                    NodeList childNodes3 = childElement2.getChildNodes();
                                    for (int k = 0; k < childNodes3.getLength(); k++) {
                                        Node childNode3 = childNodes3.item(k);
                                        if (childNode3 instanceof Element) {
                                            Element childElement3 = (Element) childNode3;
                                            outParams.put(childElement3.getTagName(), childElement3.getTextContent());
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                return outParams;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return null;
    }

}
