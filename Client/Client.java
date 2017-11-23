import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.net.Socket;
import java.net.UnknownHostException;
import java.lang.Runtime;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import java.util.Base64;
import javax.crypto.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;



public class Client {

	//Function for adjust dinamically the input buffer size:
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

	//Return a encrypted byte array by RSA algorithm:
	public static byte[] EncryptRSA(byte[] msg, PublicKey publicKey) throws Exception{
		Cipher rsa = Cipher.getInstance("RSA");
		rsa.init(Cipher.ENCRYPT_MODE, publicKey);
		return rsa.doFinal(msg);
	}

	//Return a encrypted byte array (Simetric Crypt):
	public static byte[] Encrypt(byte[] msg, Key key) throws Exception{
		Cipher aes = Cipher.getInstance("AES");
		aes.init(Cipher.ENCRYPT_MODE, key);
		return aes.doFinal(msg);
	}

	//Return a decrypted byte array (Simetric Crypt):
	public static byte[] Decrypt(byte[] msg, Key key) throws Exception{
		Cipher aes = Cipher.getInstance("AES");
		aes.init(Cipher.DECRYPT_MODE, key);
		return aes.doFinal(msg);
	}

	//Load a Public Key from Byte Array:
	public static PublicKey LoadPublicKey(byte[] key) throws GeneralSecurityException, IOException {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
	}


	public static void main(String[] args) {

		//Host name and port:
		String host = "";
		int port = 31416;

		//User and Password:
		String user = "";
		String psswd = "";

		try{
			System.out.println("Wellcome to SFTP-R");
			System.out.println("Version 1.0");
			System.out.println("****************************");
			System.out.println("Generating AES Key...");

			//Generate Symetric Key:
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			Key key = keyGenerator.generateKey();

			System.out.println("Done!");
			System.out.println("****************************");

			Scanner sc = new Scanner(System.in);
			System.out.print("Enter Host Name: " );
			host = sc.next();
			System.out.print("Enter User: ");
			user = sc.next();
			Console cnsl = System.console();
			char[] pwd = cnsl.readPassword("Enter Password: ");
			psswd = new String(pwd);

			System.out.println("Establishing connection...");

			//Create Socket:
			Socket socket = new Socket(host, port);
			//Get input/output Stream:
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();

			//Get Server Public Key
			byte[] serverPubKeyBytes = Client.ReceiveTillEnd(inputStream);
			PublicKey serverPubKey = Client.LoadPublicKey(serverPubKeyBytes);

			//Send Simetric Key encrypted by RSA:
			outputStream.write(Client.EncryptRSA(key.getEncoded(), serverPubKey));

			//LOG IN TO SERVER
			Login loginREQ = new Login();
			loginREQ.code = 1001;
			loginREQ.user = user;
			loginREQ.pass = psswd;

			byte[] loginRequest = Client.Encrypt(Login.Serialize(loginREQ), key);
			outputStream.write(loginRequest, 0, loginRequest.length);

			//Get Server Response:
			byte[] serverRES = Client.ReceiveTillEnd(inputStream);
			Login loginRES = Login.Deserialize(Client.Decrypt(serverRES, key));

			if(loginRES.code == 401){
				System.out.println("[**LOGIN ERROR**]: Invalid user or password");
				System.exit(1);
			}

			System.out.println("Done!");
			System.out.println("****************************");

			String command = "";

			//Print more info:
			System.out.println("SFTP-R> Type help for commands info, if something going wrong, you will see an error like this:");

			while(!command.equals("quit")){

				System.out.print("SFTP-R> ");
				command = sc.nextLine();

				if(!command.equals("quit")){
					String [] parts = command.split("\\s+");

					if(parts[0].equals("get") && parts.length == 3){
						//GET....
						Operation getREQ = new Operation();
						getREQ.code = 1002;
						getREQ.kind = Operation.Kind.Get;
						getREQ.path = parts[1];

						byte[] getRequest = Client.Encrypt(Operation.Serialize(getREQ), key);
						outputStream.write(getRequest, 0, getRequest.length);

						byte[] serverGetRES = Client.ReceiveTillEnd(inputStream);
						Operation getRES = Operation.Deserialize(Client.Decrypt(serverGetRES, key));

						if(getRES.code == 202){
							Path path = Paths.get(parts[2]);

							if(Files.exists(path))
								Files.delete(path);

							Path file = Files.createFile(path);

							byte[] data = getRES.data;
							Files.write(file, data);
							System.err.println("[**GET SUCCESS**]");
						}
						else
							System.err.println("[**GET ERROR**]");
					}
					else if(parts[0].equals("put") && parts.length == 3){
						//PUT..
						Path path = Paths.get(parts[1]);
						byte[] data = Files.readAllBytes(path);
						Operation putREQ = new Operation();
						putREQ.code = 1003;
						putREQ.data = data;
						putREQ.kind = Operation.Kind.Put;
						putREQ.path = parts[2];

						byte[] putRequest = Client.Encrypt(Operation.Serialize(putREQ), key);
						outputStream.write(putRequest, 0, putRequest.length);

						byte[] serverPutRES = Client.ReceiveTillEnd(inputStream);
						Operation putRES = Operation.Deserialize(Client.Decrypt(serverPutRES, key));

						if(putRES.code == 203)
							System.err.println("[**PUT SUCCESS**]");
						else
							System.err.println("[**PUT ERROR**]");

					}
					else if(parts[0].equals("help") && parts.length == 1){
						System.err.println("[GET]: get <remote file path> <local destination path>\n" +
                               "[PUT]: put <local file path> <remote destination path>\n" +
                               "[EXIT]: quit\n" +
                               "[!!!]: Remember use full paths!!!");
					}
					else
						System.err.println("[**ERROR**]: Unknown Command");

				}
			}

			//Close Elements:
			sc.close();
			socket.close();

		} catch (Exception e) {
            System.out.println(e.toString());
        }
	}
}
