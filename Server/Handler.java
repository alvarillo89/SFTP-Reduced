import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.Socket;
import java.util.Random;
import java.lang.Thread;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.*;
import java.util.Base64;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.SocketException;

public class Handler extends Thread implements Runnable {
  private Socket clientSocket;
  private PrivateKey serverPrivateKey;
  private PublicKey serverPublicKey;
  private PublicKey clientPublicKey;
  private SecretKey symmetricKey;
  private InputStream inputStream;
  private OutputStream outputStream;
  private UsersDB usersDatabase;

  private static byte[] ReceiveTillEnd(InputStream input) throws IOException {
		final int bufferSize = 1024;
		ByteArrayOutputStream dynamicBuffer = new ByteArrayOutputStream();
		byte[] buffer = new byte[bufferSize];
    boolean keepReading = true;
		int read;


		while (keepReading) {
      read = input.read(buffer);
		  dynamicBuffer.write(buffer, 0, read);

      if (read < bufferSize) keepReading = false;
		}

		return dynamicBuffer.toByteArray();
	}

   private byte[] DecryptAssymetric(byte[] msg) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                             InvalidKeyException, IllegalBlockSizeException,
                                             BadPaddingException {
    Cipher rsa = Cipher.getInstance("RSA");
    rsa.init(Cipher.DECRYPT_MODE, this.serverPrivateKey);
    return rsa.doFinal(msg);
   }

  private byte[] Encrypt(byte[] msg) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                            InvalidKeyException, IllegalBlockSizeException,
                                            BadPaddingException {
    Cipher aesCipher = Cipher.getInstance("AES");
    aesCipher.init(Cipher.ENCRYPT_MODE, this.symmetricKey);
    return aesCipher.doFinal(msg);

  }

  private byte[] Decrypt(byte[] msg) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                            InvalidKeyException, IllegalBlockSizeException,
                                            BadPaddingException {
    Cipher aesCipher = Cipher.getInstance("AES");
    aesCipher.init(Cipher.DECRYPT_MODE, this.symmetricKey);
    return aesCipher.doFinal(msg);
  }

  private Operation ProcessGetRequest(Operation request) throws IOException {
    Operation response = new Operation();
    Path path = Paths.get(request.path);

    if (!Files.exists(path)) {
      response.code = 402;
      response.kind = request.kind;
      response.path = request.path;
    } else {
      byte[] data = Files.readAllBytes(path);

      response.code = 202;
      response.kind = request.kind;
      response.path = request.path;
      response.data = data;
    }

    return response;
  }

  private Operation ProcessPutRequest(Operation request) throws IOException {
    Operation response = new Operation();
    Path path = Paths.get(request.path);
    byte[] data = request.data;

    try {
      if (Files.exists(path)) Files.delete(path);

      Path file = Files.createFile(path);
      Files.write(file, data);

      response.code = 203;
      response.kind = request.kind;
      response.path = request.path;
    } catch (java.nio.file.AccessDeniedException e) {
      response.code = 403;
      response.kind = request.kind;
      response.path = request.path;
    }

    return response;
  }

  // Constructor que tiene como parámetro una referencia al socket abierto en por otra clase
  public Handler(UsersDB database, Socket clientSocket, PrivateKey privateKey, PublicKey publicKey) {
    this.serverPrivateKey = privateKey;
    this.serverPublicKey = publicKey;
    this.clientSocket = clientSocket;
    this.usersDatabase = database;
  }

	// Aquí es donde se realiza el procesamiento realmente:
public void run() {
  String datos;
  byte[] dataReceive;
  byte[] dataSend;

  try {
    inputStream = clientSocket.getInputStream();
    outputStream = clientSocket.getOutputStream();

    // Send Server public key and receive simetric key
    outputStream.write(this.serverPublicKey.getEncoded());
    dataReceive = DecryptAssymetric(ReceiveTillEnd(inputStream));
    this.symmetricKey = new SecretKeySpec(dataReceive , 0, dataReceive.length, "AES");

    /* Server state: Not Logged --> Must receive authentication */
    // Receive login message
    dataReceive = ReceiveTillEnd(inputStream);
    dataReceive = Decrypt(dataReceive);
    Login loginReq = Login.Deserialize(dataReceive);
    Login loginRes = new Login();


    // If user is invalid, exit. Else load client public RSA key
    if (loginReq == null || !usersDatabase.ValidCredentials(loginReq.user, loginReq.pass)) {
      loginRes.code = 401;

      dataSend = Encrypt(Login.Serialize(loginRes));
      outputStream.write(dataSend);
      this.clientSocket.close();
      return;
    } else {
      loginRes.code = 201;

      dataSend = Encrypt(Login.Serialize(loginRes));
      outputStream.write(dataSend);
    }

    /* Server state: Waiting Req --> Must receive petitions */
    while (true) {
      dataReceive = Decrypt(ReceiveTillEnd(inputStream));
      Operation request = Operation.Deserialize(dataReceive);
      Operation response = new Operation();

      // If object couldnt be deserialiced, or it doesnt say
      // which kind of operation is, throw this packet
      if (request == null || request.kind == null) continue;

      switch(request.kind) {
        case Put:
          response = ProcessPutRequest(request);
          break;
        case Get:
          response = ProcessGetRequest(request);
          break;
        default:
          response.kind = request.kind;
          response.code = 404;  // Unexpected error
          break;
      }

      // Crypt and send
      dataSend = Encrypt(Operation.Serialize(response));
      outputStream.write(dataSend);
    }

    } catch (SocketException e) {
      System.err.println("[-] Connection lost with " + this.clientSocket.toString());

      try {
        this.clientSocket.close();
      } catch (IOException ioe) {
        System.err.println("[!] " + ioe.toString());
      }
    } catch (Exception e) {
      System.err.println("[!] " + e.toString());
    }
  }
}
