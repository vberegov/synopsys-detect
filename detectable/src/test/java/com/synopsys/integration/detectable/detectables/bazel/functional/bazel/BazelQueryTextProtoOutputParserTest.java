package com.synopsys.integration.detectable.detectables.bazel.functional.bazel;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelQueryTextProtoOutputParser;

public class BazelQueryTextProtoOutputParserTest {

    @Test
    public void testParseGavFromTextProto() throws Exception {
        final File textProtoFile = new File("src/test/resources/detectables/functional/bazel/commons_io.textproto");
        final String textProtoString = FileUtils.readFileToString(textProtoFile, StandardCharsets.UTF_8);
        final BazelQueryTextProtoOutputParser parser = new BazelQueryTextProtoOutputParser();

        final String pathToAttributeObjectList = ":results:target:rule:attribute";
        final String gavObjectName = "artifact";
        final String gavFieldName = "string_value";

        final List<String> gavStrings = parser.parseStringValuesFromTextProto(pathToAttributeObjectList, gavObjectName, gavFieldName, textProtoString);
        assertEquals("org.apache.commons:commons-io:1.3.2", gavStrings.get(0));
    }
}
