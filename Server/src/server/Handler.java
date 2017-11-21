import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.lang.Thread;
import protocols.Operations.OperationRequest;
import protocols.Operations.OperationResponse;
import protocols.Operations.Kind;
import protocols.Login.LoginResponse;
import protocols.Login.LoginRequest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.*;
import javax.crypto.Cipher;

public class Handler extends Thread implements Runnable {
	private Socket clientSocket;
  private PrivateKey serverPrivateKey;
  private PublicKey serverPublicKey;
  private PublicKey clientPublicKey;
	private InputStream inputStream;
	private OutputStream outputStream;

  private boolean ValidUser(String user, String password) {
      return (user == "lmao" && password == "lmao");
  }

  public void loadPublicKey(String key) throws GeneralSecurityException, IOException {
    byte[] data = Base64.getDecoder().decode((key.getBytes()));
    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
    KeyFactory fact = KeyFactory.getInstance("RSA/ECB/PKCS1Padding");
    this.clientPublicKey = fact.generatePublic(spec);
   }

  private byte[] encrypt(byte[] msg){
    Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    rsa.init(Cipher.ENCRYPT_MODE, this.publicKey);
    return rsa.doFinal(msg);
  }

  private byte[] decrypt(byte[] msg){
    Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    rsa.init(Cipher.DECRYPT_MODE, this.privateKey);
    return rsa.doFinal(msg);
  }

  private OperationResponse ProcessGetRequest(OperationRequest request) throws IOException {
    Path path = Paths.get(request.getPath());
    byte[] data = Files.readAllBytes(path);

    OperationResponse  response = OperationResponse.newBuilder()
                                    .setKind(Kind.GET)
                                    .setCode(OperationResponse.Status.OK)
                                    .setData(ByteString.of(data))
                                    .build();
    return response;
  }

  private OperationResponse ProcessPutRequest(OperationRequest request) throws IOException {
    Path file = Paths.get(request.getPath());
    byte[] data = request.getData();
    Files.write(file, data);

    return OperationResponse.newBuilder().setKind(Kind.PUT)
                            .setCode(OperationResponse.Status.OK).build();
  }

	// Constructor que tiene como parámetro una referencia al socket abierto en por otra clase
	public Handler(Socket clientSocket, PrivateKey privateKey, PublicKey publicKey) {
    this.serverPrivateKey = privateKey;
    this.serverPublicKey = publicKey;
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

      // Send Server public key
      // NOTE: Quizas cuando se recibe tb este en B64 si esto lo esta
      outputStream.write(this.serverPublicKey.getEncoded());

      /* Server state: Not Logged --> Must receive authentication */
      // Receive login message
      // TODO: Recibir bytes cifrados
      LoginRequest loginReq = LoginRequest.parseFrom(inputStream);  // NOTE: Deberia parsear desde los bytes descifrados
      LoginResponse loginRes;

      // If user is invalid, exit. Else load client public RSA key
      if (!ValidUser(loginReq.getUser(), loginReq.getPass())) {
        loginRes = LoginResponse.newBuilder().setCode(LoginResponse.Status.ERROR).build();
        loginRes.writeTo(outputStream);

        this.clientSocket.close();
        return;
      } else {
        loadPublicKey(loginReq.getPublicKey());
        loginRes = LoginResponse.newBuilder().setCode(LoginResponse.Status.OK).build();
      }

      /* Server state: Waiting Req --> Must receive petitions */
      while (true) {
        // TODO: Recibir bytes cifrados
        OperationRequest request = OperationRequest.parseFrom(inputStream);
        OperationResponse resp;

        switch(request.getKind()) {
          case Operations.Kind.GET:
            response = ProcessPutRequest(request);
            break;
          case Operations.Kind.GET:
            response = ProcessGetRequest(request);
            break;
          default:
            response = OperationResponse.newBuilder()
                                          .setKind(request.getKind())
                                          .setCode(OperationResponse.Status.ERROR)
                                          .build();

            break;
        }
      }

      // Crypt and send
      byte[] cryptData = encrypt(response.toByteArray());
      outputStream.write(cryptData);
		} catch (SocketException e) {
      System.out.println("[-] Connection lost with " + this.clientSocket.toString());
      this.clientSocket.close();
    } catch (Exception e) {
      System.out.println("[!] " + e.toString());
    }
	}
}
