package org.postronic.h3.upnp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Description {
    
    public static DescriptionResponse requestDescription(URL descriptionURL) throws IOException, ParserConfigurationException, SAXException {
        URLConnection urlConnection = descriptionURL.openConnection();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(urlConnection.getInputStream());
        //System.out.println(Utils.getPrettyPrintXML(doc));
        Element rootElem = doc.getDocumentElement();
        if (rootElem != null && "root".equalsIgnoreCase(rootElem.getNodeName())) {
            DescriptionResponse descriptionResponse = new DescriptionResponse(descriptionURL, doc);
            return descriptionResponse;
        } else {
            return null;
        }
    }

}
