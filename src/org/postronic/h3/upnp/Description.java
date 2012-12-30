package org.postronic.h3.upnp;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Description {
    
    private final URL descriptionURL;
    private final List<Listener> listeners = new ArrayList<Listener>(); 
    
    public interface Listener {
        void onDescriptionResponse(DescriptionResponse descriptionResponse);
    }
    
    public Description(URL location) {
        this.descriptionURL = location;
    }
    
    public synchronized boolean addListener(Listener listener) {
        return listeners.add(listener);
    }
    
    public synchronized boolean removeListener(Listener listener) {
        return listeners.remove(listener);
    }
    
    public void start() {
        try {
            URLConnection urlConnection = descriptionURL.openConnection();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(urlConnection.getInputStream());
            //System.out.println(Utils.getPrettyPrintXML(doc));
            Element rootElem = doc.getDocumentElement();
            if (rootElem != null && "root".equalsIgnoreCase(rootElem.getNodeName())) {
                DescriptionResponse descriptionResponse = new DescriptionResponse(descriptionURL, doc);
                List<Listener> listenersCopy = new ArrayList<Listener>();
                synchronized (this) { listenersCopy.addAll(listeners); }
                for (Listener listener : listenersCopy) {
                    try {
                        listener.onDescriptionResponse(descriptionResponse);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
    }

}
