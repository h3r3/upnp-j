package org.postronic.h3.upnp.examples;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.postronic.h3.upnp.Description;
import org.postronic.h3.upnp.DescriptionResponse;
import org.postronic.h3.upnp.Device;
import org.postronic.h3.upnp.Discovery;
import org.postronic.h3.upnp.DiscoveryResponse;
import org.postronic.h3.upnp.Service;
import org.postronic.h3.upnp.Description.Listener;

public class ListDevices implements Discovery.Listener, Description.Listener {
    
    private static final String PRODUCT_NAME = "Test";
    private static final String PRODUCT_VERSION = "1.0";
    
    public void run() throws Throwable {
        String hostName = InetAddress.getLocalHost().getHostName();
        System.out.println(hostName);
        InetAddress[] localAddresses = InetAddress.getAllByName(hostName);
        for (InetAddress inetAddress : localAddresses) {
            try {
                System.out.println("Discovery on " + inetAddress.getHostAddress());
                Discovery discovery = new Discovery(new InetSocketAddress(inetAddress, 2222));
                discovery.addListener(this);
                discovery.start();
                discovery.sendSSDP(PRODUCT_NAME, PRODUCT_VERSION);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }        
        System.out.println("All discovery requests sent");
    }
    
    @Override
    public void onDiscoveryResponse(DiscoveryResponse discoveryResponse) {
        if ("upnp:rootdevice".equalsIgnoreCase(discoveryResponse.getSearchTarget())) {
            System.out.println("\nDiscovered: " + discoveryResponse.getLocation());
            Description description = new Description(discoveryResponse.getLocation());
            description.addListener(this);
            description.start();
        }
    }

    @Override
    public void onDescriptionResponse(DescriptionResponse res) {
        Device device = res.getDevice();
        dumpDevice(0, device);
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

    public static void main(String[] args) {
        try {
            new ListDevices().run();
            Thread.sleep(10000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
