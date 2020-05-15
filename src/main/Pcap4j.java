package main;

import java.io.IOException;

import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PcapStat;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;

public class Pcap4j {

	public static void main(String[] args) throws PcapNativeException, NotOpenException {
		//Get the user to choose a network interface
		PcapNetworkInterface nif = getNetworkInterface();
		System.out.println("Choose your network interface: ");
		//If network interface is not chosen, exit
		if(nif == null) {
			System.out.println("No interface chosen.");
			System.exit(1);
		}
		
		//open the network interface and get a handle
		int snapshotLength = 65536; //in bytes
		int readTimeout = 50; //in milliseconds
		final PcapHandle handle;
		handle = nif.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);
		//set a filter to only listen for tcp packets on port 80 (HTTP)
		//String filter = "tcp port 80";
		//handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
	
		//create a dumper to dump output to a pcap file
		PcapDumper dumper = handle.dumpOpen("./src/lists/out.pcap");
		
		//create a listener that defines what to do with the received packets
		PacketListener listener = new PacketListener() {
			@Override
			public void gotPacket(Packet packet) {
				System.out.println(handle.getTimestamp());
				System.out.println(packet);
				
				//dump the packets to the output file
				try {
					dumper.dump(packet, handle.getTimestamp());
				} catch(NotOpenException e) {
					e.printStackTrace();
				}
			}
			
		};
		
		//tell the handle to loop using the listener
		try {
			int maxPackets = 50;
			handle.loop(maxPackets, listener);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//get stats about the handle
		PcapStat stats = handle.getStats();
		System.out.println("Packets received: " + stats.getNumPacketsReceived());
		System.out.println("Packets dropped: " + stats.getNumPacketsDropped());
		System.out.println("Packets dropped by interface: " + stats.getNumPacketsDroppedByIf());
		System.out.println("Packets captured: " + stats.getNumPacketsCaptured());
		
		
		//close the handler and dumper when finished
		dumper.close();
		handle.close();
		
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
