package gitlet;

// TODO: any imports you need here
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.io.Serializable;
import java.util.Set;

/** Represents a gitlet stage object.
 *
 * @author TODO:
 * 1.Cope with the situation of adding a file that is already staged for addition.
 * 2.Cope with the situation of removing a file that is already staged for removal.
 *
 */
public class Stage implements Serializable {
    /** The blobs added to the stage. */
    private Map<String,String> addFiles;
    /** The blobs removed from the stage. */
    private Set<String> removeFiles;

    /** Crete a new stage. */
    public Stage() {
        addFiles = new HashMap<String,String>();
        removeFiles = new HashSet<String>();
    }

    /** Get the map of files added to the stage. */
    public void addFile(String filename, String blobsSHA1) {
        addFiles.put(filename, blobsSHA1);
    }

    /** Unstage a file from the stage. */
    public void unstageFile(String filename) {
        addFiles.remove(filename);
    }

    /** Check a files is in the addFiles of stage. */
    public boolean isAdded(String filename) {
        return addFiles.containsKey(filename);
    }

    /** Get the map of files removed from the stage. */
    public void removeFile(String filename) {
        removeFiles.add(filename);
    }

    /** Check a files is in the removeFiles of stage. */
    public boolean isRemoved(String filename) {
        return removeFiles.contains(filename);
    }

    /** Undo remove a file from the stage. */
    public void unRemoveFile(String filename) {
        removeFiles.remove(filename);
    }

    /** Get the map of files added to the stage. */
    public Map<String, String> getAddFiles() {
        return addFiles;
    }

    /** Get the set of files removed from the stage. */
    public Set<String> getRemoveFiles() {
        return removeFiles;
    }
}
