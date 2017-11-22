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

  public void loadPublicKey(String key) throws GeneralSecurityException, IOException {
    byte[] data = Base64.getDecoder().decode((key.getBytes()));
    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
    KeyFactory fact = KeyFactory.getInstance("RSA");
    this.clientPublicKey = fact.generatePublic(spec);
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
    Path path = Paths.get(request.path);
    byte[] data = Files.readAllBytes(path);

    Operation response = new Operation();
    response.code = 200;
    response.kind = request.kind;
    response.path = request.path;
    response.data = data;

    return response;
  }

  private Operation ProcessPutRequest(Operation request) throws IOException {
    Path file = Paths.get(request.path);
    byte[] data = request.data;
    Files.write(file, data);

    Operation response = new Operation();
    response.code = 200;
    response.kind = request.kind;
    response.path = request.path;
    response.data = new byte[0];

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
    // NOTE: Quizas cuando se recibe tb este en B64 si esto lo esta
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
    if (usersDatabase.ValidCredentials(loginReq.user, loginReq.pass) || loginReq == null) {
      loginRes.code = 400;

      dataSend = Encrypt(Login.Serialize(loginRes));
      outputStream.write(dataSend);
      this.clientSocket.close();
      return;
    } else {
      loadPublicKey(loginReq.pubKey); // Load client public key
      loginRes.code = 200;

      dataSend = Encrypt(Login.Serialize(loginRes));
      outputStream.write(dataSend);
    }

    /* Server state: Waiting Req --> Must receive petitions */
    while (true) {
      dataReceive = Decrypt(ReceiveTillEnd(inputStream));
      Operation request = Operation.Deserialize(dataReceive);
      Operation response = new Operation();

      // If object couldnt be deserialiced, throw out this packet
      if (request == null) continue;

      switch(request.kind) {
        case Put:
          response = ProcessPutRequest(request);
          break;
        case Get:
          response = ProcessGetRequest(request);
          break;
        default:
          response.kind = request.kind;
          response.code = 400;
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
