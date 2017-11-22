import java.io.*;

public class Login implements Serializable {
  private static final long serialVersionUID = 7662746382818912479L;
  public int code;
  public String user;
  public String pass;
  public String pubKey;


  public static byte[] Serialize(Object obj){
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutput out = new ObjectOutputStream(bos)) {
      out.writeObject(obj);
      return bos.toByteArray();
    } catch (Exception e) {
      System.out.println(e.toString());
      return null;
    }
  }

  public static Login Deserialize(byte[] bytes) {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInput in = new ObjectInputStream(bis)) {
      Login out = Login.class.cast(in.readObject());
      return out;
    } catch (Exception e) {
      System.out.println(e.toString());
      return null;
    }
  }
}
