import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.lang.Thread;
import java.security.*;
import javax.crypto.*;

public class Server {

	public static void main(String[] args) {
    UsersDB database = new UsersDB();
		ServerSocket serverSocket;
		Socket clientSocket;

		// Puerto de escucha PI
		final int port = 31416;

		try {
      System.out.print("[*] Generating RSA Keys...");
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey privateKey = keyPair.getPrivate();
      System.out.println("\tOK");

			serverSocket = new ServerSocket(port);

			while (true) {
        System.out.println("[*] Listening for new Connections...");
				clientSocket = serverSocket.accept();

				Handler handler = new Handler(database, clientSocket, privateKey, publicKey);
				handler.start();
			}

		} catch (IOException e) {
			System.err.println("[!] Error binding to port " + port);
		} catch (NoSuchAlgorithmException noAlg) {
      System.err.println("[!] Cannot generate RSA Keys");
    }
	}
}
