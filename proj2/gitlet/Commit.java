package gitlet;

// TODO: any imports you need here
import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The timestamp of this Commit. */
    private Date timestamp;
    /** The parent(s) of this Commit. */
    private List<String> parents;
    /** The mapping of file names to blob SHA-1s in this Commit. */
    private Map<String, String> blobs;

    /* TODO: fill in the rest of this class. */
    /** create a new commit. Default constructor
     * is used for init a gitlet vault. */
    public Commit(String createMessage) {
        message = createMessage;
        timestamp = new Date(0);
        parents = null;
        blobs = new HashMap<>();
    }

    /** create a new commit with all info. */
    public Commit(String createMessage, Date time, List<String> parentCommits,
                  Map<String, String> blobMapping) {
        message = createMessage;
        timestamp = time;
        parents = parentCommits;
        blobs = blobMapping;
    }

    /** check if a file is tracked in this commit. */
    public boolean isTracked(String filename) {
        return blobs.containsKey(filename);
    }

    /** get the blob SHA1 of a file in this commit. */
    public String getBlobSHA1(String filename) {
        return blobs.get(filename);
    }
}
