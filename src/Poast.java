import java.io.Serializable;

public class Poast implements Serializable {

    String title;
    String message;
    String[] votingOptions;
    Integer[] correspondingVotes;
    String category;
    String age;
    String gender;
    String posterUsername;
    long timePostSubmitted;

    public Poast(String title, String message, String[] votingOptions, Integer[] correspondingVotes, String category, String age, String gender, String posterUsername, long timePostSubmitted) {
        this.title = title;
        this.message = message;
        this.votingOptions = votingOptions;
        this.correspondingVotes = correspondingVotes;
        this.category = category;
        this.age = age;
        this.gender = gender;
        this.posterUsername = posterUsername;
        this.timePostSubmitted = timePostSubmitted;
    }
}
