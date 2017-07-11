/*
 * (C) Copyright 2016-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 *     Yannis JULIENNE
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.Documents;
import org.nuxeo.client.api.objects.acl.ACE;
import org.nuxeo.client.api.objects.user.Group;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.common.utils.URIUtils;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @since 8.3
 */
public class RestHelper {

    private static final NuxeoClient CLIENT = new NuxeoClient(NUXEO_URL, ADMINISTRATOR, ADMINISTRATOR);

    private static final String USER_WORKSPACE_PATH_FORMAT = "/default-domain/UserWorkspaces/%s";

    private static final String DEFAULT_USER_EMAIL = "devnull@nuxeo.com";

    private static final String DOCUMENT_QUERY_BY_PATH_BASE = "SELECT * FROM Document WHERE ecm:path = '%s'";

    private static final List<String> documentIdsToDelete = new ArrayList<>();

    private static final List<String> documentPathsToDelete = new ArrayList<>();

    private static final List<String> usersToDelete = new ArrayList<>();

    private static final List<String> groupsToDelete = new ArrayList<>();

    protected static final Map<String, Set<String>> directoryEntryIdsToDelete = new HashMap<>();

    private static final int NOT_FOUND_ERROR_STATUS = 404;

    protected static final Log log = LogFactory.getLog(RestHelper.class);

    // @yannis : temporary fix for setting user password before JAVACLIENT-91
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestHelper() {
        // helper class
    }

    public static void cleanup() {
        cleanupDocuments();
        cleanupUsers();
        cleanupGroups();
        cleanupDirectoryEntries();
    }

    public static void cleanupDocuments() {
        documentIdsToDelete.forEach(RestHelper::deleteDocument);
        documentIdsToDelete.clear();
        documentPathsToDelete.clear();
    }

    public static void cleanupUsers() {
        for (String user : usersToDelete) {
            RestHelper.deleteDocument(String.format(USER_WORKSPACE_PATH_FORMAT, user));
        }
        usersToDelete.forEach(RestHelper::deleteUser);
        usersToDelete.clear();
    }

    public static void cleanupGroups() {
        groupsToDelete.forEach(RestHelper::deleteGroup);
        groupsToDelete.clear();
    }

    public static void cleanupDirectoryEntries() {
        directoryEntryIdsToDelete.forEach(
                (directoryName, entryIds) -> entryIds.forEach(id -> deleteDirectoryEntry(directoryName, id)));
        clearDirectoryEntryIdsToDelete();
    }

    public static String createUser(String username, String password) {
        return createUser(username, password, null, null, null, null, null);
    }

    public static String createUser(String username, String password, String firstName, String lastName, String company,
            String email, String group) {

        String finalEmail = StringUtils.isBlank(email) ? DEFAULT_USER_EMAIL : email;

        // @yannis : temporary fix for setting user password before JAVACLIENT-91
        String json = buildUserJSON(username, password, firstName, lastName, company, finalEmail, group);

        Response response = CLIENT.post(AbstractTest.NUXEO_URL + "/api/v1/user", json);
        if (!response.isSuccessful()) {
            throw new RuntimeException(String.format("Unable to create user '%s'", username));
        }

        try (ResponseBody responseBody = response.body()) {
            JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
            String id = jsonNode.get("id").getTextValue();
            usersToDelete.add(id);
            return id;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addUserToDelete(String userName) {
        usersToDelete.add(userName);
    }

    public static void removeUserToDelete(String userName) {
        usersToDelete.remove(userName);
    }

    public static void addDirectoryEntryToDelete(String directoryName, String entryId) {
        directoryEntryIdsToDelete.computeIfAbsent(directoryName, k -> new HashSet<>()).add(entryId);
    }

    public static void clearDirectoryEntryIdsToDelete() {
        directoryEntryIdsToDelete.clear();
    }

    private static String buildUserJSON(String username, String password, String firstName, String lastName,
            String company, String email, String group) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"entity-type\": \"user\"").append(",\n");
        sb.append("\"id\": \"").append(username).append("\",\n");
        sb.append("\"properties\": {").append("\n");
        if (firstName != null) {
            sb.append("\"firstName\": \"").append(firstName).append("\",\n");
        }
        if (lastName != null) {
            sb.append("\"lastName\": \"").append(lastName).append("\",\n");
        }
        if (email != null) {
            sb.append("\"email\": \"").append(email).append("\",\n");
        }
        if (company != null) {
            sb.append("\"company\": \"").append(company).append("\",\n");
        }
        if (group != null) {
            sb.append("\"groups\": [\"").append(group).append("\"]").append(",\n");
        }
        sb.append("\"username\": \"").append(username).append("\",\n");
        sb.append("\"password\": \"").append(password).append("\"\n");
        sb.append("}").append("\n");
        sb.append("}");
        return sb.toString();
    }

    public static void deleteUser(String username) {
        try {
            CLIENT.getUserManager().deleteUser(username);
        } catch (NuxeoClientException e) {
            if (NOT_FOUND_ERROR_STATUS == e.getStatus()) {
                log.warn(String.format("User %s not deleted because not found", username));
            } else {
                throw e;
            }
        }
    }

