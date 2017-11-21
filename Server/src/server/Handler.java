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
import protocols.Operations.Kind;
import protocols.Login.LoginRequest;
import protocols.Login.LoginResponse;
import protocols.Login.LoginRequest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class Handler extends Thread implements Runnable {
	private Socket clientSocket;
	private InputStream inputStream;
	private OutputStream outputStream;

  private boolean ValidUser(String user, String password) {
      if (user == "lmao" && password == "lmao") return true;
  }

  private OperationResponse ProcessGetRequest(OperationRequest request) {
    Path path = Paths.get("path/to/file");
    byte[] data = Files.readAllBytes(path);

    OperationResponse  response = OperationResponse.newBuilder()
                                    .setKind(Kind.GET)
                                    .setData(ByteString.of(data))
                                    .setCode(OperationResponse.Status.OK)
                                    .build();

    return response;
  }

  private OperationResponse ProcessPutRequest(OperationRequest request) {
    Path file = Paths.get("the-file-name");
    byte[] data = request.getData();
    Files.write(file, data);
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
        OperationRequest request = OperationRequest.parseFrom(inputStream);

        switch(request.getKind()) {
          case Kind.PUT:
            ProcessPutRequest(request);
            break;
          case Kind.GET:
            ProcessGetRequest(request);
            break;
        }
      }
		} catch (IOException e) {
			System.err.println(e.toString());
		}

	}
}
