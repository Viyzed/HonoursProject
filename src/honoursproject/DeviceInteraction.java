package honoursproject;

import java.awt.GridLayout;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.*;

public class DeviceInteraction extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private Socket socket;
	private String inetaddress;
	private int portNo;
	
	private JLabel lblSocket;
	
	public DeviceInteraction(String inetaddress, int portNo) {
		setTitle("Device Interaction");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(600, 300);
		setResizable(false);
		setVisible(true);
		setLayout(new GridLayout(4,2));
				
		this.portNo = portNo;
		this.inetaddress = inetaddress;
		try {
			this.socket = new Socket(inetaddress, portNo);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		lblSocket = new JLabel("Socket: " + socket.toString());
		add(lblSocket);
		
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
