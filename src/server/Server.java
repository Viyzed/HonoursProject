package server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;

public class Server extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	static final int PORT = 8080;
	private ServerSocket sSocket;
	private Socket cSocket;
		
	private InetAddress dInetaddress;
	private int dPort;
	
	//Pcap4j fields
	private InetAddress iAddress;
	private PcapNetworkInterface pcNif;
	private PcapHandle handle;
	private Packet packet;
	
	
	//Swing component fields
	private JLabel lblTitle;
	private JLabel lblStatus;
	private JTextArea txtLog;
	private JButton btnPwr;
		
	private Worker listenWorker = new Worker();
	
	public Server() {
		super("Server Demo");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 400);
		setResizable(false);
		setVisible(true);
		
		//Set up Pcap4j network interface to listen for packets
		try {
			iAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		try {
			pcNif = Pcaps.getDevByAddress(iAddress);
		} catch (PcapNativeException e) {
			e.printStackTrace();
		}
		
		
		Panel upperPanel = new Panel(new FlowLayout());
		lblTitle = new JLabel("IPv4toIPv6");
		lblStatus = new JLabel("Offline");
		upperPanel.add(lblTitle);
		upperPanel.add(lblStatus);
		add(upperPanel, BorderLayout.NORTH);
		
		Panel middlePanel = new Panel();
		middlePanel.setPreferredSize(new Dimension(super.getWidth()-20,300));
		middlePanel.setMinimumSize(new Dimension(super.getWidth()-20,300));
		txtLog = new JTextArea();
		middlePanel.add(txtLog);
		txtLog.setPreferredSize(middlePanel.getMinimumSize());
		txtLog.setMinimumSize(middlePanel.getMinimumSize());
		txtLog.setBorder(BorderFactory.createLineBorder(Color.black));
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
			
			dInetaddress = InetAddress.getByName(sInetaddress);
			dPort = Integer.parseInt(sPort);
			
			Socket tmpSock = new Socket(dInetaddress, dPort);
			txtLog.append(tmpSock.toString().substring(0, 57)+"\n");
			txtLog.append(tmpSock.toString().substring(57, tmpSock.toString().length())+"\n\n");
			
			try {
				handle = pcNif.openLive(65536, PromiscuousMode.PROMISCUOUS, 10);
			} catch (PcapNativeException e) {
				e.printStackTrace();
			}
			
			try {
				packet = handle.getNextPacketEx();
			} catch (EOFException e) {
				e.printStackTrace();
			} catch (PcapNativeException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			} catch (NotOpenException e) {
				e.printStackTrace();
			}
			
			handle.close();
			
			IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
			Inet4Address srcAddr = ipV4Packet.getHeader().getSrcAddr();
			System.out.println(srcAddr);

			
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
