import java.io.Serializable;
import java.util.HashMap;

public class LoginInfo implements Serializable {
    public HashMap<String, String> userNamesAndPasswords = new HashMap<>();
}
