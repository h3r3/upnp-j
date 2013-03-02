package org.postronic.h3.upnp.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;


public class UPnPListen {
    
    private static final int MAX_MESSAGE_SIZE = 1024 * 64;

    private MulticastSocket multicastSocket;
    private MulticastSocketReceiverRunnable multicastSocketReceiverRunnable;
    private Thread multicastSocketReceiverThread;
    
    public void start() throws IOException {
        InetSocketAddress discoveryInetSocketAddress = UPnPImplUtils.getDiscoveryInetSocketAddress(null);
        this.multicastSocket = new MulticastSocket(discoveryInetSocketAddress.getPort());
        this.multicastSocket.joinGroup(discoveryInetSocketAddress.getAddress());
        this.multicastSocketReceiverRunnable = new MulticastSocketReceiverRunnable();
        this.multicastSocketReceiverThread = new Thread(multicastSocketReceiverRunnable, "UPnP Multicast receiver");
        this.multicastSocketReceiverThread.start();
    }
    
    private final class MulticastSocketReceiverRunnable implements Runnable {
        
        @Override
        public void run() {
            try {
                byte[] buf = new byte[MAX_MESSAGE_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, MAX_MESSAGE_SIZE);
                for (;;) {
                    multicastSocket.receive(packet);
                    SocketAddress senderAddress = packet.getSocketAddress();
                    byte[] data = packet.getData();
                    String res = new String(data).replaceAll("\\f", "");
                    System.out.println("-- MULTICAST from " + senderAddress);
                    System.out.println(res);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
    }

}
