import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClientInstructionHandler {

    private JsonObject jsonReceived;
    private ConnectionThread connectionThread;
    Gson gson = new Gson();


    public ClientInstructionHandler(JsonObject jsonReceived, ConnectionThread connectionThread) {
        this.jsonReceived = jsonReceived;
        this.connectionThread = connectionThread;
    }

    public void handleInstruction() {
        String instruction = jsonReceived.get("instruction").getAsString();

        switch (instruction) {
            case "getMainFeedPosts":
                handleGetMainFeedPosts();
                break;

            case "getMyPosts":
                handleGetMyPosts();
                break;

            case "submitPost":
                handleSubmitPost();
                break;

            case "voteOnPost":
                handleVoteOnPost();
        }
    }

    public void handleGetMainFeedPosts() {
        String category = jsonReceived.get("category").getAsString();
        String sortBy = jsonReceived.get("sortBy").getAsString();

        List<Poast> postsQueried;

        if(sortBy.equals("Popular")) {
            postsQueried = this.connectionThread.global.categoriesPopularToPoasts.get(category);
        } else {
            postsQueried = this.connectionThread.global.categoriesNewToPoasts.get(category);
        }

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "getMainFeedPostsResponse");

        if (postsQueried.size() > 0) {
            ArrayList<String> listOfPostsAsStrings = new ArrayList<>();
            for (Poast post: postsQueried) {
                listOfPostsAsStrings.add(this.connectionThread.global.convertPoastToString(post));
            }
            jsonToSend.addProperty("posts", this.gson.toJson(listOfPostsAsStrings));
        }

        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleGetMyPosts() {
        String username = jsonReceived.get("username").getAsString();
        LinkedList<Poast> posts = this.connectionThread.global.usernamesToPosts.get(username);

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "getMyPostsResponse");

        if (posts != null) {
            ArrayList<String> listOfPostsAsStrings = new ArrayList<>();
            for (Poast post: posts) {
                listOfPostsAsStrings.add(this.connectionThread.global.convertPoastToString(post));
            }
            jsonToSend.addProperty("posts", this.gson.toJson(listOfPostsAsStrings));
        }
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleSubmitPost() {
        String postTitle = jsonReceived.get("title").getAsString();
        String postMessage = jsonReceived.get("message").getAsString();
        String[] votingOptions = this.gson.fromJson(jsonReceived.get("votingOptions").getAsString(), String[].class);
        Integer[] correspondingVotes = new Integer[votingOptions.length];
        String category = jsonReceived.get("category").getAsString();
        String age = jsonReceived.get("age").getAsString();
        String gender = jsonReceived.get("gender").getAsString();
        String posterUsername = jsonReceived.get("posterUsername").getAsString();

        if (age.equals("")) {
            age = null;
        }
        if (gender.equals("")) {
            gender = null;
        }
        for (int i = 0; i < correspondingVotes.length; i++) {
            correspondingVotes[i] = 0;
        }

        long timePostSubmitted = System.currentTimeMillis();
        Poast post = new Poast(postTitle, postMessage, votingOptions, correspondingVotes, category, age, gender, posterUsername, timePostSubmitted);

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullySubmittedPost");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);

        this.connectionThread.global.categoriesPopularToPoasts.get(category).add(post);
        this.connectionThread.global.categoriesNewToPoasts.get(category).addFirst(post);

        LinkedList<Poast> allPostsFromUsername = this.connectionThread.global.usernamesToPosts.get(posterUsername);
        if (allPostsFromUsername == null) {
            allPostsFromUsername = new LinkedList<>();
            allPostsFromUsername.add(post);
            this.connectionThread.global.usernamesToPosts.put(posterUsername, allPostsFromUsername);
        } else {
            allPostsFromUsername.addFirst(post);
        }

        this.connectionThread.global.timePostSubmittedToPoast.put(timePostSubmitted, post);

        ReadWrite.writeGlobalToFile(this.connectionThread.global, "globalObjectAsFile");

    }

    public void handleVoteOnPost() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        int voteIndex = jsonReceived.get("voteIndex").getAsInt();
        Poast post = this.connectionThread.global.timePostSubmittedToPoast.get(timePostSubmitted);

        if (post != null && voteIndex < post.votingOptions.length) {
            post.correspondingVotes[voteIndex] += 1;
        }

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullyVoted");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

}
