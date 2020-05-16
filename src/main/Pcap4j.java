package main;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapHandle.TimestampPrecision;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4DestinationUnreachablePacket;
import org.pcap4j.packet.IcmpV4CommonPacket.Builder;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IcmpV4ParameterProblemPacket;
import org.pcap4j.packet.IcmpV4TimeExceededPacket;
import org.pcap4j.packet.IcmpV6CommonPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Code;
import org.pcap4j.packet.namednumber.IcmpV4Type;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.packet.namednumber.TcpPort;
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
		final PcapHandle handle4;
		final PcapHandle handle6;
		handle = nif.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);

		//set a filter to only listen for tcp packets on port 80 (HTTP)
		//String filter = "icmp port 445";
		//handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
	
		//create a dumper to dump output to a pcap file
		//PcapDumper dumper = handle.dumpOpen("./src/lists/out.pcap");
		
		handle4 = Pcaps.openOffline("./src/lists/icmpv4.pcap");
		handle6 = Pcaps.openOffline("./src/lists/icmpv6.pcap");
		
		//create a listener that defines what to do with the received packets
		PacketListener listener = new PacketListener() {
			@Override
			public void gotPacket(Packet packet) {
				System.out.println(handle.getTimestamp());
				System.out.println(packet);
				
				//dump the packets to the output file
		//		try {
		//			dumper.dump(packet, handle.getTimestamp());
		//		} catch(NotOpenException e) {
		//			e.printStackTrace();
		//		}
			}
			
		};
		
		//Packet p = handle.getNextPacket();
		//Packet.Builder b = p.getBuilder();
		//b.get(IpV6Packet.Builder.class);
		Packet icmpv4 = handle4.getNextPacket();
		Packet icmpv6 = handle6.getNextPacket();
		Packet.Builder icmpv6b = icmpv4.getBuilder();
		Packet.Builder icmpv6pb = icmpv6b.getPayloadBuilder();
		Packet newP = icmpv6b.build();
		Packet newPP = icmpv6pb.build();
		byte[] newPPd = newPP.getRawData();
		byte[] newPPdc = Arrays.copyOfRange(newPPd, 48, 80);
		System.out.println(newP.toString() + "\n\n");
		System.out.println(newPP.toString() + "\n\n");
		System.out.println(new String(newPPd, Charset.forName("UTF-8")));
		System.out.println(new String(newPPdc, Charset.forName("UTF-8")));
		
		
	    Packet test = icmpv6.getPayload().getPayload().getPayload().getPayload();
	    byte[] testRaw = test.getRawData();
	    System.out.println("TEST\n"+test.toString());
	    System.out.println(new String(testRaw, Charset.forName("UTF-8")));
	    
	    
	   
	    final TcpPacket.Builder tcp = new TcpPacket.Builder();
	    tcp
	    	.ack(false)
	    	.padding(testRaw)
	    	.paddingAtBuild(true)
	    	.srcPort(new TcpPort((short) 5000, "port"))
	    	.dstPort(new TcpPort((short) 5000, "port"));
	    
	    
	    IcmpV4Type type = IcmpV4Type.ECHO;
	    IcmpV4Code code = IcmpV4Code.NO_CODE;
	    
		final Packet.Builder icmpV4eb = new IcmpV4EchoPacket.Builder();
		icmpV4eb.payloadBuilder(tcp);
		
		final IcmpV4CommonPacket.Builder icmpV4b = new IcmpV4CommonPacket.Builder();
		icmpV4b
			.type(type)
			.code(code)
			.payloadBuilder(icmpV4eb)
			.correctChecksumAtBuild(true);
		final IpV4Packet.Builder ipv4b = new IpV4Packet.Builder();
	    ipv4b
	        .version(IpVersion.IPV4)
	        .tos(IpV4Rfc791Tos.newInstance((byte) 0))
	        .identification((short) 100)
	        .ttl((byte) 100)
	        .protocol(IpNumber.ICMPV4)
	        .payloadBuilder(icmpV4b)
	        .correctChecksumAtBuild(true)
	        .correctLengthAtBuild(true);
	    final EthernetPacket.Builder eb = new EthernetPacket.Builder();
	    eb.type(EtherType.IPV4).payloadBuilder(ipv4b).paddingAtBuild(true);
	    
	    
	    
	    ipv4b.dstAddr(icmpv4.get(IpV4Packet.class).getHeader().getDstAddr());
        ipv4b.srcAddr(icmpv4.get(IpV4Packet.class).getHeader().getSrcAddr());
        eb.srcAddr(icmpv4.get(EthernetPacket.class).getHeader().getSrcAddr());
        eb.dstAddr(icmpv4.get(EthernetPacket.class).getHeader().getDstAddr());
        
        Packet packet = eb.build();
        
        try {
        	handle.sendPacket(packet);
        } catch (PcapNativeException e) {
	        e.printStackTrace();
        } catch (NotOpenException e) {
	        e.printStackTrace();
        }
	    
		
		//IcmpV4CommonPacket p = null;
		//try {
		//	p = IcmpV4CommonPacket.newPacket(newPPdc, 0, 32);
		//} catch (IllegalRawDataException e) {
		//	e.printStackTrace();
		//}
		//
		//Packet.Builder pb = p.getBuilder();
		//System.out.println(p.toString());
		
		//try {
		//	dumper.dump(newP, handle.getTimestamp());
		//} catch(NotOpenException e) {
		//	e.printStackTrace();
		//}
		
		//tell the handle to loop using the listener
		//try {
		//	int maxPackets = 50;
		//	handle.loop(maxPackets, listener);
		//} catch (InterruptedException e) {
		//	e.printStackTrace();
		//}
		
		//get stats about the handle
		//PcapStat stats = handle.getStats();
		//System.out.println("Packets received: " + stats.getNumPacketsReceived());
		//System.out.println("Packets dropped: " + stats.getNumPacketsDropped());
		//System.out.println("Packets dropped by interface: " + stats.getNumPacketsDroppedByIf());
		//System.out.println("Packets captured: " + stats.getNumPacketsCaptured());
		
		
		//close the handler and dumper when finished
		//dumper.close();
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
