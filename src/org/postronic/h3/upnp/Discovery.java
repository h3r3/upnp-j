package org.postronic.h3.upnp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.postronic.h3.upnp.impl.UPnPImplUtils;

public class Discovery {
    
    private static final int MAX_MESSAGE_SIZE = 1024 * 64;
    
    private InetSocketAddress bindAddress;
    private InetSocketAddress discoveryAddress;
    private DatagramSocket socketUDP;
    private SocketUDPReceiverRunnable socketUDPReceiverRunnable;
    private Thread socketUDPReceiverThread;
    private boolean running;
    private final List<Listener> listeners = new ArrayList<Listener>(); 
    
    public interface Listener {
        void onDiscoveryResponse(DiscoveryResponse discoveryResponse);
    }
    
    public Discovery(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
        this.discoveryAddress = UPnPImplUtils.getDiscoveryInetSocketAddress(bindAddress.getAddress());
    }
    
    public void start() throws IOException {
        this.socketUDP = new DatagramSocket(null);
        this.socketUDP.bind(bindAddress);
        this.socketUDPReceiverRunnable = new SocketUDPReceiverRunnable();
        this.socketUDPReceiverThread = new Thread(socketUDPReceiverRunnable, "UPnP UDP receiver");
        this.socketUDPReceiverThread.start();
        running = true;
    }
    
    public synchronized boolean addListener(Listener listener) {
        return listeners.add(listener);
    }
    
    public synchronized boolean removeListener(Listener listener) {
        return listeners.remove(listener);
    }
    
    public void sendSSDP(String productName, String productVersion) throws IOException {
        ByteArrayOutputStream baos = null;
        try {
            String discoveryAddr = discoveryAddress.getAddress() instanceof Inet6Address ? "[" + discoveryAddress.getAddress().getHostAddress() + "]" : discoveryAddress.getAddress().getHostAddress();  
            baos = new ByteArrayOutputStream(512);            
            baos.write("M-SEARCH * HTTP/1.1\r\n".getBytes());
            baos.write(("HOST: " + discoveryAddr + ":" + discoveryAddress.getPort() + "\r\n").getBytes());
            baos.write("MAN: \"ssdp:discover\"\r\n".getBytes());
            baos.write("MX: 3\r\n".getBytes());
            baos.write("ST: upnp:rootdevice\r\n".getBytes());
            if (productName != null && productVersion != null) {
                baos.write(("USER-AGENT: " + UPnPImplUtils.getUserAgent(productName, productVersion) + "\r\n").getBytes());
            }
            baos.write("\r\n".getBytes());
            baos.flush();
            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, discoveryAddress);
            this.socketUDP.send(packet);
        } finally {
            if (baos != null) { try { baos.close(); } catch (Throwable e) { } }
        }
    }
    
    private final class SocketUDPReceiverRunnable implements Runnable {
        
        @Override
        public void run() {
            try {
                byte[] buf = new byte[MAX_MESSAGE_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, MAX_MESSAGE_SIZE);
                for (;;) {
                    socketUDP.receive(packet);
                    SocketAddress senderAddress = packet.getSocketAddress();
                    byte[] data = packet.getData();
                    
                    ByteArrayInputStream bais = null;
                    InputStreamReader isr = null;
                    BufferedReader br = null;
                    try {
                        bais = new ByteArrayInputStream(data);
                        isr = new InputStreamReader(bais, Charset.forName("ISO-8859-1"));
                        br = new BufferedReader(isr);
                        Map<String, String> responseHeaders = new LinkedHashMap<String, String>();
                        for (int numLine = 0; ; numLine++) {
                            String line = br.readLine();
                            if (line == null) { break; }
                            if (numLine == 0) {
                                if (!line.toUpperCase().startsWith("HTTP")) { break; } // Wrong first line 
                                if (line.indexOf(" 200 ", 4) == -1) { break; } // Wrong first line
                            } else {
                                line = line.trim();
                                if (line.isEmpty()) { break; } // End of headers
                                int separ = line.indexOf(":");
                                if (separ != -1) {
                                    String headerName = line.substring(0, separ).trim().toLowerCase();
                                    String headerValue = line.substring(separ + 1, line.length()).trim();
                                    responseHeaders.put(headerName, headerValue);
                                }
                            }
                        }
                        if (!responseHeaders.isEmpty()) {
                            DiscoveryResponse discoveryResponse = new DiscoveryResponse(senderAddress, bindAddress, responseHeaders);
                            List<Listener> listenersCopy = new ArrayList<Listener>();
                            synchronized (this) { listenersCopy.addAll(listeners); }
                            for (Listener listener : listenersCopy) {
                                try {
                                    listener.onDiscoveryResponse(discoveryResponse);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        if (br != null) { try { br.close(); } catch (Throwable e) { } }
                        if (isr != null) { try { isr.close(); } catch (Throwable e) { } }
                        if (bais != null) { try { bais.close(); } catch (Throwable e) { } }
                        br = null; isr = null; bais = null;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
    }
    
}
