package org.postronic.h3.upnp.impl;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.postronic.h3.upnp.UPnPDiscoveryData;
import org.postronic.h3.upnp.UPnPUserAgent;

public class UPnPDiscoveryImpl implements Future<List<UPnPDiscoveryData>> {
    
    private static final int MAX_MESSAGE_SIZE = 1024 * 64;
    
    private enum Status {
        READY, RUNNING, CLOSING, CLOSED
    }
    
    private final InetSocketAddress bindInetSocketAddress;
    private final InetSocketAddress discoveryInetSocketAddress;
    private Status status = Status.READY;
    private UPnPUserAgent userAgent = UPnPImplUtils.DEFAULT_USER_AGENT;
    private DatagramSocket socketUDP;
    private SocketUDPReceiverRunnable socketUDPReceiverRunnable;
    private Thread socketUDPReceiverThread, timeoutThread;
    private int timeoutSeconds = 4;
    private Callback callback; 
    
    public interface Callback {
        void onDiscoveryResponse(UPnPDiscoveryData discoveryResponse);
    }
    
    public UPnPDiscoveryImpl(InetSocketAddress bindInetSocketAddress) {
        this.bindInetSocketAddress = bindInetSocketAddress;
        this.discoveryInetSocketAddress = UPnPImplUtils.getDiscoveryInetSocketAddress(bindInetSocketAddress.getAddress());
    }
    
    public synchronized void start(UPnPUserAgent userAgent, final int timeoutSeconds, Callback discoveryCallback) throws IOException {
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
            UPnPUserAgent userAgent = this.userAgent;
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
    
    private synchronized void close() {
        switch (status) {
        case READY:
            status = Status.CLOSED;
            break;
        case RUNNING:
            status = Status.CLOSING;
            if (socketUDP != null) { 
                socketUDP.close();
            } else {
                status = Status.CLOSED;
            }
            try {
                if (socketUDPReceiverThread != null) socketUDPReceiverThread.join(1000);
            } catch (InterruptedException e) { }
            break;
        case CLOSING:
            try {
                if (socketUDPReceiverThread != null) socketUDPReceiverThread.join(1000);
            } catch (InterruptedException e) { }
            break;
        case CLOSED:
            break; 
        }
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
                        synchronized (UPnPDiscoveryImpl.this) {
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
                            UPnPDiscoveryData discoveryResponse = new UPnPDiscoveryData(senderAddress, bindInetSocketAddress, responseHeaders);
                            if (callback != null) {
                                try {
                                    callback.onDiscoveryResponse(discoveryResponse);
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
            }
        }
        
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDone() {
        synchronized (this) {
            return Status.CLOSED.equals(status);
        }
    }

    @Override
    public List<UPnPDiscoveryData> get() throws InterruptedException, ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UPnPDiscoveryData> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
