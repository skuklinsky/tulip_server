import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.OutputStream;
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
                break;

            case "loginRequest":
                handleLoginRequest();
                break;

            case "signupRequest":
                handleSignupRequest();
                break;

            case "serverDowntimeAlert":
                handleServerDowntimeAlert();
                break;
        }
    }

    public void handleGetMainFeedPosts() {
        int numPostsBeingRequested = jsonReceived.get("numPostsBeingRequested").getAsInt();
        int numPostsAlreadyLoaded = jsonReceived.get("numPostsAlreadyLoaded").getAsInt();
        long lastPostTimePostSubmitted = jsonReceived.get("lastPostTimePostSubmitted").getAsLong();
        String category = jsonReceived.get("category").getAsString();
        String sortBy = jsonReceived.get("sortBy").getAsString();

        List<Poast> postsQueried = new ArrayList<>();

        if (sortBy.equals("New")) {

            int listSize = this.connectionThread.global.categoriesNewToPoasts.get(category).size();

            if (lastPostTimePostSubmitted == 0) { // numPostsAlreadyLoaded = 0, just load newest posts
                if (listSize != 0) {
                    int endIndex = Math.min(listSize, numPostsBeingRequested);
                    postsQueried = this.connectionThread.global.categoriesNewToPoasts.get(category).subList(0, endIndex);
                }
            } else {
                int index = 0;
                while ((postsQueried.size() < numPostsBeingRequested) && (index < listSize)) {
                    Poast p = this.connectionThread.global.categoriesNewToPoasts.get(category).get(index);
                    if (p.timePostSubmitted < lastPostTimePostSubmitted) { // if post is older than lastPostTimePostSubmitted, add it
                        postsQueried.add(p);
                    }
                }
            }
        } else if (sortBy.equals("Popular")) {
            int listSize = this.connectionThread.global.categoriesPopularToPoasts.get(category).size();
            if (listSize != 0 && (listSize > numPostsAlreadyLoaded)) {
                int endIndex = Math.min(listSize, numPostsAlreadyLoaded + numPostsBeingRequested);
                postsQueried = this.connectionThread.global.categoriesPopularToPoasts.get(category).subList(numPostsAlreadyLoaded, endIndex);
            }
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

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullySubmittedPost");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);

        ReadWrite.writeGlobalToFile(this.connectionThread.global, "globalObjectAsFile");
        ReadWrite.writeLoginInfoToFile(this.connectionThread.loginInfo, "loginInfoAsFile");

    }

    public void handleVoteOnPost() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        int voteIndex = jsonReceived.get("voteIndex").getAsInt();
        int numVotes = jsonReceived.get("numVotes").getAsInt();
        Poast post = this.connectionThread.global.timePostSubmittedToPoast.get(timePostSubmitted);

        if (post != null && voteIndex < post.votingOptions.length) {
            post.correspondingVotes[voteIndex] += numVotes;

            ReadWrite.writeGlobalToFile(this.connectionThread.global, "globalObjectAsFile");
            JsonObject jsonToSend = new JsonObject();
            jsonToSend.addProperty("instruction", "successfullyVoted");
            jsonToSend.addProperty("timePostSubmitted", post.timePostSubmitted);
            jsonToSend.addProperty("voteIndex", voteIndex);
            this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
        } else {
            JsonObject jsonToSend = new JsonObject();
            jsonToSend.addProperty("instruction", "votingAttemptFailed");
            this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
        }
    }

    public void handleLoginRequest() {
        String username = jsonReceived.get("username").getAsString();
        String password = jsonReceived.get("password").getAsString();

        String serverPassword = this.connectionThread.loginInfo.userNamesAndPasswords.get(username);

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "loginRequestResponse");
        jsonToSend.addProperty("username", username);
        jsonToSend.addProperty("username", username);

        if (password.equals(serverPassword)) {
            jsonToSend.addProperty("successfullyLoggedIn", true);
        } else {
            jsonToSend.addProperty("successfullyLoggedIn", false);
        }

        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleSignupRequest() {
        String username = jsonReceived.get("username").getAsString();
        String password = jsonReceived.get("password").getAsString();

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "signupRequestResponse");
        jsonToSend.addProperty("username", username);

        if (this.connectionThread.loginInfo.userNamesAndPasswords.containsKey(username)) {
            jsonToSend.addProperty("successfullyCreatedAccount", false);
        } else {
            this.connectionThread.loginInfo.userNamesAndPasswords.put(username, password);
            ReadWrite.writeLoginInfoToFile(this.connectionThread.loginInfo, "loginInfoObjectAsFile");
            jsonToSend.addProperty("successfullyCreatedAccount", true);
        }

        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleServerDowntimeAlert() {
        Integer minutesUntilDowntime = jsonReceived.get("minutesUntilDowntime").getAsInt();

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "serverDowntimeExpected");
        jsonToSend.addProperty("minutesUntilDowntime", minutesUntilDowntime);

        for (OutputStream activeOutputStream : this.connectionThread.setOfActiveOutputStreams) {
            this.connectionThread.global.sendMessage(jsonToSend, activeOutputStream);
        }
    }

}
