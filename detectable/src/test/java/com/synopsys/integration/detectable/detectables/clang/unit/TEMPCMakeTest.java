package com.synopsys.integration.detectable.detectables.clang.unit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.synopsys.integration.detectable.detectables.clang.compilecommand.CompileCommand;
import com.synopsys.integration.detectable.detectables.clang.compilecommand.CompileCommandDatabaseParser;

public class TEMPCMakeTest {

    @Test
    public void test() throws IOException {

        final CompileCommandDatabaseParser parser = new CompileCommandDatabaseParser(new Gson());

        final File dir = new File("/Users/billings/Documents/projects/detect/cmake/linkfiles");
        final List<String> names = new ArrayList<>(1);
        names.add("link.txt");
        final NameFileFilter filenameFilter = new NameFileFilter(names);

        final Iterator<File> fileIterator = FileUtils.iterateFilesAndDirs(dir, filenameFilter, DirectoryFileFilter.DIRECTORY);
        while (fileIterator.hasNext()) {
            final File linkFile = fileIterator.next();
            if (linkFile.isFile()) {
                System.out.printf("File: %s: %s\n", linkFile.getAbsolutePath(), FileUtils.readFileToString(linkFile, StandardCharsets.UTF_8));

                List<CompileCommand> cmds = parser.parseCompileCommandDatabase(linkFile);
                for (CompileCommand cmd : cmds) {
                    System.out.printf("cmd: %s\n", cmd.command);
                }
            }
        }
    }
}
