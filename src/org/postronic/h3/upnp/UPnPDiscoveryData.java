package org.postronic.h3.upnp;

import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.midi.Receiver;

public class UPnPDiscoveryData {
    
    private final SocketAddress receiverAddress;
    private final Map<String, String> headers = new LinkedHashMap<String, String>();
    
    public UPnPDiscoveryData(SocketAddress senderAddress, SocketAddress receiverAddress, Map<String, String> responseHeaders) {
        this.receiverAddress = receiverAddress;
        headers.putAll(responseHeaders);
    }
    
    public SocketAddress getReceiverAddress() {
        return receiverAddress;
    }
    
    public URL getLocation() {
        String location = headers.get("location");
        if (location == null) { return null; }
        try {
            return new URL(location);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public String getSearchTarget() {
        return headers.get("st");
    }
    
    public String getUniqueServiceName() {
        return headers.get("usn");
    }

}
