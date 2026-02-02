package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
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
            throw error("A Gitlet version-control system "
                    + "already exists in the current directory.");
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
        /** check if the repository is initialized. */
        checkInit();

        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw error("File does not exist.");
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

    /** gitlet commit [message]
     * create a new commit with the staged files.
     * TODO: handle the situation of 2 or more parents(merge).
     * */
    public static void commit(String message) {
        /** check if the repository is initialized. */
        checkInit();

        /** read the staging area. */
        Stage stage = readStage();

        /** check if there are staged files. */
        if (stage.getAddFiles().isEmpty() && stage.getRemoveFiles().isEmpty()) {
            throw error("No changes added to the commit.");
        }

        /** check if the message is valid. */
        if (message == null || message.trim().isEmpty()) {
            throw error("Please enter a commit message.");
        }

        /** get the current commit. */
        Commit currentCommit = getCurrentCommit();

        /** create a new blob mapping for the new commit. */
        Map<String, String> newBlobMapping = new HashMap<>(currentCommit.getBlobs());

        /** add the staged files and remove the removed files.
         * to the new blob mapping. */
        for (String fileName : stage.getAddFiles().keySet()) {
            String blobSHA1 = stage.getAddFiles().get(fileName);
            newBlobMapping.put(fileName, blobSHA1);
        }

        for (String fileName : stage.getRemoveFiles()) {
            newBlobMapping.remove(fileName);
        }

        /** create a new commit object. */
        List<String> parentCommits = new ArrayList<>();
        parentCommits.add(readHEAD());
        Commit newCommit = new Commit(message, parentCommits, newBlobMapping);

        /** save the new commit to the objects directory. */
        String newCommitSHA1 = saveCommit(newCommit);

        /** update the current branch to point to the new commit. */
        saveHead(newCommitSHA1);

        /** clear the staging area. */
        clearStage();
    }

    /** gitlet rm [file name]
     * remove a file from the staging area or
     * mark it for removal in the next commit. */
    public static void rm(String fileName) {
        /** check if the repository is initialized. */
        checkInit();

        File file = join(CWD, fileName);

        /** read the staging area and commit. */
        Stage stage = readStage();
        Commit currentCommit = getCurrentCommit();

        if (!stage.isAdded(fileName)
                && (currentCommit == null || !currentCommit.isTracked(fileName))) {
            System.out.println("No reason to remove the file.");
            return;
        }

        /** Situation A: Unstage a file
         * If the file is staged for addition, unstage it. */
        if (stage.isAdded(fileName)) {
            stage.unstageFile(fileName);
        }

        /** Situation B: Mark a file for removal
         * If the file is tracked in the current commit,
         * stage it for removal and delete it from the working directory
         * if it exists. */
        if (currentCommit != null && currentCommit.isTracked(fileName)) {
            stage.removeFile(fileName);

            if (file.exists()) {
                Utils.restrictedDelete(file);
            }
        }

        /** save the staging area. */
        Utils.writeObject(STAGING, stage);
    }

    /** gitlet status
     * show the status of the repository. */
    public static void status() {
        /** check if the repository is initialized. */
        checkInit();

        Stage stage = readStage();

        /** print the branches. */
        File headsDir = join(REFS_DIR, "heads");
        String currentHeadRef = Utils.readContentsAsString(HEAD).trim();

        Set<String> branchNames = new HashSet<>();
        for (File branchFile : Objects.requireNonNull(headsDir.listFiles())) {
            branchNames.add(branchFile.getName());
        }
        List<String> branchNamesList = sortNameList(branchNames);

        System.out.println("=== Branches ===");
        for (String branch : branchNamesList) {
            if (currentHeadRef.equals("refs/heads/" + branch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        /** print the staged files. */
        Map<String, String> addFiles = stage.getAddFiles();
        Set<String> addFileNames = addFiles.keySet();

        // sort the file names.
        List<String> addFileNamesList = sortNameList(addFileNames);

        System.out.println("=== Staged Files ===");
        for (String file : addFileNamesList) {
            System.out.println(file);
        }
        System.out.println();

        /** print the removed files. */
        Set<String> removeFileNames = stage.getRemoveFiles();
        List<String> removeFileNamesList = sortNameList(removeFileNames);

        System.out.println("=== Removed Files ===");
        for (String file : removeFileNamesList) {
            System.out.println(file);
        }
        System.out.println();

        /** print the modifications not staged for commit. */
        System.out.println("=== Modifications Not Staged For Commit ===");
    }

    /** gitlet log
     * show the commit history.
     * TODO: cope with the merge situation. */
    public static void log() {
        /** check if the repository is initialized. */
        checkInit();

        /** get the current commit. */
        Commit currentCommit = getCurrentCommit();

        /** traverse the commit history. */
        while (currentCommit != null) {
            /** get the commit info. */
            String commitSHA1 = sha1(serialize(currentCommit));
            String message = currentCommit.getMessage();
            Date date = currentCommit.getTimestamp();

            System.out.println("===");
            System.out.println("commit " + commitSHA1);
            /** TODO: cope with the merge situation here. */

            System.out.println("Date: " + formatDate(date));
            System.out.println(message);
            System.out.println();

            /** move to the parent commit. */
            String parentSHA1 = currentCommit.getFirstParent();
            if (parentSHA1 == null) {
                break;
            }
            currentCommit = getCommitBySHA1(parentSHA1);
        }
    }

    /** gitlet checkout -- [file name] */
    public static void checkoutFile(String fileName) {

    }

    /** gitlet checkout [commit id] -- [file name] */
    public static void checkoutCommit(String commitID, String fileName) {

    }

    /** gitlet checkout [branch name] */
    public static void checkoutBranch(String branchName) {

    }

    /** aux function: check if the repository is initialized. */
    private static void checkInit() {
        if (!GITLET_DIR.exists()) {
            throw error("Not in an initialized Gitlet directory.");
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
     * that HEAD refers, which is the SHA1 of current commit. */
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
    public static void updateHead(String headContent) {
        writeContents(HEAD, headContent);
    }

    /** aux function: save HEAD
     * situation: update the current branch to point to the new commit. */
    public static void saveHead(String commitSHA1) {
        String headRef = Utils.readContentsAsString(HEAD).trim();
        File headFile = join(GITLET_DIR, headRef);
        Utils.writeContents(headFile, commitSHA1);
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

    /** aux function: get a commit by its SHA1. */
    public static Commit getCommitBySHA1(String commitSHA1) {
        String dirName = commitSHA1.substring(0, 2);
        String fileName = commitSHA1.substring(2);
        File commitFile = join(OBJECTS_DIR, dirName, fileName);
        return Utils.readObject(commitFile, Commit.class);
    }

    /** aux function: clear the staging area. */
    public static void clearStage() {
        Stage emptyStage = new Stage();
        Utils.writeObject(Repository.STAGING, emptyStage);
    }

    /** sort name list */
    private static List<String> sortNameList(Set<String> names) {
        List<String> NamesList = new ArrayList<>(names);
        Collections.sort(NamesList);
        return NamesList;
    }

    /** aux function: format current date */
    public static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z",
                Locale.US);
        return formatter.format(date);
    }
}
