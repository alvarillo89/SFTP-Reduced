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

		try {
			// Abrimos el socket en modo pasivo, escuchando el en puerto indicado por "port"
			serverSocket = new ServerSocket(port);

			do {

				// Aceptamos una nueva conexión con accept()
				socketServicio = serverSocket.accept();

				// Creamos un objeto de la clase ProcesadorYodafy, pasándole como
				// argumento el nuevo socket, para que realice el procesamiento
				// Este esquema permite que se puedan usar hebras más fácilmente.
				ProcesadorYodafy procesador = new ProcesadorYodafy(socketServicio);
				hebrita = new Hebrita(procesador);
				hebrita.start();

			} while (true);

		} catch (IOException e) {
			System.err.println("Error al escuchar en el puerto "+port);
		}

	}
