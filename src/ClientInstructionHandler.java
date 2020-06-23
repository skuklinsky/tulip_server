import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.util.*;

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

            case "reportPost":
                handleReportPost();
                break;

            case "getReportedPosts":
                handleGetReportedPosts();
                break;

            case "deletePost":
                handleDeletePost();
                break;

            case "blockPoster":
                handleBlockPoster();
                break;

            case "reportedPostIsOk":
                handleReportedPostIsOk();
                break;
        }
    }

    public void handleGetMainFeedPosts() {
        int numPostsBeingRequested = jsonReceived.get("numPostsBeingRequested").getAsInt();
        int numPostsAlreadyLoaded = jsonReceived.get("numPostsAlreadyLoaded").getAsInt();
        long lastPostTimePostSubmitted = jsonReceived.get("lastPostTimePostSubmitted").getAsLong();
        boolean fullRefresh = jsonReceived.get("fullRefresh").getAsBoolean(); // true if refreshing, false if requesting more posts as part of infinite scroll
        String category = jsonReceived.get("category").getAsString();
        String sortBy = jsonReceived.get("sortBy").getAsString();

        List<Poast> postsQueried = new ArrayList<>();

        if (sortBy.equals("New")) {

            int listSize = this.connectionThread.global.postsNew.size();
            int index = 0;
            while ((postsQueried.size() < numPostsBeingRequested) && (index < listSize)) {
                Poast post = this.connectionThread.global.postsNew.get(index);
                if ((category.equals("All") || post.category.equals(category)) && ((post.timePostSubmitted < lastPostTimePostSubmitted) || (lastPostTimePostSubmitted == 0))) { // if post is older than lastPostTimePostSubmitted
                    postsQueried.add(post);
                }
                index += 1;
            }
        } else if (sortBy.equals("Popular")) {

            int listSize = this.connectionThread.global.postsPopular.size();
            int index = 0;
            int numPostsSeenThatFitCriteria = 0;

            // move index along in order to skip over posts already seen. For example, if already seen 10 posts in category x, keep adding to index until it has passed 10 posts of category x
            while ((numPostsSeenThatFitCriteria < numPostsAlreadyLoaded) && (index < listSize)) {
                Poast post = this.connectionThread.global.postsPopular.get(index);
                if (category.equals("All") || post.category.equals(category)) {
                    numPostsSeenThatFitCriteria += 1;
                }
                index += 1;
            }

            while ((postsQueried.size() < numPostsBeingRequested) && (index < listSize)) {
                Poast post = this.connectionThread.global.postsPopular.get(index);
                if (category.equals("All") || post.category.equals(category)) {
                    postsQueried.add(post);
                }
                index += 1;
            }
        }

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "getMainFeedPostsResponse");
        jsonToSend.addProperty("fullRefresh", fullRefresh);

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
        int[] correspondingVotes = new int[votingOptions.length]; // autofills with zeros
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

        if (this.connectionThread.reportedPosts.blockedPosters.contains(posterUsername)) {
            JsonObject jsonToSend = new JsonObject();
            jsonToSend.addProperty("instruction", "youAreBlockedFromPosting");
            this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
            return;
        }

        long timePostSubmitted = System.currentTimeMillis();
        Poast post = new Poast(postTitle, postMessage, votingOptions, correspondingVotes, category, age, gender, posterUsername, timePostSubmitted);

        this.connectionThread.global.postsPopular.add(post);
        this.connectionThread.global.postsNew.addFirst(post);

        this.connectionThread.global.timePostSubmittedToPoast.put(timePostSubmitted, post);

        LinkedList<Poast> allPostsFromUsername = this.connectionThread.global.usernamesToPosts.getOrDefault(posterUsername, new LinkedList<>());
        allPostsFromUsername.addFirst(post);
        if (allPostsFromUsername.size() == 1) { // just created a new LinkedList, need to add to hashMap
            this.connectionThread.global.usernamesToPosts.put(posterUsername, allPostsFromUsername);
        }

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullySubmittedPost");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);

        Collections.sort(this.connectionThread.global.postsPopular);

        ReadWrite.writeGlobalToFile(this.connectionThread.global, "globalObjectAsFile");
    }

    public void handleVoteOnPost() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        int voteIndex = jsonReceived.get("voteIndex").getAsInt();
        int numVotes = jsonReceived.get("numVotes").getAsInt();
        Poast post = this.connectionThread.global.timePostSubmittedToPoast.get(timePostSubmitted);

        if (post != null && voteIndex < post.votingOptions.length) {
            post.correspondingVotes[voteIndex] += numVotes;
            post.totalVotes += numVotes;

            JsonObject jsonToSend = new JsonObject();
            jsonToSend.addProperty("instruction", "successfullyVoted");
            jsonToSend.addProperty("timePostSubmitted", post.timePostSubmitted);
            jsonToSend.addProperty("voteIndex", voteIndex);
            this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);

            Collections.sort(this.connectionThread.global.postsPopular);
            ReadWrite.writeGlobalToFile(this.connectionThread.global, "globalObjectAsFile");
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

    public void handleReportPost() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        int numReports = this.connectionThread.reportedPosts.reportedPostsTimeStampsToNumReports.getOrDefault(timePostSubmitted, 0);
        this.connectionThread.reportedPosts.reportedPostsTimeStampsToNumReports.put(timePostSubmitted, numReports + 1);

        ReadWrite.writeReportedPostsToFile(this.connectionThread.reportedPosts, "reportedPostsObjectAsFile");

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullyReportedPost");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleGetReportedPosts() {

        ArrayList<Poast> posts = new ArrayList<>();
        ArrayList<Integer> numReports = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : this.connectionThread.reportedPosts.reportedPostsTimeStampsToNumReports.entrySet()) {
            posts.add(this.connectionThread.global.timePostSubmittedToPoast.get(entry.getKey()));
            numReports.add(entry.getValue());
        }

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "getReportedPostsResponse");

        ArrayList<String> listOfPostsAsStrings = new ArrayList<>();
        for (Poast post: posts) {
            listOfPostsAsStrings.add(this.connectionThread.global.convertPoastToString(post));
        }
        jsonToSend.addProperty("posts", this.gson.toJson(listOfPostsAsStrings));
        jsonToSend.addProperty("numReports", this.gson.toJson(numReports));

        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleDeletePost() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        Poast p = this.connectionThread.global.timePostSubmittedToPoast.get(timePostSubmitted);

        this.connectionThread.reportedPosts.reportedPostsTimeStampsToNumReports.remove(timePostSubmitted);
        this.connectionThread.global.postsPopular.remove(p);
        this.connectionThread.global.postsNew.remove(p);
        this.connectionThread.global.timePostSubmittedToPoast.remove(timePostSubmitted);
        this.connectionThread.global.usernamesToPosts.get(p.posterUsername).remove(p);

        ReadWrite.writeReportedPostsToFile(this.connectionThread.reportedPosts, "reportedPostsObjectAsFile");
        ReadWrite.writeGlobalToFile(this.connectionThread.global, "globalObjectAsFile");

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullyDeletedPost");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleBlockPoster() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        Poast p = this.connectionThread.global.timePostSubmittedToPoast.get(timePostSubmitted);

        this.connectionThread.reportedPosts.blockedPosters.add(p.posterUsername);
        ReadWrite.writeReportedPostsToFile(this.connectionThread.reportedPosts, "reportedPostsObjectAsFile");

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullyBlockedPoster");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

    public void handleReportedPostIsOk() {
        Long timePostSubmitted = jsonReceived.get("timePostSubmitted").getAsLong();
        Poast p = this.connectionThread.global.timePostSubmittedToPoast.get(timePostSubmitted);

        this.connectionThread.reportedPosts.reportedPostsTimeStampsToNumReports.remove(timePostSubmitted);
        ReadWrite.writeReportedPostsToFile(this.connectionThread.reportedPosts, "reportedPostsObjectAsFile");

        JsonObject jsonToSend = new JsonObject();
        jsonToSend.addProperty("instruction", "successfullyMarkedReportedPostAsOk");
        this.connectionThread.global.sendMessage(jsonToSend, this.connectionThread.outputStream);
    }

}
