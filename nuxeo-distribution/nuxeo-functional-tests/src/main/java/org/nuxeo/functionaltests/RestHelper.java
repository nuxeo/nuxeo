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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.Documents;
import org.nuxeo.client.api.objects.acl.ACE;
import org.nuxeo.client.api.objects.operation.DocRef;
import org.nuxeo.client.api.objects.user.Group;
import org.nuxeo.client.api.objects.workflow.Workflow;
import org.nuxeo.client.api.objects.workflow.Workflows;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.common.utils.URIUtils;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @since 8.3
 */
public class RestHelper {

    private static final NuxeoClient CLIENT = new NuxeoClient(NUXEO_URL, ADMINISTRATOR, ADMINISTRATOR);
    static {
        // by default timeout is 10s, hot reload needs a bit more
        CLIENT.timeout(120);
    }

    private static final String USER_WORKSPACE_PATH_FORMAT = "/default-domain/UserWorkspaces/%s";

    private static final String DEFAULT_USER_EMAIL = "devnull@nuxeo.com";

    private static final String DOCUMENT_QUERY_BY_PATH_BASE = "SELECT * FROM Document WHERE ecm:path = '%s'";

    private static final List<String> documentIdsToDelete = new ArrayList<>();

    private static final List<String> documentPathsToDelete = new ArrayList<>();

    private static final List<String> usersToDelete = new ArrayList<>();

    private static final List<String> groupsToDelete = new ArrayList<>();

    protected static final Map<String, Set<String>> directoryEntryIdsToDelete = new HashMap<>();

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

    /**
     * @since 9.3
     */
    public static void addDocumentToDelete(String idOrPath) {
        Document document = fetchDocumentByIdOrPath(idOrPath);
        addDocumentToDelete(document.getId(), document.getPath());
    }

    /**
     * @since 9.3
     */
    public static void addDocumentToDelete(String id, String path) {
        // do we already have to delete one parent?
        if (documentPathsToDelete.stream().noneMatch(path::startsWith)) {
            documentIdsToDelete.add(id);
            documentPathsToDelete.add(path);
        }
    }

    public static void addUserToDelete(String userName) {
        usersToDelete.add(userName);
    }

    public static void removeUserToDelete(String userName) {
        usersToDelete.remove(userName);
    }

    public static void addGroupToDelete(String groupName) {
        groupsToDelete.add(groupName);
    }

    public static void addDirectoryEntryToDelete(String directoryName, String entryId) {
        directoryEntryIdsToDelete.computeIfAbsent(directoryName, k -> new HashSet<>()).add(entryId);
    }

    public static void clearDirectoryEntryIdsToDelete() {
        directoryEntryIdsToDelete.clear();
    }

    // ---------------------
    // User & Group Services
    // ---------------------

    public static String createUser(String username, String password) {
        return createUser(username, password, null, null, null, null, null);
    }

