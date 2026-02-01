package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  This class listen to Main and deal with the
 *  operations of gitlet.
 *
 *
 *  @author BillyDu
 *
 */
public class Repository {
    /**
     * TODO:
     * 1. add Add feature
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The directory where commits and blobs are stored. */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The directory where heads are stored. */
    public static final File REFS_DIR = new File(GITLET_DIR, "refs");
    /** The directory where logs are stored. */
    public static final File LOGS_DIR = join(GITLET_DIR, "logs");
    /** The directory where stage located. */
    public static final File STAGING = join(GITLET_DIR, "staging");

    /** The HEAD pointer */
    public static final File HEAD = join(GITLET_DIR, "HEAD");


    /* TODO: fill in the rest of this class. */
    /** gitlet init
     * Initialize a gitlet repository in the current working directory. */
    public static void initRepo() {
        /** check if a gitlet repository already exists. */
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        REFS_DIR.mkdir();
        LOGS_DIR.mkdir();

        /** create an initial commit. */
        Commit initialCommit = new Commit("initial commit");

        /** save the initial commit to objects directory. */
        String commitSHA1 = saveCommit(initialCommit);

        /** create the master branch and point it to the initial commit. */
        File masterRef = join(REFS_DIR, "heads");
        masterRef.mkdir();
        File masterFile = join(masterRef, "master");
        Utils.writeContents(masterFile, commitSHA1);

        /** set HEAD to point to master branch. */
        Utils.writeContents(HEAD, "refs/heads/master");

    }

    /** gitlet add [file name]
     * add blobs to the staging area. */
    public static void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        /** read the staging area. */
        Stage stage = readStage();

        /** read the file content and create a blob object. */
        byte[] fileContent = Utils.readContents(file);
        String fileSHA1 = Utils.sha1(fileContent);

        /** get the current commit and check if the file exists. */
        Commit currentCommit = getCurrentCommit();
        if (currentCommit != null && currentCommit.isTracked(fileName) &&
                currentCommit.getBlobSHA1(fileName).equals(fileSHA1)) {

            /** Situation A: Revoke Changes
             * If the file is the same as the one in the current commit,
             * do not stage it and remove it from the staging area.
             * If it is staged for removal, unstage it.
             */
            if (stage.isAdded(fileName)) {
                stage.unstageFile(fileName);
            }

            if (stage.isRemoved(fileName)) {
                stage.unRemoveFile(fileName);
            }
        } else {

            /** Situation B: New Changes
             * If the file is different from the one in the current commit,
             * or is a new file, stage it for addition.
             * If it is staged for removal, unstage it.
             */
            stage.addFile(fileName, fileSHA1);

            if (stage.isRemoved(fileName)) {
                stage.unRemoveFile(fileName);
            }

            /** save the blob object to the objects directory. */
            saveBlob(fileContent);
        }

        /** save the staging area. */
        Utils.writeObject(STAGING, stage);
    }

    /** gitlet status
     * show the status of the repository. */
    public static void status() {
        Stage stage = readStage();

        /** print the staged files. */
        Map<String, String> addFiles = stage.getAddFiles();
        Set<String> addFileNames = addFiles.keySet();

        // sort the file names.
        List<String> addFileNamesList = new ArrayList<>(addFileNames);
        Collections.sort(addFileNamesList);

        System.out.println("=== Staged Files ===");
        for (String file : addFileNamesList) {
            System.out.println(file);
        }
    }

    /** aux function: read the staging area. */
    public static Stage readStage() {
        if (!STAGING.exists()) {
            return new Stage();
        }
        return Utils.readObject(STAGING, Stage.class);
    }

    /** aux function: read HEAD, and return the contents
     * that HEAD refers. */
    public static String readHEAD() {
        String headRef = Utils.readContentsAsString(HEAD).trim();
        File headFile = join(GITLET_DIR, headRef);
        if (!headFile.exists()) {
            return null;
        }
        return Utils.readContentsAsString(headFile);
    }

    /**
     * aux function：update HEAD
     * situation 1: checkout -> saveHead("master")
     * situation 2: Detached -> saveHead("a1b2c3...")
     */
    public static void saveHead(String headContent) {
        writeContents(HEAD, headContent);
    }

    /** aux function: save a blob object to the objects directory.
     */
    public static void saveBlob(byte[] content) {
        String SHA1 = Utils.sha1(content);
        /** we divide the SHA1 to two parts
         * to create a sub directory to store the commit file. */
        String dirName = SHA1.substring(0, 2);
        String fileName = SHA1.substring(2);

        /** create the store directory. */
        File dir = new File(OBJECTS_DIR, dirName);

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = join(dir, fileName);

        /** if file already exists, do nothing. */
        if (file.exists()) {
            return;
        }

        /** write the commit object to the file. */
        Utils.writeContents(file, content);
    }

    /** aux function: save a commit object to the objects directory.
     * @return : the SHA1 of the commit object.
     */
    public static String saveCommit(Commit commit) {
        /** serialize the commit object. */
        String SHA1 = Utils.sha1(Utils.serialize(commit));

        /** we divide the SHA1 to two parts
         * to create a sub directory to store the commit file. */
        String dirName = SHA1.substring(0, 2);
        String fileName = SHA1.substring(2);

        /** create the store directory. */
        File dir = new File(OBJECTS_DIR, dirName);

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = join(dir, fileName);

        /** write an empty content to the file. */
        Utils.writeObject(file, commit);

        return SHA1;
    }

    /** aux function: get the current commit. */
    public static Commit getCurrentCommit() {
        String commitSHA1 = readHEAD();
        if (commitSHA1 == null) {
            return null;
        }

        /** read the commit object from the objects directory. */
        String dirName = commitSHA1.substring(0, 2);
        String fileName = commitSHA1.substring(2);
        File commitFile = join(OBJECTS_DIR, dirName, fileName);
        return Utils.readObject(commitFile, Commit.class);
    }
}
