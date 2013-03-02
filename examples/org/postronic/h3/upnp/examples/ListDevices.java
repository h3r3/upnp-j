package org.postronic.h3.upnp.examples;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.postronic.h3.upnp.Description;
import org.postronic.h3.upnp.DescriptionResponse;
import org.postronic.h3.upnp.Device;
import org.postronic.h3.upnp.Discovery;
import org.postronic.h3.upnp.DiscoveryResponse;
import org.postronic.h3.upnp.Service;
import org.postronic.h3.upnp.UserAgent;

public class ListDevices implements Discovery.Callback {
    
    public void listDevices() throws Throwable {
        UserAgent userAgent = new UserAgent("ListDevices Example", "1.0");
        String canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
        InetAddress[] localAddresses = InetAddress.getAllByName(canonicalHostName);
        for (InetAddress inetAddress : localAddresses) {
            try {
                System.out.println("Starting discovery on " + inetAddress.getHostAddress());
                Discovery discovery = new Discovery(new InetSocketAddress(inetAddress, 0));
                discovery.setUserAgent(userAgent);
                discovery.setTimeoutSeconds(4);
                discovery.start(this);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }        
        System.out.println("Discovery started on all local interfaces");
    }
    
    @Override
    public void onDiscoveryResponse(Discovery discovery, final DiscoveryResponse discoveryResponse) {
        if ("upnp:rootdevice".equalsIgnoreCase(discoveryResponse.getSearchTarget())) {
            System.out.println("\nDiscovered: " + discoveryResponse.getLocation() + " on " + discovery.getBindInetSocketAddress());
            Thread descThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DescriptionResponse descriptionResponse = Description.requestDescription(discoveryResponse.getLocation());
                        if (descriptionResponse != null) {
                            Device device = descriptionResponse.getDevice();
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
    
    private static void dumpDevice(int indent, Device device) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) { sb.append(" "); }
        System.out.println(sb.toString() + device.getDeviceType() + " " + device.getFriendlyName());
        for (Service service : device.getServiceList()) {
            System.out.println(sb.toString() + service.getServiceType() + " " + service.getControlURL());
        }
        for (Device subDevice : device.getDeviceList()) {
            dumpDevice(indent + 1, subDevice);
        }
    }

    @Override
    public void onDiscoveryTerminated(Discovery discovery) {
        System.out.println("Discovery on " + discovery.getBindInetSocketAddress() + " terminated");
    }
    
    public static void main(String[] args) {
        try {
            new ListDevices().listDevices();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
