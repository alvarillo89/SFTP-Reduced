import java.io.*;

public class Operation implements Serializable{
  private static final long serialVersionUID = 7462746372818764178L;
  public int code;
  public Kind kind;
  public String path;
  public byte[] data;

  public static enum Kind {
    Put, Get;
  }

  public Operation() {
  }

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

  public static Operation Deserialize(byte[] bytes) {
      try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = new ObjectInputStream(bis)) {
        Operation out = Operation.class.cast(in.readObject());
        return out;
      } catch (Exception e) {
        System.out.println(e.toString());
        return null;
      }
  }
}
