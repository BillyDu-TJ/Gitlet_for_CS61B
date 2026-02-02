package gitlet;

// TODO: any imports you need here
import java.io.Serializable;
import java.util.*;

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
    /** The parent(s) of this Commit.
     * parents[0] is the HEAD parent
     * parents[1...] are merged parents. */
    private List<String> parents;
    /** The mapping of file names to blob SHA-1s in this Commit. */
    private Map<String, String> blobs;

    /* TODO: fill in the rest of this class. */
    /** create a new commit. Default constructor
     * is used for init a gitlet vault. */
    public Commit(String createMessage) {
        message = createMessage;
        timestamp = new Date(0);
        parents = new ArrayList<>();
        blobs = new HashMap<>();
    }

    /** create a new commit with all info. */
    public Commit(String createMessage, List<String> parentCommits,
                  Map<String, String> blobMapping) {
        message = createMessage;
        timestamp = new Date();
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

    /** get the mapping of blobs in this commit. */
    public Map<String, String> getBlobs() {
        if (blobs == null) {
            return new HashMap<>();
        }
        return blobs;
    }

    /** get the first parent of this commit
     * which is the HEAD parent. */
    public String getFirstParent() {
        if (parents == null || parents.isEmpty())
            return null;
        return parents.getFirst();
    }

    /** get the message of this commit. */
    public String getMessage() {
        return message;
    }

    /** get the timestamp of this commit. */
    public Date getTimestamp() {
        return timestamp;
    }
}
