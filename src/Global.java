import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;

public class Global implements Serializable {

    public HashMap<String, ArrayList<Poast>> categoriesPopularToPoasts = new HashMap<>();
    public HashMap<String, LinkedList<Poast>> categoriesNewToPoasts = new HashMap<>();
    public HashMap<Long, Poast> timePostSubmittedToPoast = new HashMap<>();
    public HashMap<String, LinkedList<Poast>> usernamesToPosts = new HashMap<>();

    public HashSet<OutputStream> setOfActiveOutputStreams = new HashSet<>();

    public Global() {

        categoriesPopularToPoasts.put("All", new ArrayList<>());
        categoriesPopularToPoasts.put("Is he interested", new ArrayList<>());
        categoriesPopularToPoasts.put("Is she interested", new ArrayList<>());
        categoriesPopularToPoasts.put("Should I break up with her", new ArrayList<>());

        categoriesNewToPoasts.put("All", new LinkedList<>());
        categoriesNewToPoasts.put("Is he interested", new LinkedList<>());
        categoriesNewToPoasts.put("Is she interested", new LinkedList<>());
        categoriesNewToPoasts.put("Should I break up with her", new LinkedList<>());

    }

    public void sendMessage(JsonObject jsonToSend, OutputStream outputStream) {
        Gson gson = new Gson();
        String messageToSend = gson.toJson(jsonToSend);
        byte[] lengthHeader = ByteBuffer.allocate(4).putInt(messageToSend.length()).array(); // first 4 bytes indicate length of message
        byte[] data = messageToSend.getBytes();
        try {
            outputStream.write(lengthHeader);
            outputStream.write(data);
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
