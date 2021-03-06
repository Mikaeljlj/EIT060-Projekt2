package server;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.security.KeyStore;


import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.security.cert.X509Certificate;


import staff.User;

public class Server implements Runnable {

	
	
	
	
	
	private ServerSocket serverSocket = null;
	private static int numConnectedClients = 0;
	private Hospital hospital;
	
	public Server(ServerSocket ss) throws IOException {
		serverSocket = ss;
		newListener();
		hospital=new Hospital();

	}

	public void run() {
		try {
			SSLSocket socket = (SSLSocket) serverSocket.accept();
			newListener();
			SSLSession session = socket.getSession();
			X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];

			numConnectedClients++;

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			User user = hospital.getUser(cert);


			String clientMsg = "";
			String handleClientInput =null;
			input: while ((clientMsg = in.readLine()) != "" && clientMsg != null) {
				if (clientMsg != null && clientMsg.equals("quit")) {
					break input;
				}
				handleClientInput = hospital.handleClientInput(clientMsg, user);
				out.println(handleClientInput);
				out.println("ENDOFMSG".toCharArray());
				
				if((handleClientInput).equals("Write information")){
					String additionalClientMsg = in.readLine();
					String additionalHandleClientInput = hospital.handleClientInput("41 "+clientMsg+" "+additionalClientMsg, user);
					out.println(additionalHandleClientInput);
					out.println("ENDOFMSG".toCharArray());
				}
				
			}

			close(socket, out, in);
		} catch (Exception e) {
			System.out.println("Client died: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}



	private void close(SSLSocket socket, PrintWriter out, BufferedReader in) throws IOException {
		hospital.save();
		in.close();
		out.close();
		socket.close();
		numConnectedClients--;
		System.out.println("client disconnected");
		System.out.println(numConnectedClients + " concurrent connection(s)\n");
	}

	private void newListener() {
		(new Thread(this)).start();
	} // calls run()

	

	public static void main(String args[]) {
		System.out.println("\nServer Started\n");
		int port = -1;
		if (args.length >= 1) {
			port = Integer.parseInt(args[0]);
		}
		String type = "TLS";
		try {
			ServerSocketFactory ssf = getServerSocketFactory(type);
			ServerSocket ss = ssf.createServerSocket(port);
			((SSLServerSocket) ss).setNeedClientAuth(true); // enables client
															// authentication
			new Server(ss);
		} catch (IOException e) {
			System.out.println("Unable to start Server: " + e.getMessage());
			e.printStackTrace();
		}
	}



	private static ServerSocketFactory getServerSocketFactory(String type) {
		if (type.equals("TLS")) {
			SSLServerSocketFactory ssf = null;
			try { // set up key manager to perform server authentication
				SSLContext ctx = SSLContext.getInstance("TLS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				char[] password = "password".toCharArray();

				ks.load(new FileInputStream("Certs/ServerStore/serverkeystore"), password); // keystore
				// password
				// (storepass)
				ts.load(new FileInputStream("Certs/ServerStore/servertruststore"), password); // truststore
				// password
				// (storepass)
				kmf.init(ks, password); // certificate password (keypass)
				tmf.init(ts); // possible to use keystore as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				ssf = ctx.getServerSocketFactory();
				return ssf;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return ServerSocketFactory.getDefault();
		}
		return null;
	}
}
