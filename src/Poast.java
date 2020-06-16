import java.io.Serializable;

public class Poast implements Serializable, Comparable<Poast> {

    String title;
    String message;
    String[] votingOptions;
    int[] correspondingVotes;
    int totalVotes = 0;
    String category;
    String age;
    String gender;
    String posterUsername;
    long timePostSubmitted;

    public Poast(String title, String message, String[] votingOptions, int[] correspondingVotes, String category, String age, String gender, String posterUsername, long timePostSubmitted) {
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

    @Override
    public int compareTo(Poast o) {
        // return negative if this object is "less than" Poast o
        // return zero if this object is "equal to" Poast o
        // return positive if this object is "greater than" Poast o

        // want more popular posts to be first in list, so should be "less than" other posts. If more popular, return negative

        double daysSinceThisPostSubmitted = Math.max((System.currentTimeMillis() - this.timePostSubmitted) / 86400000.0, 4); // minimum of 4 hours so that can't have super popular post because was voted on once within a minute
        double daysSinceOtherPostedSubmitted = Math.max((System.currentTimeMillis() - o.timePostSubmitted) / 86400000.0, 4);
        double popularityThis = ((double) this.totalVotes) / daysSinceThisPostSubmitted;
        double popularityOther = ((double) o.totalVotes) / daysSinceOtherPostedSubmitted;

        return popularityThis > popularityOther ? -1 : 1;
    }
}
