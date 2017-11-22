import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.lang.Runtime;
import java.security.*;
import java.util.Scanner;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


public class Client {

	private static byte[] ReceiveTillEnd(InputStream input) throws IOException {
		final int bufferSize = 1024;
		ByteArrayOutputStream dynamicBuffer = new ByteArrayOutputStream();
		byte[] buffer = new byte[bufferSize];
		int read = 0;
	
		while ((read = input.read(buffer)) >= bufferSize) {
		  dynamicBuffer.write(buffer, 0, read);
		}
	
		return dynamicBuffer.toByteArray();
	}

	//Return a encrypted byte array:
	public static byte[] Encrypt(byte[] msg, PublicKey publicKey) throws Exception{
		Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		rsa.init(Cipher.ENCRYPT_MODE, publicKey);
		return rsa.doFinal(msg);
	}

	//Return a decrypted byte array:
	public static byte[] Decrypt(byte[] msg, PrivateKey privateKey) throws Exception{
		Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		rsa.init(Cipher.DECRYPT_MODE, privateKey);
		return rsa.doFinal(msg);
	}

	//Load a Public Key from Byte Array:
	public static PublicKey LoadPublicKey(byte[] key) throws GeneralSecurityException, IOException {
		X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
		KeyFactory fact = KeyFactory.getInstance("RSA/ECB/PKCS1Padding");
		return fact.generatePublic(spec);
	}

	//Convert PublicKey to String:
	public static String ToString(PublicKey key) throws GeneralSecurityException {
		byte[] keyBytes = key.getEncoded();
		return Base64.getEncoder().encodeToString(keyBytes);
	}


	public static void main(String[] args) {
		
		//Host name and port:
		String host = "localhost";
		int port = 31416;

		//User and Password (for simplificate):
		String user = "user";
		String psswd = "user";
	
		try{
			System.out.println("Wellcome to SFTP-R");
			System.out.println("Version 1.0");
			System.out.println("****************************");
			System.out.println("Generating RSA Keys...");

			//Generate Keys:
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();

			System.out.println("Done!");
			System.out.println("****************************");
			System.out.println("Establishing connection...");

			//Create Socket:
			Socket socket = new Socket(host, port);
			//Get input/output Stream:
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();

			//GET SERVER PUBKEY
			byte[] serverPubKeyBytes = Client.ReceiveTillEnd(inputStream);
			PublicKey serverPubKey = Client.LoadPublicKey(serverPubKeyBytes);

			//LOG IN TO SERVER
			Login loginREQ = new Login();
			loginREQ.code = 1001;
			loginREQ.login = user;
			loginREQ.pass = psswd;
			loginREQ.pubKey = Client.ToString(publicKey);

			byte[] loginRequest = Client.Encrypt(Login.Serialize(loginREQ), serverPubKey);
			outputStream.write(loginRequest, 0, loginRequest.length);

			//Get Server Response:
			byte[] serverRES = Client.ReceiveTillEnd(inputStream);
			Login loginRES = Login.Deserialize(Client.Decrypt(serverRES, privateKey));

			if(loginRES.code == 401){
				System.out.println("[**LOGIN ERROR**]: Invalid user or password");
				System.exit(1);
			}

			System.out.println("Done!");
			System.out.println("****************************");
			
			String command = "";
			Scanner sc = new Scanner(System.in);

			while(!command.equals("quit")){
				
				System.out.print("SFTP-R> ");
				command = sc.nextLine();

				if(!command.equals("quit")){
					String [] parts = command.trim().split("\\s+");
					
					if(parts[0].equals("get") && parts.length == 3){
						//GET....
						Operation getREQ = new Operation();
						getREQ.code = 1002;
						getREQ.kind = Operation.Kind.Get;
						getREQ.path = parts[1];

						byte[] getRequest = Client.Encrypt(Operation.Serialize(getREQ), serverPubKey);
						outputStream.write(getRequest, 0, getRequest.length);

						byte[] serverGetRES = Client.ReceiveTillEnd(inputStream);
						Operation getRES = Operation.Deserialize(Client.Decrypt(serverGetRES, privateKey));

						if(getRES.code == 202){
							Path file = Paths.get(parts[2]);
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
						putREQ.path = parts[2];

						byte[] putRequest = Client.Encrypt(Operation.Serialize(putREQ), serverPubKey);
						outputStream.write(putRequest, 0, putRequest.length);

						byte[] serverPutRES = Client.ReceiveTillEnd(inputStream);
						Operation putRES = Operation.Deserialize(Client.Decrypt(serverPutRES, privateKey));

						if(putRES.code == 203)
							System.err.println("[**PUT SUCCESS**]");
						else
							System.err.println("[**PUT ERROR**]");

					}
					else{
						System.err.println("[**ERROR**]: Unknown Command");
					}
				
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
