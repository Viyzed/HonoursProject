package honoursproject;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.*;

public class DeviceInfo extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private boolean iot;
	
	private InetAddress ip;
	private String hostName;
	private InetAddress[] knownIps;
	private Socket SOCKET;
	
	private static HttpURLConnection connection;
	
	private JLabel lblTitle;
	private JLabel lblIpv6;
	
	private JButton btnPortScan;
	private JButton btnPortConnect;
	private JList<Integer> lstIP;
	private JScrollPane sclIP;
	private DefaultListModel<Integer> lstIPM;
	private JButton btnClear;
	private ArrayList<Integer> ports;

	private JTextArea txtSpec;
	
	private JLabel lblSubnet;
	private JTextField txtSubnet;
	private JLabel lblIpEdit;
	private JTextField txtIpEdit;
	private JButton btnSave;

	private Worker portWorker = new Worker();
	
	public DeviceInfo(InetAddress ip) {
		super("Device Specification");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(600, 300);
		setResizable(false);
		setVisible(true);
		setLayout(new GridLayout(4,2));
		
		ports = constructPortsArray();
		
		this.ip = ip;
		hostName = this.ip.getHostName();
		try {
			this.knownIps = InetAddress.getAllByName(hostName);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		lblTitle = new JLabel(hostName);
		lblTitle.setFont(new Font("Sans", Font.BOLD, 15));
		add(lblTitle);
		
		Panel upperPanel = new Panel(new GridLayout());
		lblIpv6 = new JLabel(getIpv6Addresses(knownIps));
		upperPanel.add(lblIpv6);
		add(upperPanel);
		
		Panel lowerPanel = new Panel();
		Panel leftLowerPanel = new Panel(new GridLayout(2,2));
		Panel rightLowerPanel = new Panel(new GridLayout());
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.X_AXIS));
		
		btnPortScan = new JButton("Scan Ports");
		btnPortConnect = new JButton("Connect");
		lstIPM = new DefaultListModel<Integer>();
		lstIP = new JList<Integer>(lstIPM);
		sclIP = new JScrollPane(lstIP);
		sclIP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sclIP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		lstIP.setBounds(0, 0, leftLowerPanel.getWidth(), this.getHeight());
		lstIP.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		lstIP.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		lstIP.setVisibleRowCount(-1);
		btnClear = new JButton("Clear");
		leftLowerPanel.add(btnPortScan);
		leftLowerPanel.add(btnPortConnect);
		leftLowerPanel.add(sclIP);
		leftLowerPanel.add(btnClear);
		
		txtSpec = new JTextArea();
		txtSpec.setSize(this.getWidth()/2, this.getHeight()/2);
		txtSpec.setEditable(false);
		rightLowerPanel.add(txtSpec);
		
		lowerPanel.add(leftLowerPanel);
		lowerPanel.add(rightLowerPanel);
		
		add(lowerPanel);
		
		Panel bottomPanel = new Panel(new BorderLayout());
		Panel topBottomPanel = new Panel(new FlowLayout());
		Panel bottomBottomPanel = new Panel(new FlowLayout());
		lblSubnet = new JLabel("IPv6 Subnet: ");
		txtSubnet = new JTextField("\t\t\t\t");
		lblIpEdit = new JLabel("New IPv6 Address: ");
		txtIpEdit = new JTextField("\t\t\t\t");
		txtSubnet.setEditable(false);
		btnSave = new JButton("Save");
		topBottomPanel.add(lblSubnet);
		topBottomPanel.add(txtSubnet);
		bottomBottomPanel.add(lblIpEdit);
		bottomBottomPanel.add(txtIpEdit);
		bottomBottomPanel.add(btnSave);
		bottomPanel.add(topBottomPanel, BorderLayout.PAGE_START);
		bottomPanel.add(bottomBottomPanel, BorderLayout.PAGE_END);
		add(bottomPanel);
		
		this.getDeviceSpecs();
		
		EventHandler handler = new EventHandler();
		btnPortScan.addActionListener(handler);
		btnPortConnect.addActionListener(handler);
		btnClear.addActionListener(handler);

	}
	
	public ArrayList<Integer> constructPortsArray() {
		ArrayList<Integer> array = new ArrayList<Integer>();
		Scanner pScan = null;
		try {
	    	pScan = new Scanner(new File("C:\\Users\\Andrew\\ports.csv"));
		} catch (FileNotFoundException e) {
			try {
				pScan = new Scanner(new File("/Users/Andrew/GitHub/HonoursProject/bin/lists/ports.csv"));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		} 
	    pScan.useDelimiter(",");
	    while(pScan.hasNext()) {
	    	array.add(pScan.nextInt());
	    }
	    return array;
	}
	
	public String getIpv6Addresses(InetAddress[] addresses) {
	    for (InetAddress addr : addresses) {	    	
	        if (addr instanceof Inet6Address) {
	            return ((Inet6Address)addr).toString();
	        }
	    }
	    return "Device is not IPv6 capable.";
	}
	
	private void getDeviceSpecs() {
		String mac = null;
		URL url = null;
		String cmd = null;
		
		if(System.getProperties().getProperty("os.name").startsWith("Windows")) {
			cmd = "arp -a ";
		} else {
			cmd = "arp ";
		}
		
		try {
			Process process = Runtime.getRuntime().exec(cmd + ip.getHostAddress());
		    process.waitFor();
		    BufferedReader macReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		    BufferedReader responseReader;
		    StringBuffer responseContent = new StringBuffer();
		    String line;
		    
		    while(macReader.ready()) {
		    	String addr = macReader.readLine();
		    	if(addr.startsWith("  " + ip.getHostAddress())) {
		    		mac = (addr.substring(addr.indexOf('-')-2, addr.indexOf('-')+ 15)).toUpperCase();
		    	} else if(addr.startsWith("? (" + ip.getHostAddress())) {
		    		mac = (addr.substring(addr.indexOf(':')-2, addr.indexOf(':')+15)).toUpperCase();
		    	}
		    }
		    
		    macReader.close();
		    
		    try {
				url = new URL("https://api.macvendors.com/" + mac);
				
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);
				
				int responseCode = connection.getResponseCode();
				
				if(responseCode > 299) {
					responseReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
					while((line = responseReader.readLine()) != null) {
						responseContent.append(line);
					}
					responseReader.close();
				} else {
					responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					while((line = responseReader.readLine()) != null) {
						responseContent.append(line);
					}
				}
					responseReader.close();
					
				txtSpec.append("Vendor: " + responseContent.toString() + "\n");
				txtSpec.append("MAC Address: " + mac + "\n");
				
					
				
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			} finally {
				connection.disconnect();
			}
		   
			
			//mac = netInterface.getHardwareAddress();
			/*StringBuilder sb = new StringBuilder();
	        for (int x = 0; x < mac.length; x++) {
	            sb.append(String.format("%02X%s", mac[x], (x < mac.length - 1) ? "-" : ""));
	        }
	        Enumeration<InetAddress> inetAddresses = netInterface.getInetAddresses();
	        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
	            if(inetAddress instanceof Inet6Address) {
	            	txtIpEdit.setText(String.valueOf(inetAddress));
	            }
	        }
			txtSpec.setText("MAC Address: " + sb.toString());
			txtSpec.append("\nSystem OS: " + System.getProperty("os.name"));
			System.getProperties().list(System.out);
			if(System.getProperty("os.name").startsWith("Windows")) {
				iot = false;
				txtSpec.append("\nHW: Desktop PC " + (System.getProperty("sun.cpu.isalist").equals("amd64") ? "(x64)" : "(x86)"));
			}
			if(iot) {
				txtSpec.append("\nIOT Device: True");
			} else {
				txtSpec.append("\nIOT Device: False");
			}
			txtSubnet.setText((String.valueOf(netInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength())));*/
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	class Worker extends SwingWorker<Void, Void> {
		@Override
		protected Void doInBackground() throws Exception {
		    
			for(int port : ports) {
					
					try {
						System.out.println("Open new Socket...");
						SOCKET = new Socket(hostName, port);
						lstIPM.addElement(port);
						System.out.println("Port " + port + " is open on " + hostName);
						SOCKET.close();
					} catch(Exception e) {
						System.out.println("Port " + port + " is closed on " + hostName);
						continue;
					}
			}
			
			return null;
		}
		
	}
	
	
	private class EventHandler implements ActionListener {
		
		public void actionPerformed(ActionEvent event) {
			if(event.getSource()==btnPortScan) {
				btnPortScan.setEnabled(false);
				btnClear.setText("Stop");
				portWorker.execute();
			} 
			if(event.getSource()==btnClear) {
				if(btnClear.getText().equals("Stop")) {
					btnClear.setText("Clear");
					portWorker.cancel(true);
					btnPortScan.setEnabled(true);
					portWorker = new Worker();
				} else {
					lstIPM.clear();
					portWorker.cancel(true);
					portWorker = new Worker();
				}
			}
			if(event.getSource()==btnPortConnect) {
				try {
					Socket SOCKET = new Socket(hostName, lstIP.getSelectedValue());
					txtSpec.append(SOCKET.toString());
					SOCKET.close();
				} catch (UnknownHostException e) {
					txtSpec.setText("Port " + lstIP.getSelectedValue() + " cannot be opened on " + hostName);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
		

}
