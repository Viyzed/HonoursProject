package main;

import java.io.IOException;
import java.net.InetAddress;

import javax.swing.SwingUtilities;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.util.NifSelector;

public class Main {
	
	public static PcapNetworkInterface netDevice = null;
	public static  InetAddress ipv4DeskAddr = null;

	public static void main(String[] args) {
		//Get the user to choose a network interface
		System.out.println("Choose your network interface: ");
		netDevice = getNetworkInterface();
		//If network interface is not chosen, exit
		if(netDevice == null) {
			System.out.println("No interface chosen.");
			System.exit(1);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new client.DeviceScanner();
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new server.Server();
			}
		});
	}
	
	static PcapNetworkInterface getNetworkInterface() {
		PcapNetworkInterface nif = null;
		try {
			nif = new NifSelector().selectNetworkInterface();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return nif;
	}

}
