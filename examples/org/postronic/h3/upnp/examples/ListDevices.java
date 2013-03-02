package org.postronic.h3.upnp.examples;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import org.postronic.h3.upnp.UPnPDescriptionData;
import org.postronic.h3.upnp.UPnPDeviceData;
import org.postronic.h3.upnp.UPnPDiscoveryData;
import org.postronic.h3.upnp.UPnPServiceData;
import org.postronic.h3.upnp.UPnPClient;
import org.postronic.h3.upnp.UPnPUserAgent;
import org.postronic.h3.upnp.impl.UPnPDiscoveryImpl;

public class ListDevices implements UPnPDiscoveryImpl.Callback {
    
    private final UPnPClient uPnPClient = new UPnPClient();
    
    public void listDevices() throws Throwable {
        UPnPUserAgent userAgent = new UPnPUserAgent("ListDevices Example", "1.0");
        String canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
        InetAddress[] localAddresses = InetAddress.getAllByName(canonicalHostName);
        for (InetAddress inetAddress : localAddresses) {
            try {
                System.out.println("Starting discovery on " + inetAddress.getHostAddress());
                Future<List<UPnPDiscoveryData>> discovery = uPnPClient.discover(new InetSocketAddress(inetAddress, 0), userAgent, 4, this);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }        
        System.out.println("Discovery started on all local interfaces");
    }
    
    @Override
    public void onDiscoveryResponse(final UPnPDiscoveryData discoveryResponse) {
        if ("upnp:rootdevice".equalsIgnoreCase(discoveryResponse.getSearchTarget())) {
            System.out.println("\nDiscovered: " + discoveryResponse.getLocation() + " on " + discoveryResponse.getReceiverAddress());
            Thread descThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        UPnPDescriptionData descriptionResponse = uPnPClient.describe(discoveryResponse.getLocation());
                        if (descriptionResponse != null) {
                            UPnPDeviceData device = descriptionResponse.getDevice();
                            dumpDevice(0, device);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }, "Description thread");
            descThread.setDaemon(false);
            descThread.start();
        }
    }
    
    private static void dumpDevice(int indent, UPnPDeviceData device) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) { sb.append(" "); }
        System.out.println(sb.toString() + device.getDeviceType() + " " + device.getFriendlyName());
        for (UPnPServiceData service : device.getServiceList()) {
            System.out.println(sb.toString() + service.getServiceType() + " " + service.getControlURL());
        }
        for (UPnPDeviceData subDevice : device.getDeviceList()) {
            dumpDevice(indent + 1, subDevice);
        }
    }
    
    public static void main(String[] args) {
        try {
            new ListDevices().listDevices();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
