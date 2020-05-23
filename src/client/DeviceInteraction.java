package client;

import java.awt.GridLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.*;

/**
 * Device Interaction
 * Send packet data to the Server
 */
public class DeviceInteraction extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	//fields for device specs and Server socket
	private Socket servSock;
	private String inetaddress;
	private String mac;
	private int portNo;
	
	private JLabel lblComm;
	
	//invisible GUI (previously visible)
	public DeviceInteraction(String inetaddress, int portNo, String mac)  {
		setTitle("Device Interaction");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(600, 300);
		setResizable(false);
		setVisible(false);
		setLayout(new GridLayout(4,2));
		
		lblComm = new JLabel();
		this.portNo = portNo;
		this.inetaddress = inetaddress;
		this.mac = mac;
		try {
			this.servSock = new Socket("localhost", 5000);
			lblComm.setText("Connection with Server open on port: 5000");
		} catch (IOException e) {
			lblComm.setText("Connection with Server refused.");
			e.printStackTrace();
		}
		
		add(lblComm);
		
		try {
			serverComm();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	//open Socket connection with the Server
	private void serverComm() throws IOException {
		PrintWriter pr = new PrintWriter(servSock.getOutputStream());
		pr.println("Connect to " + inetaddress.toString() + " on port " + portNo);
		pr.println(inetaddress);
		pr.println(portNo);
		pr.println(mac);
		pr.flush();
	}

}
