package org.postronic.h3.upnp;

import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscoveryResponse {
    
    private final Map<String, String> headers = new LinkedHashMap<String, String>();
    
    public DiscoveryResponse(SocketAddress senderAddress, SocketAddress receiverAddress, Map<String, String> responseHeaders) {
        headers.putAll(responseHeaders);
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
