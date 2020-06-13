import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;

public class Global implements Serializable {

    public ArrayList<Poast> postsPopular = new ArrayList<>();
    public LinkedList<Poast> postsNew = new LinkedList<>();

    public HashMap<Long, Poast> timePostSubmittedToPoast = new HashMap<>();
    public HashMap<String, LinkedList<Poast>> usernamesToPosts = new HashMap<>();

    public Global() {

    }

    public void sendMessage(JsonObject jsonToSend, OutputStream outputStream) {
        Gson gson = new Gson();
        String messageToSend = gson.toJson(jsonToSend);

        byte[] asBytes = messageToSend.getBytes();
        byte[] lengthHeader = ByteBuffer.allocate(4).putInt(asBytes.length).array();

        try {
            outputStream.write(lengthHeader);
            outputStream.write(asBytes);
        } catch (IOException e) {
            System.out.println("IOException. Error sending message");
            return;
        }
        System.out.println("Message sent with instruction: " + jsonToSend.get("instruction"));
    }

    public String convertPoastToString(Poast post) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", post.title);
        jsonObject.addProperty("message", post.message);
        jsonObject.addProperty("votingOptions", gson.toJson(post.votingOptions));
        jsonObject.addProperty("correspondingVotes", gson.toJson(post.correspondingVotes));
        jsonObject.addProperty("category", post.category);
        jsonObject.addProperty("age", post.age);
        jsonObject.addProperty("gender", post.gender);
        jsonObject.addProperty("posterUsername", post.posterUsername);
        jsonObject.addProperty("timePostSubmitted", post.timePostSubmitted);

        return gson.toJson(jsonObject);
    }
}
