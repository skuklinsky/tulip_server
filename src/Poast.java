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

        long daysSinceThisPostSubmittedRoundedUp = ((System.currentTimeMillis() - this.timePostSubmitted) / 86400000) + 1;
        long daysSinceOtherPostedSubmittedRoundedUp = ((System.currentTimeMillis() - o.timePostSubmitted) / 86400000) + 1;
        double popularityThis = ((double) this.totalVotes) / daysSinceThisPostSubmittedRoundedUp;
        double popularityOther = ((double) o.totalVotes) / daysSinceOtherPostedSubmittedRoundedUp;

        return popularityThis > popularityOther ? -1 : 1;
    }
}
