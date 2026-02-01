package gitlet;

import gitlet.Utils.*;

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
                    Repository.initRepo();
                    break;
                case "add":
                    String filenameToAdd = args[1];
                    Repository.add(filenameToAdd);
                    break;
                case "status":
                    Repository.status();
                    break;
                case "commit":
                    String message = args[1];
                    Repository.commit(message);
                    break;
                case "log":
                    //TODO: implement log
                    break;
                case "rm":
                    String filenameToRm = args[1];
                    Repository.rm(filenameToRm);
                    break;
                case "checkout":
                    //TODO: implement checkout
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
                    System.out.println("No command with that name exists.");
                    break;
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
