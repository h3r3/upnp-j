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
import java.util.LinkedHashMap;
import java.util.Map;

import org.postronic.h3.upnp.impl.UPnPImplUtils;

public class Discovery {
    
    private static final int MAX_MESSAGE_SIZE = 1024 * 64;
    
    private enum Status {
        READY, RUNNING, CLOSING, CLOSED
    }
    
    private final InetSocketAddress bindInetSocketAddress;
    private final InetSocketAddress discoveryInetSocketAddress;
    private Status status = Status.READY;
    private UserAgent userAgent = UPnPImplUtils.DEFAULT_USER_AGENT;
    private DatagramSocket socketUDP;
    private SocketUDPReceiverRunnable socketUDPReceiverRunnable;
    private Thread socketUDPReceiverThread, timeoutThread;
    private int timeoutSeconds = 4;
    private Callback callback; 
    
    public interface Callback {
        void onDiscoveryResponse(Discovery discovery, DiscoveryResponse discoveryResponse);
        void onDiscoveryTerminated(Discovery discovery);
    }
    
    public Discovery(InetSocketAddress bindInetSocketAddress) {
        this.bindInetSocketAddress = bindInetSocketAddress;
        this.discoveryInetSocketAddress = UPnPImplUtils.getDiscoveryInetSocketAddress(bindInetSocketAddress.getAddress());
    }
    
    public InetSocketAddress getBindInetSocketAddress() {
        return bindInetSocketAddress;
    }
    
    public synchronized void start(UserAgent userAgent, final int timeoutSeconds, Callback discoveryCallback) throws IOException {
        if (!Status.READY.equals(status)) throw new RuntimeException("Cannot start Discovery while in " + status + " status");
        this.status = Status.RUNNING;
        try {
        this.callback = discoveryCallback;
        this.socketUDP = new DatagramSocket(bindInetSocketAddress);
        this.socketUDPReceiverRunnable = new SocketUDPReceiverRunnable();
        this.socketUDPReceiverThread = new Thread(socketUDPReceiverRunnable, "UPnP UDP receiver");
        this.socketUDPReceiverThread.start();
        timeoutThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(((long) timeoutSeconds) * 1000L);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    close();
                }
            }
        }, "upnp-j DiscoveryTimeoutThread");
        timeoutThread.setDaemon(false);
        sendSSDP();
        timeoutThread.start();
        } catch (IOException e) {
            close();
            throw e;
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }
    
    private void sendSSDP() throws IOException {
        ByteArrayOutputStream baos = null;
        try {
            String discoveryAddr = discoveryInetSocketAddress.getAddress() instanceof Inet6Address ? "[" + discoveryInetSocketAddress.getAddress().getHostAddress() + "]" : discoveryInetSocketAddress.getAddress().getHostAddress();  
            baos = new ByteArrayOutputStream(512);            
            baos.write("M-SEARCH * HTTP/1.1\r\n".getBytes());
            baos.write(("HOST: " + discoveryAddr + ":" + discoveryInetSocketAddress.getPort() + "\r\n").getBytes());
            baos.write("MAN: \"ssdp:discover\"\r\n".getBytes());
            baos.write(("MX: " + Math.max(1, timeoutSeconds - 1) + "\r\n").getBytes());
            baos.write("ST: upnp:rootdevice\r\n".getBytes());
            UserAgent userAgent = this.userAgent;
            if (userAgent != null) {
                baos.write(("USER-AGENT: " + userAgent.toString() + "\r\n").getBytes());
            }
            baos.write("\r\n".getBytes());
            baos.flush();
            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, discoveryInetSocketAddress);
            this.socketUDP.send(packet);
        } finally {
            if (baos != null) { try { baos.close(); } catch (Throwable e) { } }
        }
    }
    
    public synchronized void abort() {
        //FIXME Implement!
    }
    
    private synchronized void close() {
        status = Status.CLOSING;
        if (socketUDP != null) { 
            socketUDP.close();
        } else {
            status = Status.CLOSED;
        }
        try {
            if (socketUDPReceiverThread != null) socketUDPReceiverThread.join(1000);
        } catch (InterruptedException e) { }
    }
    
    private final class SocketUDPReceiverRunnable implements Runnable {
        
        @Override
        public void run() {
            try {
                byte[] buf = new byte[MAX_MESSAGE_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, MAX_MESSAGE_SIZE);
                for (;;) {
                    try {
                        socketUDP.receive(packet);
                    } catch (Throwable e) {
                        synchronized (Discovery.this) {
                            if (!Status.CLOSING.equals(status)) {
                                e.printStackTrace();                                
                            }
                            status = Status.CLOSED;
                        }
                        return;
                    }
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
                            DiscoveryResponse discoveryResponse = new DiscoveryResponse(senderAddress, bindInetSocketAddress, responseHeaders);
                            if (callback != null) {
                                try {
                                    callback.onDiscoveryResponse(Discovery.this, discoveryResponse);
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
            } finally {
                socketUDP.close();
                if (callback != null) {
                    try {
                        callback.onDiscoveryTerminated(Discovery.this);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
    }
    
}
