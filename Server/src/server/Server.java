import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.lang.Thread;

public class ServerFTP {

	public static void main(String[] args) {
		ServerSocket serverSocket;
		Socket socketServicio;
		Hebrita hebrita;

		// Puerto de escucha PI
		final int port = 31416;

    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    PublicKey publicKey = keyPair.getPublic();
    PrivateKey privateKey = keyPair.getPrivate();

		try {
			serverSocket = new ServerSocket(port);

			while (true) {
				clientSocket = serverSocket.accept();

				Handler handler = new Handler(clientSocket, privateKey, publicKey);
				handler.start();
			}

		} catch (IOException e) {
			System.err.println("Error al escuchar en el puerto "+port);
		}
	}
}
