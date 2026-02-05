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
     * 1. add .gitletignore support in add command.
     * 2. refractor status function.
     * 3. handle recursion files and directories.
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
        updateHeadToBranch("master");

    }

    /** gitlet add [file name]
     * add blobs to the staging area. */
    public static void add(String fileName) {
        /** check if the repository is initialized. */
        checkInit();

        /** read the staging area. */
        Stage stage = readStage();

        /** stage the file. */
        Commit currentCommit = getCurrentCommit();
        stageSingleFile(fileName, stage, currentCommit);

        /** save the staging area. */
        Utils.writeObject(STAGING, stage);
    }

    /** gitlet add .
     * add all files in the current working directory to the staging area.
     */
    public static void addAll() {
        /** check if the repository is initialized. */
        checkInit();

        /** read the staging area. */
        Stage stage = readStage();

        /** get the current commit. */
        Commit currentCommit = getCurrentCommit();

        /** initialize an ignore list */
        GitletIgnore ignore = new GitletIgnore();

        /** get all files in the current working directory. */
        List<String> fileNames = plainFilenamesIn(CWD);
        if (fileNames == null) {
            throw error("No files to add.");
        }

        /** add each file to the staging area. */
        for (String fileName : fileNames) {
            /** ignore hidden files in .gitletIgnore */
            if (fileName.startsWith(".")) continue;
            if (ignore.isIgnored(fileName)) continue;

            stageSingleFile(fileName, stage, currentCommit);
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
            throw error("No reason to remove the file.");
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

        System.out.println();


        /** print the untracked files */
        List<String> untrackedFiles = findUntrackedFiles(getCurrentCommit());
        Collections.sort(untrackedFiles);
        System.out.println("=== Untracked Files ===");
        for (String file : untrackedFiles) {
            System.out.println(file);
        }
        System.out.println();
    }

    /** gitlet log
     * show the commit history.
     */
    public static void log() {
        /** check if the repository is initialized. */
        checkInit();

        /** get the current commit. */
        Commit currentCommit = getCurrentCommit();

        /** traverse the commit history. */
        while (currentCommit != null) {
            /** get the commit info. */
            currentCommit.printCommit();

            /** move to the parent commit. */
            String parentSHA1 = currentCommit.getFirstParent();
            if (parentSHA1 == null) {
                break;
            }
            currentCommit = getCommitBySHA1(parentSHA1);
        }
    }

    /** gitlet global-log
     * use BFS to show all commits in repository.
     * differs from CS61B's demand:
     * 1. it's similar to 'git log --all' in real git.
     *    will not print dangling commits.
     * 2. the order of commits is not strictly defined.
     * */
    public static void global_log() {
        /** check if the repository is initialized. */
        checkInit();

        /** get the latest version commit of every branch. */
        List<String> allBranchHeadName = plainFilenamesIn( join(REFS_DIR ,"heads"));
        List<String> branchHeadCommits = new ArrayList<>();
        for (String branchName : allBranchHeadName) {
            File headFile = join(REFS_DIR, "heads", branchName);
            branchHeadCommits.add(readContentsAsString(headFile).trim());
        }

        /** use a set to avoid duplicate commits. */
        Set<String> visitedCommits = new HashSet<>();

        /** use queue to store every commit hash to visit. */
        Queue<String> commitQueue = new LinkedList<>(branchHeadCommits);

        while (!commitQueue.isEmpty()) {
            /** get the current commit of the branch. */
            String currentCommitSHA1 = commitQueue.poll();
            Commit currentCommit = getCommitBySHA1(currentCommitSHA1);

            if (visitedCommits.contains(currentCommitSHA1))
                continue;

            /** get the commit info. */
            currentCommit.printCommit();
            visitedCommits.add(currentCommitSHA1);

            /** add parent commit(s) to the queue. */
            if (currentCommit.getFirstParent() != null) {
                commitQueue.add(currentCommit.getFirstParent());
            }
            if (currentCommit.getSecondParent() != null) {
                commitQueue.add(currentCommit.getSecondParent());
            }
        }
    }

    /** gitlet checkout -- [file name] */
    public static void checkoutFile(String fileName) {
        /** check if the repository is initialized. */
        checkInit();

        /** call checkoutCommit with the current commit ID. */
        String currentCommitID = readHEAD();
        checkoutCommit(currentCommitID, fileName);
    }

    /** gitlet checkout [commit id] -- [file name]
     * @param commitID: the commit id is the 6 digits prefix of SHA1 or full.
     */
    public static void checkoutCommit(String commitID, String fileName) {
        /** check if the repository is initialized. */
        checkInit();

        /** get the commit by commitID (6 digits prefix of SHA1)
         * if commitID is full size(40), directly get the commit. */
        Commit commit = commitID.length() == UID_LENGTH ?
                getCommitBySHA1(commitID) :
                getCommitByPrefixSHA1(commitID);

        /** restore files */
        restoreFilesFromCommit(commit, fileName);
    }

    /** gitlet checkout [branch name] */
    public static void checkoutBranch(String branchName) {
        /** check if the repository is initialized. */
        checkInit();

        /** get the branch ref file. */
        File branchRefFile = join(REFS_DIR, "heads", branchName);
        if (!branchRefFile.exists()) {
            throw error("No such branch exists.");
        }
        if (isCurrentBranch(branchName)) {
            throw error("No need to checkout the current branch.");
        }

        /** get the commit that the branch points to and current commit. */
        String targetCommitSHA1 = Utils.readContentsAsString(branchRefFile).trim();
        Commit targetCommit = getCommitBySHA1(targetCommitSHA1);
        Map<String, String> targetBlobs = targetCommit.getBlobs();

        Commit currentCommit = getCurrentCommit();
        Map<String, String> currentBlobs = currentCommit.getBlobs();

        /** check for untracked files that would be overwritten. */
        checkUntrackedConflict(targetCommit, currentCommit);

        /** delete files tracked in current commit
         * but not in target commit. */
        clearOldTrackedFiles(targetCommit, currentCommit);

        /** restore all files in the commit to the working directory. */
        for (String fileName : targetBlobs.keySet()) {
            restoreFilesFromCommit(targetCommit, fileName);
        }

        /** update HEAD to point to the target branch. */
        updateHeadToBranch(branchName);

        /** clear the staging area. */
        clearStage();
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

    /** aux function: stage a single file. */
    private static void stageSingleFile(String fileName, Stage stage, Commit currentCommit) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw error("File does not exist.");
        }

        /** read the file content and create a blob object. */
        byte[] fileContent = Utils.readContents(file);
        String fileSHA1 = Utils.sha1(fileContent);

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
     * situation: checkout -> saveHead("master")
     */
    public static void updateHeadToBranch(String branchName) {
        String refPath = String.join("/", "refs", "heads", branchName);
        writeContents(HEAD, refPath);
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
        return getCommitBySHA1(commitSHA1);
    }

    /** aux function: get a commit by its SHA1. */
    public static Commit getCommitBySHA1(String commitSHA1) {
        String dirName = commitSHA1.substring(0, 2);
        String fileName = commitSHA1.substring(2);
        File commitFile = join(OBJECTS_DIR, dirName, fileName);
        return readObject(commitFile, Commit.class);
    }

    /** aux function: get a commit by 6 digits prefix of SHA1. */
    public static Commit getCommitByPrefixSHA1(String commitSHA1) {
        if (commitSHA1.length() < 2) {
            throw error("Commit id must be at least 2 characters.");
        }

        String dirName = commitSHA1.substring(0, 2);
        String restPrefix = commitSHA1.substring(2);
        File commitDir = join(OBJECTS_DIR, dirName);

        if (!commitDir.exists() || !commitDir.isDirectory()) {
            throw error("No commit with that id exists.");
        }

        /** search for the commit file with the given prefix. */
        List<String> allFiles = plainFilenamesIn(commitDir);
        List<String> candidates = new ArrayList<>();

        for (String fileName : allFiles) {
            if (fileName.startsWith(restPrefix)) {
                String fullSHA1 = dirName + fileName;

                /** check if it's Commit instead of blobs */
                if (isCommit(fullSHA1)) {
                    candidates.add(fullSHA1);
                }
            }
        }

        /** check if there are 2 or more candidates. */
        if (candidates.isEmpty()) {
            throw error("No commit with that id exists.");
        } else if (candidates.size() > 1) {
            throw error("Ambiguous ID prefix: more than one commit matches.");
        }

        return getCommitBySHA1(candidates.get(0));
    }

    /** aux function: check if a SHA1 refers to a commit object. */
    private static boolean isCommit(String sha1) {
        String dirName = sha1.substring(0, 2);
        String fileName = sha1.substring(2);

        File file = join(OBJECTS_DIR, dirName, fileName); // 之前写的路径拼接函数
        try {
            readObject(file, Commit.class);
            return true;
        } catch (Exception e) {
            return false;
        }
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

    /** aux function: get SHA1 of file from commit,
     * then call writeBlobsToCWD() to restore it.
     */
    private static void restoreFilesFromCommit(Commit commit, String fileName) {
        String SHA1 = commit.getBlobSHA1(fileName);
        if (SHA1 == null) {
            throw error("File does not exist in that commit.");
        }
        writeBlobsToCWD(fileName, SHA1);
    }

    /** aux function: write blob content to CWD. */
    private static void writeBlobsToCWD(String fileName, String blobSHA1) {
        String dirName = blobSHA1.substring(0, 2);
        String filePart = blobSHA1.substring(2);
        File blobFile = join(OBJECTS_DIR, dirName, filePart);

        byte[] content = Utils.readContents(blobFile);

        File fileInCWD = join(CWD, fileName);

        Utils.writeContents(fileInCWD, content);
    }

    /** aux function: check if targetBranchName is current branch */
    public static boolean isCurrentBranch(String targetBranchName) {
        String headContent = Utils.readContentsAsString(HEAD).trim();
        File currentBranchFile = Utils.join(GITLET_DIR, headContent);
        File targetBranchFile = Utils.join(REFS_DIR, "heads", targetBranchName);
        return currentBranchFile.equals(targetBranchFile);
    }

    /** aux function: check for untracked files that would be overwritten. */
    private static void checkUntrackedConflict(Commit targetCommit, Commit currentCommit) {
        List<String> untrackedFiles = findUntrackedFiles(currentCommit);
        Map<String, String> targetBlobs = targetCommit.getBlobs();
        Map<String, String> currentBlobs = currentCommit.getBlobs();

        for (String untrackedFile : untrackedFiles) {
            if (targetBlobs.containsKey(untrackedFile)
                    && !currentBlobs.containsKey(untrackedFile)) {
                throw error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
    }

    /** aux function: find all untracked files.
     * @return: a list of all untracked files. */
    private static List<String> findUntrackedFiles(Commit commit) {
        List<String> untrackedFiles = new ArrayList<>();
        List<String> cwdFiles = plainFilenamesIn(CWD);
        GitletIgnore ignore = new GitletIgnore();
        if (cwdFiles == null) {
            throw error("No cwd files found.");
        }

        Map<String, String> trackedBlobs = commit.getBlobs();

        for (String fileName : cwdFiles) {
            if (!trackedBlobs.containsKey(fileName) && !ignore.isIgnored(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }


    /** aux function: delete files tracked in current commit
     * but not in target commit. */
    private static void clearOldTrackedFiles(Commit targetCommit, Commit currentCommit) {
        Map<String, String> currentBlobs = currentCommit.getBlobs();
        Map<String, String> targetBlobs = targetCommit.getBlobs();

        for (String fileName : currentBlobs.keySet()) {
            if (!targetBlobs.containsKey(fileName)) {
                Utils.restrictedDelete(join(CWD, fileName));
            }
        }
    }
}
