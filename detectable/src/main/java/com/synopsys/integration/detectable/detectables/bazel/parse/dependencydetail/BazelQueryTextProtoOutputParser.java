package com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.synopsys.integration.exception.IntegrationException;

public class BazelQueryTextProtoOutputParser {

    public List<String> parseStringValuesFromTextProto(final String pathToAttributeObjectList, final String gavObjectName, final String gavFieldName, final String textProtoString) throws IntegrationException {

        final List<String> linesList = splitStringIntoLines(textProtoString);
        final String gavString = parseGavString(pathToAttributeObjectList, gavObjectName, gavFieldName, linesList);
        return Arrays.asList(gavString);
    }

    @NotNull
    private List<String> splitStringIntoLines(final String textProtoString) {
        final String lines[] = textProtoString.split("\\r?\\n");
        return Arrays.asList(lines);
    }

    private String parseGavString(final String pathToAttributeObjectList, final String gavObjectName, final String gavFieldName, final List<String> textProtoStringList) throws IntegrationException {
        final List<Map<String, String>> resultsTargetAttributeObjects = parseResultsTargetAttributeObjects(pathToAttributeObjectList, textProtoStringList);
        final Map<String, String> artifactObject = findArtifactObject(gavObjectName, resultsTargetAttributeObjects);
        return getValueFromArtifactObject(gavFieldName, artifactObject);
    }

    private String getValueFromArtifactObject(final String gavFieldName, final Map<String, String> artifactObject) throws IntegrationException {
        final String value = artifactObject.get(gavFieldName);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        throw new IntegrationException(String.format("Value not found in artifact object: %s", artifactObject));
    }

    private Map<String, String> findArtifactObject(final String gavObjectName, final List<Map<String, String>> resultsTargetAttributeObjects) throws IntegrationException {
        for (int i=0; i < resultsTargetAttributeObjects.size(); i++) {
            final Map<String, String> currentAttributeObject = resultsTargetAttributeObjects.get(i);
            System.out.println(String.format("Read resultsTargetAttributeObject: %s", currentAttributeObject));
            final String name = currentAttributeObject.get("name");
            if (gavObjectName.equals(name)) {
                return currentAttributeObject;
            }
        }
        throw new IntegrationException("Artifact Object not found");
    }

    private List<Map<String, String>> parseResultsTargetAttributeObjects(final String pathToAttributeObjectList, final List<String> textProtoStringList) {
        final List<Map<String, String>> resultsTargetAttributeObjects = new ArrayList<>();
        boolean inAttribute = false;
        // :grandparent:parent:currentobject
        String currentObjectLineage = "";
        String currentObjectName = null;
        for (final String line : textProtoStringList) {
            System.out.println(line);
            if (!isObjectStart(line) && !isObjectEnd(line) && inAttribute) {
                final String fieldName = getFieldName(line);
                final String fieldValue = getFieldValue(line);
                final Map<String, String> currentAttributeObject = resultsTargetAttributeObjects.get(resultsTargetAttributeObjects.size()-1);
                currentAttributeObject.put(fieldName, fieldValue);
            }
            if (isObjectStart(line)) {
                currentObjectName = getObjectName(line);
                currentObjectLineage = String.format("%s:%s", currentObjectLineage, currentObjectName);
                if (pathToAttributeObjectList.equals(currentObjectLineage)) {
                    inAttribute = true;
                    resultsTargetAttributeObjects.add(new HashMap<>());
                }
            } else if (isObjectEnd(line)) {
                if (inAttribute) {
                    inAttribute = false;
                    currentObjectLineage = popObjectLineage(currentObjectLineage);
                }
            }
        }
        return resultsTargetAttributeObjects;
    }

    String popObjectLineage(final String oldObjectLineage) {
        final int lastColonIndex = oldObjectLineage.lastIndexOf(':');
        final String newObjectLineage = oldObjectLineage.substring(0, lastColonIndex);
        return newObjectLineage;
    }
    String getFieldValue(final String line) {
        final String trimmedLine = line.trim();
        int colonIndex = trimmedLine.indexOf(':');
        if (colonIndex < 0) {
            return "";
        }
        final String fieldValueQuoted = trimmedLine.substring(colonIndex+1).trim();
        final String fieldValue;
        if (fieldValueQuoted.startsWith("\"") || fieldValueQuoted.startsWith("'")) {
            fieldValue = fieldValueQuoted.substring(1, fieldValueQuoted.length()-1);
        } else {
            // it's actually not quoted
            fieldValue = fieldValueQuoted;
        }
        return fieldValue;
    }

    String getFieldName(final String line) {
        final String trimmedLine = line.trim();
        int colonIndex = trimmedLine.indexOf(':');
        if (colonIndex < 0) {
            return "";
        }
        final String fieldName = trimmedLine.substring(0, colonIndex);
        return fieldName;
    }

    boolean isObjectEnd(final String line) {
        return line.endsWith("}");
    }

    boolean isObjectStart(final String line) {
        return line.endsWith("{");
    }

    String getObjectName(final String line) {
        return line.substring(0, line.length()-1).trim();
    }
}
