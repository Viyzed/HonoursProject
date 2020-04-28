package client;

import java.awt.GridLayout;
import java.io.IOException;
import java.net.Socket;

import javax.swing.*;

public class DeviceInteraction extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private Socket SOCKET;
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
			this.SOCKET = new Socket(this.inetaddress, this.portNo);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		lblSocket = new JLabel("Socket: " + SOCKET.toString());
		add(lblSocket);
		
		try {
			SOCKET.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
