package client;

import main.Main;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.*;

/**
 * Main Client GUI and network scanner
 */
public class DeviceScanner extends JFrame {

	private static final long serialVersionUID = 1L;
	
	//Swing components
	private JLabel lblTitle;
	private JLabel lblScan;
	private JButton btnScan;
	private JLabel lblReset;
	private JButton btnReset;
	private JList<String> lstIP;
	private JScrollPane sclIP;
	private JLabel lblStatus;
	private DefaultListModel<String> lstIPM;
	
	//IP addresses and SwingUtil thread worker for ip scanning
	byte[] ip;
	ArrayList<JLabel> foundIp;
	Worker scanWorker = new Worker();
	
	public DeviceScanner() {
		super("IPv6 Device Finder");
		//System.setProperty("java.net.preferIPv6Stack" , "true");
		this.ip = null;
		try {
    		ip = InetAddress.getLocalHost().getAddress();
		} catch (Exception e) {
    		return;    
		}
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(300, 450);
		setResizable(false);
		setVisible(true);
		setLayout(new GridLayout(4,2));
		
		Panel upperPanel = new Panel(new GridLayout(3,3));
		lblTitle = new JLabel("Scan for IPv6 Devices");
		lblTitle.setFont(new Font("Sans", Font.BOLD, 20));
		add(lblTitle);
		
		lblScan = new JLabel("Scan: ");
		btnScan = new JButton("Scan");
		btnScan.setToolTipText("Click to scan on your network for IPv6 devices.");
		lblReset = new JLabel("Stop/Reset:");
		btnReset = new JButton("Reset");
		upperPanel.add(lblScan);
		upperPanel.add(btnScan);
		upperPanel.add(lblReset);
		upperPanel.add(btnReset);
		
		add(upperPanel);
		
		Panel lowerPanel = new Panel(new GridLayout(1,5));
		
		foundIp = new ArrayList<JLabel>();
		
		lstIPM = new DefaultListModel<String>();
		lstIP = new JList<String>(lstIPM);
		//lstIP.setEditable(false);
		lstIP.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		lstIP.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		lstIP.setVisibleRowCount(-1);
		lstIP.setCellRenderer(new RedCellRenderer());

		
		sclIP = new JScrollPane(lstIP);
		sclIP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sclIP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		lowerPanel.add(sclIP);
		
		add(lowerPanel);
		
		lblStatus = new JLabel("Ready to scan.", JLabel.CENTER);
		add(lblStatus);
	
		//Event handlers for button presses and mouse actions
		EventHandler handler = new EventHandler();
		MouseHandler mHandler = new MouseHandler();
		btnScan.addActionListener(handler);
		btnReset.addActionListener(handler);
		lstIP.addMouseListener(mHandler);

	}
	
	//SwingUtil thread worker thread
	class Worker extends SwingWorker<Void, Void> {
			@Override
			protected Void doInBackground() throws Exception {
				//System.out.println("Worker Start");
				
				for(int i=1;i<256;i++) {
	        		final int j = i;  
	        		if(btnReset.getText().equals("Stop")) {
		        			//add IP address and IPv6 address to JList if IP is found
	                		try {
	                    		ip[3] = (byte)j;
	                    		InetAddress address = InetAddress.getByAddress(ip);
	                    		String output = address.toString().substring(1);
	                    		if (address.isReachable(500)) {
	                    			InetAddress[] addresses = InetAddress.getAllByName(address.getHostName());
	                        		System.out.println(output + " is on the network");
	                        		foundIp.add(new JLabel(output + ">" + address.getHostName()));
	                        		String element = null;
	                        		for(InetAddress ipv6 : addresses) {
	                        			if(ipv6 instanceof Inet6Address) {
	                        				element = (output + ">" + address.getHostName() + " ");
	                        			} else {
	                        				element = (output + ">" + address.getHostName());
	                        			}
	                        		}
	                        		lstIPM.addElement(element);
	                        		
	                    		} else {
	                        		System.out.println("Not Reachable: "+output);
	                    		}
	                		} catch (Exception e) {
	                    		e.printStackTrace();
	                		}
		           			 	
		    				  
        				} else break;
	    			}
				
				return null;
			}
			
	}
	
	//highlight the device in red if IPv6 address is found
	private static class RedCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent( JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
            Component c = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
            if(value.toString().endsWith(" ")) {
            	 c.setBackground( Color.RED );
            }
            
            return c;
        }
    }
	
	//Handlers for Swing buttons
	private class EventHandler implements ActionListener {
		
		public void actionPerformed(ActionEvent event) {
			if(event.getSource()==btnScan) {
				btnScan.setEnabled(false);
				btnReset.setText("Stop");
				lblStatus.setText("Scanning...");
				scanWorker.execute();
			}
			
			else if(event.getSource()==btnReset) {
				if(btnReset.getText().equals("Stop")) {
					btnScan.setEnabled(true);
					btnReset.setText("Reset");
					lblStatus.setText("Ready to scan.");
					scanWorker.cancel(true);
					//System.out.println(foundIp);
					
				} else {
					lstIPM.clear();
					scanWorker = new Worker();
				}
			}	
		}
		
	}
	
	//Handler for mouse activity
	private class MouseHandler extends MouseAdapter implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
            int index = lstIP.getSelectedIndex();
            System.out.println("Index Selected: " + index);
            String s = (String) lstIP.getSelectedValue();
            System.out.println("Value Selected: " + s.toString());
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					String[] parts = lstIP.getSelectedValue().split(">");
					InetAddress singleIp = null;	
					try {
						singleIp = InetAddress.getByName(parts[0]);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					Main.ipv4DeskAddr = singleIp;
					new DeviceInfo(singleIp);
				}
			}); 
			
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
		
	}

}
