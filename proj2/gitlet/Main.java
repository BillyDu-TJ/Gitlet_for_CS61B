package gitlet;

import java.util.Objects;

import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author BillyDu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];

        try {
            switch (firstArg) {
                case "init":
                    validateNumArgs(args, 1);
                    Repository.initRepo();
                    break;
                case "add":
                    validateNumArgs(args, 2);
                    String filenameToAdd = args[1];
                    if (Objects.equals(filenameToAdd, ".")) {
                        Repository.addAll();
                    }
                    else {
                        Repository.add(filenameToAdd);
                    }
                    break;
                case "status":
                    validateNumArgs(args, 1);
                    Repository.status();
                    break;
                case "commit":
                    validateNumArgs(args, 2);
                    String message = args[1];
                    Repository.commit(message);
                    break;
                case "log":
                    Repository.log();
                    break;
                case "global-log":
                    Repository.global_log();
                    break;
                case "rm":
                    validateNumArgs(args, 2);
                    String filenameToRm = args[1];
                    Repository.rm(filenameToRm);
                    break;
                case "checkout":
                    handleCheckout(args);
                    break;
                case "branch":
                    //TODO: implement branch
                    break;
                case "rm-branch":
                    //TODO: implement rm-branch
                    break;
                case "reset":
                    //TODO: implement reset
                    break;
                case "merge":
                    //TODO: implement merge
                    break;
                default:
                    throw error("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Validate the number of arguments provided.
     *  @param args the array of arguments
     *  @param num the expected number of arguments
     */
    public static void validateNumArgs(String[] args, int num) {
        if (args.length != num) {
            throw error("Incorrect operands.");
        }
    }

    /** handle checkout
     * TODO: write checkout in Repository class
     * */
    public static void handleCheckout(String[] args) {
        if (args.length == 3) {
            // checkout -- [file name]
            if (!args[1].equals("--")) {
                throw error("Incorrect operands.");
            }
            Repository.checkoutFile(args[2]);
        } else if (args.length == 4) {
            // checkout [commit id] -- [file name]
            if (!args[2].equals("--")) {
                throw error("Incorrect operands.");
            }
            Repository.checkoutCommit(args[1], args[3]);
        } else if (args.length == 2) {
            // checkout [branch name]
            Repository.checkoutBranch(args[1]);
        } else {
            throw error("Incorrect operands.");
        }
    }
}
