import java.io.*;

public class Operation implements Serializable{
  public int code;
  public Kind kind;
  public String path;
  public byte[] data;

  public static enum Kind {
    put, get;
  }

  public Operation() {
  }

  public static byte[] Serizalice(Object obj){
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutput out = new ObjectOutputStream(bos)) {
      out.writeObject(obj);
      return bos.toByteArray();
    } catch (Exception e) {
      System.out.println(e.toString());
      return null;
    }
  }

  public static Operation Deserialice(byte[] bytes) {
      try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = new ObjectInputStream(bis)) {
        Operation out = Operation.class.cast(in.readObject());
        return out;
      } catch (Exception e) {
        System.out.println(e.toString());
        return null;
      }
  }

  public static void main(String[] args) {
    Operation operation = new Operation();
    operation.kind = Kind.get;
    operation.code = 200;
    operation.path = "/home/salva/archivo.txt";
    operation.data = "Helloooo".getBytes();

    try{
      byte[] serializado = Operation.Serizalice(operation);
      Object msg2 = Operation.Deserialice(serializado);
      Operation msg = Operation.class.cast(msg2);

      System.out.print(msg.code + "\n" + msg.kind + "\n" +  msg.path + "\n" + msg.data + "\n");
    } catch(Exception e){
    }
  }

}
