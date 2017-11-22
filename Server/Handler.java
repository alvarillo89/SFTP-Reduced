import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.lang.Thread;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.*;
import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;


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

  private byte[] ReceiveTillEnd(InputStream input) {
    final int bufferSize = 1024;
    ByteArrayOutputStream dynamicBuffer = new ByteArrayOutputStream();
    byte[] buffer = new byte[bufferSize];
    int read = 0;

    while ((read = input.read(buffer)) >= bufferSize) {
      dynamicBuffer.write(buffer, 0, read);
    }

    return dynamicBuffer.toByteArray();
  }

  public void loadPublicKey(String key) throws GeneralSecurityException, IOException {
    byte[] data = Base64.getDecoder().decode((key.getBytes()));
    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
    KeyFactory fact = KeyFactory.getInstance("RSA/ECB/PKCS1Padding");
    this.clientPublicKey = fact.generatePublic(spec);
   }

  private byte[] Encrypt(byte[] msg){
    Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    rsa.init(Cipher.ENCRYPT_MODE, this.clientPublicKey);
    return rsa.doFinal(msg);
  }

  private byte[] Decrypt(byte[] msg){
    Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    rsa.init(Cipher.DECRYPT_MODE, this.serverPrivateKey);
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
    byte[] dataReceive;
    byte[] dataSend;

		try {
			inputStream = clientSocket.getInputStream();
			outputStream = clientSocket.getOutputStream();

      // Send Server public key
      // NOTE: Quizas cuando se recibe tb este en B64 si esto lo esta
      outputStream.write(this.serverPublicKey.getEncoded());

      /* Server state: Not Logged --> Must receive authentication */
      // Receive login message
      data = ReceiveTillEnd(inputStream);
      data = Decrypt(data);
      LoginRequest loginReq = LoginRequest.parseFrom(data);  // NOTE: Deberia parsear desde los bytes descifrados
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

        // Encrypt and send
        dataSend = Encrypt(loginRes.toByteArray());
        outputStream.write(dataSend);
      }

      /* Server state: Waiting Req --> Must receive petitions */
      while (true) {
        // TODO: Recibir bytes cifrados
        data = DecryptReceiveTillEnd(inputStream);
        data = Decrypt(data);
        OperationRequest request = OperationRequest.parseFrom(data);
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

        // Crypt and send
        dataSend = Encrypt(response.toByteArray());
        outputStream.write(dataSend);
      }

		} catch (SocketException e) {
      System.out.println("[-] Connection lost with " + this.clientSocket.toString());
      this.clientSocket.close();
    } catch (Exception e) {
      System.out.println("[!] " + e.toString());
    }
	}
}
