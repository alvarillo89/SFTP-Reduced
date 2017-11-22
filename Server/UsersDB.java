import java.util.Map;
import java.util.HashMap;

// TODO(Salva): This is a PoC. This should be a conncetion to a SQL DB or call system

public class UsersDB {
  private Map<String, String> creedentials;

  public UsersDB() {
    this.creedentials = new HashMap<String, String>();
    this.creedentials.put("salva", "1234");
    this.creedentials.put("alvaro", "1234");
  }

  public boolean ValidCredentials(String user, String password) {
    return (this.creedentials.getOrDefault(user, null) == password);
  }
}
