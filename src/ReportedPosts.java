import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ReportedPosts implements Serializable {
    public HashMap<Long, Integer> reportedPostsTimeStampsToNumReports = new HashMap<>();
    public HashSet<String> blockedPosters = new HashSet<>();
}
