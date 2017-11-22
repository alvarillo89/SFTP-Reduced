import java.util.Map;
import java.util.HashMap;

public class UsersDB {
  private Map<String, String> creedentials;

  public UsersDB() {
    this.creedentials = new HashMap<String, String>();
    this.creedentials.put("Salva", "1234");
    this.creedentials.put("Alvaro", "1234");
  }

  public boolean ValidCredentials(String user, String password) {
    return (this.creedentials.get(user) == password);
  }
}
