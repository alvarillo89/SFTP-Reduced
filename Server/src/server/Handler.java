import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.lang.Thread;
import protocols.Operations.OperationRequest;
import protocols.Operations.OperationResponse;
import protocols.Login.LoginRequest;
import protocols.Login.LoginResponse;


public class Handler extends Thread implements Runnable {
	private Socket clientSocket;
	private InputStream inputStream;
	private OutputStream outputStream;

  private boolean ValidUser(String user, String password) {
      if (user == "lmao" && password == "lmao") return true;
  }

	// Constructor que tiene como parámetro una referencia al socket abierto en por otra clase
	public Handler(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}


	// Aquí es donde se realiza el procesamiento realmente:
	public void run() {
		PrintWriter outPrinter;
		BufferedReader inReader;
		String datos;

		try {
			inputStream = clientSocket.getInputStream();
			outputStream = clientSocket.getOutputStream();

			// outPrinter = new PrintWriter(outputStream, true);
			// inReader = new BufferedReader(new InputStreamReader(inputStream));

      // TODO(Salva): Meter Logica para mandar mi clave publica
      //              Abajo los parse from habria que hacerlos en "texto plano"
      //              asi que habiar que pasarle a una funcion que lo haga

      /* Server state: Not Logged --> Must receive authentication */
      // Receive login message
      LoginRequest loginReq = LoginRequest.parseFrom(inputStream);
      LoginResponse loginRes;

      // If user is invalid, exit
      if (!ValidUser(loginReq.getUser(), loginReq.getPass())) {
        loginRes = LoginResponse.newBuilder().setCode(LoginResponse.Status.ERROR).build();
        loginRes.writeTo(outputStream);
        this.clientSocket.close();
        return;
      } else {
        loginRes = LoginResponse.newBuilder().setCode(LoginResponse.Status.OK).build();
        loginRes.writeTo(outputStream);
      }

      /* Server state: Waiting Req --> Must receive petitions */
      while (true) {
        OperationRequest request = OperationRequest.
        


      }



			outPrinter.println(respuesta);



		} catch (IOException e) {
			System.err.println("Error al obtener los flujso de entrada/salida.");
		}

	}
}
