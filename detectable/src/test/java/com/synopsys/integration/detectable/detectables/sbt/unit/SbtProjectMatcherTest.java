package com.synopsys.integration.detectable.detectables.sbt.unit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import com.paypal.digraph.parser.GraphParser;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectables.sbt.dot.SbtDotGraphNodeParser;
import com.synopsys.integration.detectable.detectables.sbt.dot.SbtProjectMatcher;

public class SbtProjectMatcherTest {

    private GraphParser createGraphParser(String actualGraph) {
        String simpleGraph = "digraph \"dependency-graph\" {\n"
                                 + "    graph[rankdir=\"LR\"]\n"
                                 + "    edge [\n"
                                 + "        arrowtail=\"none\"\n"
                                 + "    ]\n"
                                 + actualGraph + "\n"
                                 + "\n"
                                 + "}";
        InputStream stream = new ByteArrayInputStream(simpleGraph.getBytes(StandardCharsets.UTF_8));
        return new GraphParser(stream);
    }

    private String node(String org, String name, String version) {
        return "    \"" + org + ":" + name + ":" + version + "\"[label=<" + org + "<BR/><B>" + name + "</B><BR/>" + version + "> style=\"\"]\n";
    }

    private String edge(String fromOrg, String fromName, String fromVersion, String toOrg, String toName, String toVersion) {
        return "    \"" + fromOrg + ":" + fromName + ":" + fromVersion + "\" -> \"" + toOrg + ":" + toName + ":" + toVersion + "\"\n";
    }

    @Test
    public void projectFoundFromSingleNode() throws DetectableException {
        GraphParser graphParser = createGraphParser(node("one-org", "one-name", "one-version"));
        SbtProjectMatcher projectMatcher = new SbtProjectMatcher(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        String projectId = projectMatcher.determineProjectNodeID(graphParser, null);
        Assertions.assertEquals("\"one-org:one-name:one-version\"", projectId);
    }

    @Test
    public void projectFoundFromTwoNodesWhereProjectIsSecond() throws DetectableException {
        GraphParser graphParser = createGraphParser(node("two-org", "two-name", "two-version") +
                                                        node("one-org", "one-name", "one-version") +
                                                        edge("one-org", "one-name", "one-version", "two-org", "two-name", "two-version"));
        SbtProjectMatcher projectMatcher = new SbtProjectMatcher(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        String projectId = projectMatcher.determineProjectNodeID(graphParser, null);
        Assertions.assertEquals("\"one-org:one-name:one-version\"", projectId);
    }

    @Test
    public void testNonGraphCompileDot() throws DetectableException {
        String dependenciesCompileDotBody = "    \"com.hanhuy.sbt:kotlin-plugin:2.0.1-SNAPSHOT\"[label=<com.hanhuy.sbt<BR/><B>kotlin-plugin</B><BR/>2.0.1-SNAPSHOT> style=\"\"]\n"
                                                + "    \"io.argonaut:argonaut_2.12:6.2\"[label=<io.argonaut<BR/><B>argonaut_2.12</B><BR/>6.2> style=\"\"]\n"
                                                + "    \"org.scala-lang:scala-reflect:2.12.10\"[label=<org.scala-lang<BR/><B>scala-reflect</B><BR/>2.12.10> style=\"\"]\n"
                                                + "    \"org.scalaz:scalaz-core_2.12:7.2.28\"[label=<org.scalaz<BR/><B>scalaz-core_2.12</B><BR/>7.2.28> style=\"\"]\n"
                                                + "    \"io.argonaut:argonaut_2.12:6.2\" -> \"org.scala-lang:scala-reflect:2.12.10\"";
        GraphParser graphParser = createGraphParser(dependenciesCompileDotBody);
        SbtProjectMatcher projectMatcher = new SbtProjectMatcher(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        String projectId = projectMatcher.determineProjectNodeID(graphParser, "kotlin-plugin");
        Assertions.assertEquals("\"com.hanhuy.sbt:kotlin-plugin:2.0.1-SNAPSHOT\"", projectId);
    }
}