    public static String createUser(String username, String password, String firstName, String lastName, String company,
            String email, String group) {

        String finalEmail = StringUtils.isBlank(email) ? DEFAULT_USER_EMAIL : email;

        // @yannis : temporary fix for setting user password before JAVACLIENT-91
        String json = buildUserJSON(username, password, firstName, lastName, company, finalEmail, group);

        Response response = CLIENT.post(AbstractTest.NUXEO_URL + "/api/v1/user", json);
        try (ResponseBody responseBody = response.body()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(String.format("Unable to create user '%s'", username));
            }

            JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
            String id = jsonNode.get("id").getTextValue();
            usersToDelete.add(id);
            return id;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            if (HttpStatus.SC_NOT_FOUND == e.getStatus()) {
                log.warn(String.format("User %s not deleted because not found", username));
            } else {
                throw e;
            }
        }
    }

    /**
     * @since 9.3
     */
    public static boolean userExists(String username) {
        return exists(() -> CLIENT.getUserManager().fetchUser(username));
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
            if (HttpStatus.SC_NOT_FOUND == e.getStatus()) {
                log.warn(String.format("Group %s not deleted because not found", name));
            } else {
                throw e;
            }
        }
    }

    /**
     * @since 9.3
     */
    public static boolean groupExists(String groupName) {
        return exists(() -> CLIENT.getUserManager().fetchGroup(groupName));
    }

    // -----------------
    // Document Services
    // -----------------

    /**
     * @since 9.3
     */
    public static String createDocument(String idOrPath, String type, String title) {
        return createDocument(idOrPath, type, title, Collections.emptyMap());
    }

    public static String createDocument(String idOrPath, String type, String title, String description) {
        Map<String, Object> props;
        if (description == null) {
            props = Collections.emptyMap();
        } else {
            props = Collections.singletonMap("dc:description", description);
        }
        return createDocument(idOrPath, type, title, props);
    }

    /**
     * @since 9.3
     */
    public static String createDocument(String idOrPath, String type, String title, Map<String, Object> props) {
        Document document = new Document(title, type);
        Map<String, Object> properties = new HashMap<>();
        if (props != null) {
            properties.putAll(props);
        }
        properties.put("dc:title", title);
        document.setProperties(properties);

        if (idOrPath.startsWith("/")) {
            document = CLIENT.repository().createDocumentByPath(idOrPath, document);
        } else {
            document = CLIENT.repository().createDocumentById(idOrPath, document);
        }

        String docId = document.getId();
        String docPath = document.getPath();
        addDocumentToDelete(docId, docPath);
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
        ACE ace = new ACE();
        ace.setUsername(username);
        ace.setPermission(permission);

        // @yannis : temporary fix for setting permission before JAVACLIENT-90 is done
        Calendar beginDate = Calendar.getInstance();
        ace.setBegin(beginDate);
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.YEAR, 1);
        ace.setEnd(endDate);

        fetchDocumentByIdOrPath(idOrPath).addPermission(ace);
    }

    public static void removePermissions(String idOrPath, String username) {
        fetchDocumentByIdOrPath(idOrPath).removePermission(username);
    }

    /**
     * @since 9.3
     */
    public static void followLifecycleTransition(String idOrPath, String transitionName) {
        CLIENT.automation("Document.FollowLifecycleTransition")
              .input(new DocRef(idOrPath))
              .param("value", transitionName)
              .execute();
    }

    /**
     * @since 9.3
     */
    public static boolean documentExists(String idOrPath) {
        return exists(() -> fetchDocumentByIdOrPath(idOrPath));
    }

    /**
     * @since 9.3
     */
    public static void startWorkflowInstance(String idOrPath, String workflowId) {
        Workflow workflow = CLIENT.repository().fetchWorkflowModel(workflowId);
        if (idOrPath.startsWith("/")) {
            CLIENT.repository().startWorkflowInstanceWithDocPath(idOrPath, workflow);
        } else {
            CLIENT.repository().startWorkflowInstanceWithDocId(idOrPath, workflow);
        }
    }

    /**
     * @since 9.3
     */
    public static boolean documentHasWorkflowStarted(String idOrPath) {
        Workflows workflows;
        if (idOrPath.startsWith("/")) {
            workflows = CLIENT.repository().fetchWorkflowInstancesByDocPath(idOrPath);
        } else {
            // TODO replace this call by CLIENT.repository().fetchWorkflowInstancesByDocId(idOrPath) once RepositoryAPI
            // interface will be fixed (fetchWorkflowInstancesByDocId doesn't exist in the interface)
            workflows = fetchDocumentByIdOrPath(idOrPath).fetchWorkflowInstances();
        }
        return workflows.size() > 0;
    }

    /**
     * Fetches a {@link Document} instance according the input parameter which can be a document id or path.
     * <p />
     * CAUTION: Keep this method protected, we want to keep nuxeo-java-client objects here.
     *
     * @since 9.3
     */
    protected static Document fetchDocumentByIdOrPath(String idOrPath) {
        if (idOrPath.startsWith("/")) {
            return CLIENT.repository().fetchDocumentByPath(idOrPath);
        } else {
            return CLIENT.repository().fetchDocumentById(idOrPath);
        }
    }

    // ------------------
    // Directory Services
    // ------------------

    /**
     * @since 9.2
     */
    public static String createDirectoryEntry(String directoryName, Map<String, String> properties) {
        Response response = CLIENT.post(NUXEO_URL + "/api/v1/directory/" + directoryName,
                buildDirectoryEntryJSON(directoryName, properties));
        try (ResponseBody responseBody = response.body()) {
            if (!response.isSuccessful()) {
                throw new NuxeoClientException(
                        String.format("Unable to create entry for directory %s: %s", directoryName, properties));
            }
            JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
            String entryId = jsonNode.get("properties").get("id").getValueAsText();
            addDirectoryEntryToDelete(directoryName, entryId);
            return entryId;
        } catch (IOException e) {
            throw new NuxeoClientException(e);
        }
    }

    public static Map<String, Object> fetchDirectoryEntry(String directoryName, String entryId) {
        Response response = CLIENT.get(NUXEO_URL + "/api/v1/directory/" + directoryName + "/" + entryId);
        try (ResponseBody responseBody = response.body(); Reader reader = responseBody.charStream()) {
            if (!response.isSuccessful()) {
                throw new NuxeoClientException(
                        String.format("Unable to fetch entry for directory %s/%s", directoryName, entryId));
            }
            return MAPPER.readValue(reader, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (IOException e) {
            throw new NuxeoClientException(e);
        }
    }

    /**
     * @since 9.2
     */
    public static void deleteDirectoryEntry(String directoryName, String entryId) {
        // Work around JAVACLIENT-133 by passing an empty string
        executeHTTP(() -> CLIENT.delete(NUXEO_URL + "/api/v1/directory/" + directoryName + "/" + entryId, ""));
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

    // ------------------
    // Operation Services
    // ------------------

    /**
     * @since 9.3
     */
    public static void operation(String operationId, Map<String, Object> parameters) {
        CLIENT.automation(operationId).parameters(parameters).execute();
    }

    /**
     * @since 9.3
     */
    public static void logOnServer(String message) {
        logOnServer("WARN", message);
    }

    public static void logOnServer(String level, String message) {
        executeHTTP(
                () -> CLIENT.get(String.format("%s/restAPI/systemLog?token=dolog&level=%s&message=----- RestHelper: %s",
                        AbstractTest.NUXEO_URL, level, URIUtils.quoteURIPathComponent(message, true))));
    }

    // -------------
    // HTTP Services
    // -------------

    /**
     * Performs a GET request and return whether or not request was successful.
     *
     * @since 9.3
     */
    public static boolean get(String path) {
        return executeHTTP(() -> CLIENT.get(NUXEO_URL + path));
    }

    /**
     * Performs a POST request and return whether or not request was successful.
     *
     * @since 9.3
     */
    public static boolean post(String path, String body) {
        return executeHTTP(() -> CLIENT.post(NUXEO_URL + path, body));
    }

    /**
     * @since 9.3
     */
    protected static boolean executeHTTP(Supplier<Response> fetcher) {
        Response response = fetcher.get();
        response.body().close();
        return response.isSuccessful();
    }

    /**
     * @since 9.3
     */
    protected static <T> boolean exists(Supplier<T> fetcher) {
        try {
            return fetcher.get() != null;
        } catch (NuxeoClientException nce) {
            if (nce.getStatus() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            throw nce;
        }
    }

}
