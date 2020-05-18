package server;

import main.Main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;

import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Code;
import org.pcap4j.packet.namednumber.IcmpV4Type;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;

public class Server extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	static final int PORT = 5000;
	private ServerSocket sSocket;
	private Socket cSocket;
		
	private InetAddress dInetaddress;
	private int dPort;
	private String sInetaddress;
	
	//Pcap4j fields
	private PcapHandle handle;
	private Packet packet;
	
	
	//Swing component fields
	private JLabel lblTitle;
	private JLabel lblStatus;
	private JTextArea txtLog;
	private JScrollPane txtScl;
	private JButton btnPwr;
		
	private Worker listenWorker = new Worker();
	
	public Server() {
		super("Server");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 400);
		setResizable(false);
		setVisible(true);
		
		Panel upperPanel = new Panel(new FlowLayout());
		lblTitle = new JLabel("IPv4toIPv6");
		lblStatus = new JLabel("Offline");
		upperPanel.add(lblTitle);
		upperPanel.add(lblStatus);
		add(upperPanel, BorderLayout.NORTH);
		
		Panel middlePanel = new Panel();
		middlePanel.setPreferredSize(new Dimension(super.getWidth()-20,300));
		middlePanel.setMinimumSize(new Dimension(super.getWidth()-20,300));
		txtLog = new JTextArea(17, 40);
		txtLog.setEditable(false);
		txtLog.setBorder(BorderFactory.createLineBorder(Color.black));
		txtScl = new JScrollPane(txtLog);
		txtScl.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		txtScl.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(txtScl);
		add(middlePanel, BorderLayout.CENTER);
		
		Panel lowerPanel = new Panel(new FlowLayout());
		btnPwr = new JButton("Start");
		lowerPanel.add(btnPwr);
		add(lowerPanel, BorderLayout.SOUTH);
		
		EventHandler handler = new EventHandler();
		btnPwr.addActionListener(handler);
		

	}
	
	class Worker extends SwingWorker<Void, Void> {
		@Override
		protected Void doInBackground() throws Exception {
			sSocket = new ServerSocket(PORT);
			txtLog.append("Server running and listening on port " + PORT + "\n\n");
			cSocket = sSocket.accept();
			System.out.println("Client connected");
			
			InputStreamReader in = new InputStreamReader(cSocket.getInputStream());
			BufferedReader br = new BufferedReader(in);
			
			String comm = br.readLine();
			txtLog.append(comm+"\n\n");
			String sInetaddress = br.readLine();
			String sPort = br.readLine();
			String macAddr = br.readLine();
			
			dInetaddress = InetAddress.getByName(sInetaddress);
			dPort = Integer.parseInt(sPort);
			sInetaddress = dInetaddress.getHostAddress();
			
			//open the network interface and get a handle
			int snapshotLength = 65536; //in bytes
			int readTimeout = 50; //in milliseconds
			final PcapHandle handle;
			handle = Main.netDevice.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);
			//set a filter to only listen for tcp packets on port 80 (HTTP)
			String filter = "(icmp6 and ether dst " + macAddr + ")";
			handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
			Runtime.getRuntime().exec("ping -6 " + sInetaddress);
			
			//create a listener that defines what to do with the received packets
			PacketListener listener = new PacketListener() {
				@Override
				public void gotPacket(Packet packet) {
					
					
					//deconstruct ICMPv6 packet
					txtLog.append(handle.getTimestamp().toString() + "\n\n");
					txtLog.append(packet.toString() + "\n\n");
					
					Packet transPkg = packet.getPayload();
					txtLog.append(transPkg.toString() + "\n\n");
					
					transPkg = transPkg.getPayload();
					txtLog.append(transPkg.toString() + "\n\n");
					
					transPkg = transPkg.getPayload();
					txtLog.append(transPkg.toString() + "\n\n");
					
					transPkg = transPkg.getPayload();
					txtLog.append(transPkg.toString() + "\n\n");
					
					//get payload data
					byte[] payload = transPkg.getRawData();
					
					//build ICMPv4 packet
					final Packet.Header payloadHeader = new Packet.Header() {
						private static final long serialVersionUID = 1L;
						@Override
						public int length() {
							return 32;
						}
						@Override
						public byte[] getRawData() {
							return payload;
						}
					};
					
					TcpPacket payloadPacket = null;
					try {
						payloadPacket = TcpPacket.newPacket(payloadHeader.getRawData(), 0, payloadHeader.length());
					} catch (IllegalRawDataException e1) {
						e1.printStackTrace();
					}
					Packet.Builder payloadBuilder = payloadPacket.getBuilder();
					
					txtLog.append("Payload Header: \n" + payloadBuilder.build().toString() + "\n\n");
					try {
						txtLog.append("TCP Data: \n" + new String(payloadBuilder.build().getRawData(), "UTF-8") + "\n\n");
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
				    
				    IcmpV4Type type = IcmpV4Type.ECHO;
				    IcmpV4Code code = IcmpV4Code.NO_CODE;
					final Packet.Builder icmpV4eb = new IcmpV4EchoPacket.Builder();
					//icmpV4eb
					//	.payloadBuilder(payloadBuilder);
					txtLog.append("ICMPv4 Echo Packet: \n" + icmpV4eb.build().toString() + "\n\n");
					
					final IcmpV4CommonPacket.Builder icmpV4b = new IcmpV4CommonPacket.Builder();
					icmpV4b
						.type(type)
						.code(code)
						.payloadBuilder(icmpV4eb)
						.correctChecksumAtBuild(true);
					txtLog.append("ICMPv4 Packet: \n" + icmpV4b.build().toString() + "\n\n");
					
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
				    
				    
				    ipv4b.dstAddr((Inet4Address) Main.ipv4DeskAddr);
			        try {
						ipv4b.srcAddr((Inet4Address) Inet4Address.getLocalHost());
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
			        txtLog.append("IPv4 Packet: " + ipv4b.build().toString() + "\n\n");
			        
			        eb.srcAddr(packet.get(EthernetPacket.class).getHeader().getSrcAddr());
			        eb.dstAddr(packet.get(EthernetPacket.class).getHeader().getDstAddr());
			        txtLog.append("Ethernet Packet: " + eb.build().toString() + "\n\n");
			        
			        Packet IcmpV4Pack = eb.build();
			        
			        txtLog.append("IPv6 Packet translated to IPv4.\n\n");
					
			        try {
			        	handle.sendPacket(IcmpV4Pack);
			        	txtLog.append("IPv4 Packet sent.");
			        } catch (PcapNativeException e) {
				        e.printStackTrace();
			        } catch (NotOpenException e) {
				        e.printStackTrace();
			        }
				}
			};
			
			//tell the handle to loop using the listener
			try {
				int maxPackets = 1;
				handle.loop(maxPackets, listener);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//get stats about the handle
			PcapStat stats = handle.getStats();
			System.out.println("Packets received: " + stats.getNumPacketsReceived() + "\n");
			System.out.println("Packets dropped: " + stats.getNumPacketsDropped() + "\n");
			System.out.println("Packets dropped by interface: " + stats.getNumPacketsDroppedByIf() + "\n");
			System.out.println("Packets captured: " + stats.getNumPacketsCaptured() + "\n");
			
			//close the handler and dumper when finished
			handle.close();
			
			return null;
		}
		
	} 
	
	private class EventHandler implements ActionListener {
		
		public void actionPerformed(ActionEvent event) {
			if(event.getSource()==btnPwr && btnPwr.getText().equals("Start")) {
				btnPwr.setText("Stop");
				lblStatus.setText("Online");
				listenWorker.execute();
				
			} else {
				btnPwr.setText("Start");
				lblStatus.setText("Offline");
				txtLog.setText("");
				try {
					sSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				listenWorker.cancel(true);
				listenWorker = new Worker();
				
			}
			
		}	
		
	}

}
