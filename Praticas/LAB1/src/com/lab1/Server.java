package com.lab1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.*;

public class Server {
    private static Map<String, IP> DNSTable = new HashMap<>();
    public static void main(String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[0]);
        DatagramSocket socket = new DatagramSocket(portNumber);
        byte[] receiveBuffer = null;
        DatagramPacket receivePacket = null;
        byte[] sendBuffer = null;
        DatagramPacket sendPacket = null;
        while(true) {
            receiveBuffer = new byte[256];
            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Client: " + received);
            String response = dns_entries(received.split(" "));
            InetAddress receiveAddress = receivePacket.getAddress();
            int receivePort = receivePacket.getPort();
            sendBuffer = response.getBytes(StandardCharsets.UTF_8);
            sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, receiveAddress, receivePort);
            socket.send(sendPacket);
            System.out.println("Server: " + response);
        }
    }
    public static String dns_entries(String[] args){
        if (args[0].equals("register")) {
            IP ip = DNSTable.putIfAbsent(args[1], new IP(args[2]));
            if (ip != null) {
                return "EXISTS";
            } else {
                System.out.println(DNSTable.size());
                return String.valueOf(DNSTable.size());
            }
        } else if (args[0].equals("lookup")) {
            try {
                IP ip = DNSTable.get(args[1]);
                if (ip == null) {
                    return "NOT_FOUND";
                }
                return ip.string();
            } catch(Exception e){
                return "NOT_FOUND";
            }
        } else {
            return "ERROR";
        }
    }
}
