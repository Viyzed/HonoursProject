package honoursprojectproofofconcept;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;

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
	
	private JLabel lblTitle;
	private JLabel lblIpv6;
	
	private JButton btnPortScan;
	private JButton btnPortConnect;
	private JList<Integer> lstIP;
	private JScrollPane sclIP;
	private DefaultListModel<Integer> lstIPM;
	private JButton btnClear;

	private JTextArea txtSpec;
	
	private JLabel lblSubnet;
	private JTextField txtSubnet;
	private JLabel lblIpEdit;
	private JTextField txtIpEdit;
	private JButton btnSave;

	private Worker worker = new Worker();
	
	public DeviceInfo(InetAddress ip) {
		super("Device Specification");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(600, 300);
		setResizable(false);
		setVisible(true);
		setLayout(new GridLayout(4,2));
		
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
		
		
		EventHandler handler = new EventHandler();
		MouseHandler mHandler = new MouseHandler();
		btnPortScan.addActionListener(handler);
		btnPortConnect.addActionListener(handler);
		btnClear.addActionListener(handler);

	}
	
	public String getIpv6Addresses(InetAddress[] addresses) {
	    for (InetAddress addr : addresses) {	    	
	        if (addr instanceof Inet6Address) {
	            return ((Inet6Address)addr).toString();
	        }
	    }
	    return "Device is not IPv6 capable.";
	}
	
	class Worker extends SwingWorker<Void, Void> {
		@Override
		protected Void doInBackground() throws Exception {
			for(int i = 9999; i < 10000; i++) {
					
					try {
						System.out.println("Open new Socket...");
						Socket SOCKET = new Socket(hostName, i);
						lstIPM.addElement(i);
						System.out.println("Port " + i + " is open on " + hostName);
						SOCKET.close();
					} catch(Exception e) {
						System.out.println("Port " + i + " is closed on " + hostName);
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
				worker.execute();
			} 
			if(event.getSource()==btnClear) {
				worker.cancel(true);
				btnPortScan.setEnabled(true);
				lstIPM.clear();
				txtSpec.setText("");
				worker = new Worker();
			}
			if(event.getSource()==btnPortConnect) {
				try {
					Socket SOCKET = new Socket(hostName, lstIP.getSelectedValue());
					NetworkInterface netInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(hostName)); 
					byte[] mac = netInterface.getHardwareAddress();
					StringBuilder sb = new StringBuilder();
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
					txtSubnet.setText((String.valueOf(netInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength())));
					SOCKET.close();
				} catch (UnknownHostException e) {
					txtSpec.setText("Port " + lstIP.getSelectedValue() + " cannot be opened on " + hostName);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
		
	
	
	private class MouseHandler extends MouseAdapter implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			
			
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
		
	}

}
