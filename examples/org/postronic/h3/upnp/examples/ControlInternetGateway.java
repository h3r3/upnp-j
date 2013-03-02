package org.postronic.h3.upnp.examples;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import org.postronic.h3.upnp.UPnPDescriptionData;
import org.postronic.h3.upnp.UPnPDeviceData;
import org.postronic.h3.upnp.UPnPDiscoveryData;
import org.postronic.h3.upnp.UPnPServiceData;
import org.postronic.h3.upnp.UPnPClient;
import org.postronic.h3.upnp.impl.UPnPDiscoveryImpl;
import org.postronic.h3.upnp.impl.UPnPImplUtils;

public class ControlInternetGateway implements UPnPDiscoveryImpl.Callback {
    
    private final UPnPClient uPnPClient = new UPnPClient();
    
    public void controlInternetGateway() throws IOException {
//      String hostName = InetAddress.getLocalHost().getHostName();
//      System.out.println(hostName);
//      InetAddress[] localAddresses = InetAddress.getAllByName(hostName);
//      for (InetAddress inetAddress : localAddresses) {
//          System.out.println(inetAddress.getHostAddress());
//      }
        uPnPClient.discover(new InetSocketAddress("192.168.0.14", 0), UPnPImplUtils.DEFAULT_USER_AGENT, 4, this);
    }

    @Override
    public void onDiscoveryResponse(UPnPDiscoveryData discoveryResponse) {
        if ("upnp:rootdevice".equalsIgnoreCase(discoveryResponse.getSearchTarget())) {
            System.out.println("Discovered: " + discoveryResponse.getLocation());
            UPnPDescriptionData descriptionResponse = null;
            try {
                descriptionResponse = uPnPClient.describe(discoveryResponse.getLocation());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (descriptionResponse != null) {
                UPnPDeviceData device = descriptionResponse.getDevice();
                UPnPServiceData service = findWANIPConnectionService(device);
                if (service != null) {
                    Map<String, String> outParams = uPnPClient.control(service, UPnPImplUtils.DEFAULT_USER_AGENT, "GetExternalIPAddress", null);
                    System.out.println(outParams);
                    
                    Map<String, String> inParams2 = new LinkedHashMap<String, String>();
                    inParams2.put("NewRemoteHost", "");
                    inParams2.put("NewExternalPort", "2222");
                    inParams2.put("NewProtocol", "TCP");
                    inParams2.put("NewInternalPort", "2222");
                    inParams2.put("NewInternalClient", "192.168.0.2");
                    inParams2.put("NewEnabled", "1");
                    inParams2.put("NewPortMappingDescription", "Just a test");
                    inParams2.put("NewLeaseDuration", "10");
                    Map<String, String> outParams2 = uPnPClient.control(service, UPnPImplUtils.DEFAULT_USER_AGENT, "AddPortMapping", inParams2);
                    System.out.println(outParams2);
                    
                    for (int i = 0; i < 5; i++) {
                        Map<String, String> inParams3 = new LinkedHashMap<String, String>();
                        inParams3.put("NewPortMappingIndex", "" + i);
                        Map<String, String> outParams3 = uPnPClient.control(service, UPnPImplUtils.DEFAULT_USER_AGENT, "GetGenericPortMappingEntry", inParams3);
                        System.out.println(i + ": " + outParams3);
                    }
                    
                }
                dumpDevice(0, device);
            }
        }
    }
    
    private UPnPServiceData findWANIPConnectionService(UPnPDeviceData device) {
        if ("InternetGatewayDevice".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(device.getDeviceType()))) {
            for (UPnPDeviceData subDevice1 : device.getDeviceList()) {
                if ("WANDevice".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(subDevice1.getDeviceType()))) {
                    for (UPnPDeviceData subDevice2 : subDevice1.getDeviceList()) {
                        if ("WANConnectionDevice".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(subDevice2.getDeviceType()))) {
                            for (UPnPServiceData service : subDevice2.getServiceList()) {
                                if ("WANIPConnection".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(service.getServiceType()))) {
                                    return service;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private void dumpDevice(int indent, UPnPDeviceData device) {
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

            System.out.println("started");
            Thread.sleep(10000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
