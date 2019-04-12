package com.synopsys.integration.detectable.detectables.cmake;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.synopsys.integration.detectable.detectables.clang.compilecommand.CompileCommand;
import com.synopsys.integration.detectable.detectables.clang.compilecommand.CompileCommandDatabaseParser;
import com.synopsys.integration.detectable.detectables.clang.compilecommand.CompileCommandParser;

public class TEMPCMakeTest {

    @Test
    public void testAr() throws IOException {
        final File buildDir = new File("/Users/billings/Documents/projects/detect/cmake/linkfiles");
        final File sourceDir = new File("/Users/billings/Documents/projects/detect/cmake/linkfiles");
        final File dir = new File("/Users/billings/Documents/projects/detect/cmake/linkfiles");


        final Set<String> commands = new HashSet<>();

        final CompileCommandParser parser = new CompileCommandParser();


        final List<String> names = new ArrayList<>(1);
        names.add("link.txt");
        final NameFileFilter filenameFilter = new NameFileFilter(names);

        final Iterator<File> fileIterator = FileUtils.iterateFilesAndDirs(dir, filenameFilter, DirectoryFileFilter.DIRECTORY);
        while (fileIterator.hasNext()) {
            final File linkFile = fileIterator.next();
            if (linkFile.isFile()) {
                final File linkFileDir = linkFile.getParentFile();
                System.out.printf("Dir: %s\n", linkFileDir.getAbsolutePath());
                final String commandString = FileUtils.readFileToString(linkFile, StandardCharsets.UTF_8);
//                System.out.printf("File: %s: %s\n", linkFile.getAbsolutePath(), commandString);
                final List<String> cmdTokens = parser.parseCommandString(commandString, new HashMap<>(0));
//                System.out.printf("Command: %s\n", cmdTokens.get(0));

                if (isThisCmd(cmdTokens.get(0), "gcc")) {
                    commands.add("gcc");
                    int index = 0;
                    for (final String cmdToken : cmdTokens) {
                        if ((index > 0) && (isLinkedFilePathGcc(cmdToken))) {
                            System.out.printf("Linked file: '%s:%s'\n", linkFileDir.getCanonicalPath(), cmdToken);
                            final File resolvedLinkedFile = Paths.get(linkFileDir.getCanonicalPath(), cmdToken).toFile();
                            System.out.printf("Resolved linked file: '%s'\n", resolvedLinkedFile.getCanonicalPath());
                        }
                        index++;
                    }
                } else if (isThisCmd(cmdTokens.get(0), "ar")) {
                    commands.add("ar");
                } else {
                    commands.add(cmdTokens.get(0));
                }
            }
        }

        for (String cmd : commands) {
            System.out.printf("Command: %s\n", cmd);
        }
    }

    private boolean isThisCmd(final String commandPath, final String command) {
        if (commandPath.equals(command) || commandPath.endsWith(String.format("/%s", command))) {
            return true;
        }
        return  false;
    }

    private boolean isLinkedFilePathGcc(final String filePathCandidate) {
        if (filePathCandidate.startsWith("-")) {
            return false;
        }
        if (filePathCandidate.endsWith(".")) {
            return false;
        }
        final int lastDotIndex = filePathCandidate.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return true;
        }
        final String suffix = filePathCandidate.substring(lastDotIndex+1);
        if (isNonLinkedFileSuffix(suffix)) {
            return false;
        }
        return true;
    }

    private final List<String> nonLinkedFileSuffixesGcc = Arrays.asList("c", "i", "ii", "m", "mi", "mm", "M", "mii", "h", "cc", "cp", "cxx", "cpp", "CPP", "c++", "C",
        "hh", "H", "hp", "hxx", "hpp", "HPP", "h++", "tcc", "f", "for", "ftn", "F", "FOR", "fpp", "FPP", "FTN", "f90", "f95", "f03", "f08", "F90", "F95", "F03",
        "F08", "go", "brig", "d", "di", "dd", "ads", "adb", "s", "S", "sx");

    private boolean isNonLinkedFileSuffix(final String suffix) {
        return nonLinkedFileSuffixesGcc.contains(suffix);
    }
}
