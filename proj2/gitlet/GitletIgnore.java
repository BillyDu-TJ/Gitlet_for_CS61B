package gitlet;


import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static gitlet.Utils.*;

/** Represents a gitlet .gitletignore object.
 *  Recently, only supports specific file names to be ignored.
 *  @author BillyDu
 */
public class GitletIgnore {
    private final Set<String> ignoredFiles = new HashSet<String>();

    public GitletIgnore() {
        ignoredFiles.add(".gitlet");

        /** load ignored files from .gitletignore file if exists */
        File ignoreFile = join(Repository.CWD, ".gitletignore");
        if (ignoreFile.exists()) {
            List<String> lines = readContentsAsString(ignoreFile).lines().toList();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    ignoredFiles.add(line);
                }
            }
        }
    }

    /** Check if a file is ignored. */
    public boolean isIgnored(String filename) {
        return ignoredFiles.contains(filename);
    }
}
