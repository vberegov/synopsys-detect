package com.synopsys.integration.detectable.detectables.docker.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detectable.detectables.docker.DockerDetectableOptions;
import com.synopsys.integration.detectable.detectables.docker.DockerProperties;
import com.synopsys.integration.log.LogLevel;

public class DockerPropertiesTest {

    @Test
    public void test() throws IOException {
        File tempDir = FileUtils.getTempDirectory();
        File propertiesFile = new File(tempDir, "application.properties");
        File outputDir = new File(tempDir, "output");
        outputDir.mkdirs();

        final boolean dockerPathRequired = false;
        final String suppliedDockerImage = "alpine:latest";
        final String suppliedDockerImageId = "";
        final String suppliedDockerTar = "";
        final LogLevel dockerInspectorLoggingLevel = LogLevel.DEBUG;
        final String dockerInspectorVersion = "1.2.3";
        Map<String, String> additionalDockerProperties = new HashMap<>(0);
        Path dockerInspectorPath = null;
        final String dockerPlatformTopLayerId = "";
        DockerDetectableOptions dockerDetectableOptions = new DockerDetectableOptions(dockerPathRequired,
            suppliedDockerImage, suppliedDockerImageId, suppliedDockerTar,
            dockerInspectorLoggingLevel, dockerInspectorVersion, additionalDockerProperties,
            dockerInspectorPath, dockerPlatformTopLayerId);

        DockerProperties dockerProperties = new DockerProperties(dockerDetectableOptions);
        dockerProperties.populatePropertiesFile(propertiesFile, outputDir);

        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFile));
        assertEquals("DEBUG", properties.getProperty("logging.level.com.synopsys"));
        assertEquals("Detect", properties.getProperty("caller.name"));
        assertEquals("true", properties.getProperty("output.include.containerfilesystem"));
        assertEquals("false", properties.getProperty("upload.bdio"));
        assertEquals("false", properties.getProperty("phone.home"));
    }
}
