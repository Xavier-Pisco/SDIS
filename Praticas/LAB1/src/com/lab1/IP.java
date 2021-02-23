package com.lab1;

public class IP {
    private static int IP1, IP2, IP3, IP4;
    public IP(String IP) {
        this.setIP(IP);
    }
    public static void setIP(String IP) {
        String[] IPs = IP.split("\\.");
        IP1 = Integer.parseInt(IPs[0]);
        IP2 = Integer.parseInt(IPs[1]);
        IP3 = Integer.parseInt(IPs[2]);
        IP4 = Integer.parseInt(IPs[3]);
    }
    public static String string() {
        return String.valueOf(IP1) + "." + String.valueOf(IP2) + "." + String.valueOf(IP3) + "." + String.valueOf(IP4);
    }
}