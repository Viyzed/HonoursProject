package server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class HttpServer implements Runnable {
	
	static final File API_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	
	static final int PORT = 8080;
	
	static final boolean verbose = true;
	
	private Socket connect;
	
	public HttpServer(Socket c) {
		connect = c;
	}
	
	public void run() {
		try {
			@SuppressWarnings("resource")
			ServerSocket sSocket = new ServerSocket(PORT);
			System.out.println("Server running and listening on port " + PORT);
			
			while(true) {
				HttpServer newServer = new HttpServer(sSocket.accept());
				
				if(verbose) {
					System.out.println("Connection opened.");
				}
				
				Thread thread = new Thread(newServer);
				thread.start();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//manage client connection
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			String input = in.readLine();
			StringTokenizer parse = new StringTokenizer(input);
			//get the HTTP method of the client
			String method = parse.nextToken().toUpperCase();
			fileRequested = parse.nextToken().toLowerCase();
			
			if(!method.equals("GET") && !method.equals("HEAD")) {
				
				if(verbose)
					System.out.println("501: Method not implemented:" + method);
				
				File file = new File(API_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int)file.length();
				String contentMimeType = "text/html";
				byte[] fileData = readFileData(file, fileLength);
				
				out.println("HTTP/1.1 501 Not implemented");
				out.println("Server: Java Http Server from SSaurel : 1.0");
				out.println("Date: " + new Date());
				out.println("Content type: " + contentMimeType);
				out.println("Content length: " + fileLength);
				out.println();
				out.flush();
				
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				
				return;
			}
			
			else {
				
			}
		
		} catch (IOException e) {
			System.out.println("Error getting input stream.");
			e.printStackTrace();
		}
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if(fileIn != null) {
				fileIn.close();
			}
		}
		
		return fileData;
	}


}