    public static void createGroup(String name, String label) {
        createGroup(name, label, null, null);
    }

    public static void createGroup(String name, String label, String[] members, String[] subGroups) {
        Group group = new Group();
        group.setGroupName(name);
        group.setGroupLabel(label);
        if (members != null) {
            group.setMemberUsers(Arrays.asList(members));
        }
        if (subGroups != null) {
            group.setMemberGroups(Arrays.asList(subGroups));
        }

        CLIENT.getUserManager().createGroup(group);
        groupsToDelete.add(name);
    }

    public static void deleteGroup(String name) {
        try {
            CLIENT.getUserManager().deleteGroup(name);
        } catch (NuxeoClientException e) {
            if (NOT_FOUND_ERROR_STATUS == e.getStatus()) {
                log.warn(String.format("Group %s not deleted because not found", name));
            } else {
                throw e;
            }
        }
    }

    public static String createDocument(String idOrPath, String type, String title, String description) {
        Document document = new Document(title, type);
        Map<String, Object> properties = new HashMap<>();
        properties.put("dc:title", title);
        if (description != null) {
            properties.put("dc:description", description);
        }
        document.setProperties(properties);

        if (idOrPath.startsWith("/")) {
            document = CLIENT.repository().createDocumentByPath(idOrPath, document);
        } else {
            document = CLIENT.repository().createDocumentById(idOrPath, document);
        }

        String docId = document.getId();
        String docPath = document.getPath();
        // do we already have to delete one parent?
        if (documentPathsToDelete.stream().noneMatch(docPath::startsWith)) {
            documentIdsToDelete.add(docId);
            documentPathsToDelete.add(docPath);
        }
        return docId;
    }

    public static void deleteDocument(String idOrPath) {
        // TODO change that by proper deleteDocument(String)
        if (idOrPath.startsWith("/")) {
            // @yannis : temporary way to avoid DocumentNotFoundException in server log before NXP-19658
            Documents documents = CLIENT.repository().query(String.format(DOCUMENT_QUERY_BY_PATH_BASE, idOrPath));
            if (documents.size() > 0) {
                CLIENT.repository().deleteDocument(documents.getDocument(0));
            }
        } else {
            CLIENT.repository().deleteDocument(CLIENT.repository().fetchDocumentById(idOrPath));
        }
    }

    public static void addPermission(String idOrPath, String username, String permission) {
        Document document;
        if (idOrPath.startsWith("/")) {
            document = CLIENT.repository().fetchDocumentByPath(idOrPath);
        } else {
            document = CLIENT.repository().fetchDocumentById(idOrPath);
        }

        ACE ace = new ACE();
        ace.setUsername(username);
        ace.setPermission(permission);

        // @yannis : temporary fix for setting permission before JAVACLIENT-90 is done
        Calendar beginDate = Calendar.getInstance();
        ace.setBegin(beginDate);
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.YEAR, 1);
        ace.setEnd(endDate);

        document.addPermission(ace);
    }

    public static void removePermissions(String idOrPath, String username) {
        Document document;
        if (idOrPath.startsWith("/")) {
            document = CLIENT.repository().fetchDocumentByPath(idOrPath);
        } else {
            document = CLIENT.repository().fetchDocumentById(idOrPath);
        }

        document.removePermission(username);
    }

    /**
     * @since 9.2
     */
    public static String createDirectoryEntry(String directoryName, Map<String, String> properties) {
        Response response = CLIENT.post(NUXEO_URL + "/api/v1/directory/" + directoryName,
                buildDirectoryEntryJSON(directoryName, properties));
        if (!response.isSuccessful()) {
            throw new NuxeoClientException(
                    String.format("Unable to create entry for directory %s: %s", directoryName, properties));
        }
        try (ResponseBody responseBody = response.body()) {
            JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
            String entryId = jsonNode.get("properties").get("id").getValueAsText();
            addDirectoryEntryToDelete(directoryName, entryId);
            return entryId;
        } catch (IOException e) {
            throw new NuxeoClientException(e);
        }
    }

    /**
     * @since 9.2
     */
    public static void deleteDirectoryEntry(String directoryName, String entryId) {
        // Work around JAVACLIENT-133 by passing an empty string
        CLIENT.delete(NUXEO_URL + "/api/v1/directory/" + directoryName + "/" + entryId, "");
    }

    protected static String buildDirectoryEntryJSON(String directoryName, Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"entity-type\": \"directoryEntry\"").append(",\n");
        sb.append("\"directoryName\": \"").append(directoryName).append("\",\n");
        sb.append("\"properties\": {").append("\n");
        sb.append(properties.keySet()
                            .stream()
                            .map(key -> new StringBuilder().append("\"")
                                                           .append(key)
                                                           .append("\": \"")
                                                           .append(properties.get(key))
                                                           .append("\""))
                            .collect(Collectors.joining(",\n")));
        sb.append("\n").append("}").append("\n");
        sb.append("}");
        return sb.toString();
    }

    public static void logOnServer(String level, String message) {
        CLIENT.get(String.format("%s/restAPI/systemLog?token=dolog&level=%s&message=%s", AbstractTest.NUXEO_URL, level,
                URIUtils.quoteURIPathComponent(message, true)));
    }

}
