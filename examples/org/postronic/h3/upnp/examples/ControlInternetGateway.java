package org.postronic.h3.upnp.examples;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import org.postronic.h3.upnp.Control;
import org.postronic.h3.upnp.Description;
import org.postronic.h3.upnp.DescriptionResponse;
import org.postronic.h3.upnp.Device;
import org.postronic.h3.upnp.Discovery;
import org.postronic.h3.upnp.DiscoveryResponse;
import org.postronic.h3.upnp.Service;
import org.postronic.h3.upnp.impl.UPnPImplUtils;

public class ControlInternetGateway {
    
    public static void main(String[] args) {
        try {
//            String hostName = InetAddress.getLocalHost().getHostName();
//            System.out.println(hostName);
//            InetAddress[] localAddresses = InetAddress.getAllByName(hostName);
//            for (InetAddress inetAddress : localAddresses) {
//                System.out.println(inetAddress.getHostAddress());
//            }
            
            Discovery discovery = new Discovery(new InetSocketAddress("192.168.0.14", 0));;
            discovery.start(new Discovery.Callback() {
                @Override
                public void onDiscoveryResponse(Discovery discovery, DiscoveryResponse discoveryResponse) {
                    if ("upnp:rootdevice".equalsIgnoreCase(discoveryResponse.getSearchTarget())) {
                        System.out.println("Discovered: " + discoveryResponse.getLocation());
                        Description description = new Description(discoveryResponse.getLocation());
                        description.addListener(new Description.Listener() {
                            @Override
                            public void onDescriptionResponse(DescriptionResponse res) {
                                Device device = res.getDevice();
                                Service service = findWANIPConnectionService(device);
                                if (service != null) {
                                    Control control = new Control(service);
                                    Map<String, String> outParams = control.sendSOAPRequest("GetExternalIPAddress", null);
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
                                    Map<String, String> outParams2 = control.sendSOAPRequest("AddPortMapping", inParams2);
                                    System.out.println(outParams2);
                                    
                                    for (int i = 0; i < 5; i++) {
                                        Map<String, String> inParams3 = new LinkedHashMap<String, String>();
                                        inParams3.put("NewPortMappingIndex", "" + i);
                                        Map<String, String> outParams3 = control.sendSOAPRequest("GetGenericPortMappingEntry", inParams3);
                                        System.out.println(i + ": " + outParams3);
                                    }
                                    
                                }
                                dumpDevice(0, device);
                            }
                            private Service findWANIPConnectionService(Device device) {
                                if ("InternetGatewayDevice".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(device.getDeviceType()))) {
                                    for (Device subDevice1 : device.getDeviceList()) {
                                        if ("WANDevice".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(subDevice1.getDeviceType()))) {
                                            for (Device subDevice2 : subDevice1.getDeviceList()) {
                                                if ("WANConnectionDevice".equalsIgnoreCase(UPnPImplUtils.extractURNShortName(subDevice2.getDeviceType()))) {
                                                    for (Service service : subDevice2.getServiceList()) {
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
                            private void dumpDevice(int indent, Device device) {
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
                        });
                        description.start();
                    }
                }
                @Override
                public void onDiscoveryTerminated(Discovery discovery) {
                    System.out.println("Discovery terminated");
                }
            });
            System.out.println("started");
            Thread.sleep(10000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
