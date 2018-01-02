/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.objects.Documents;
import org.nuxeo.client.objects.acl.ACE;
import org.nuxeo.client.objects.directory.DirectoryEntry;
import org.nuxeo.client.objects.operation.DocRef;
import org.nuxeo.client.objects.user.Group;
import org.nuxeo.client.objects.user.User;
import org.nuxeo.client.objects.workflow.Workflow;
import org.nuxeo.client.objects.workflow.Workflows;
import org.nuxeo.client.spi.NuxeoClientRemoteException;

import okhttp3.Response;

/**
 * @since 8.3
 */
public class RestHelper {

    private static final NuxeoClient CLIENT = new NuxeoClient.Builder().url(NUXEO_URL)
                                                                       .authentication(ADMINISTRATOR, ADMINISTRATOR)
                                                                       // by default timeout is 10s, hot reload needs a
                                                                       // bit more
                                                                       .timeout(120)
                                                                       .connect();

    private static final String USER_WORKSPACE_PATH_FORMAT = "/default-domain/UserWorkspaces/%s";

    private static final String DEFAULT_USER_EMAIL = "devnull@nuxeo.com";

    private static final String DOCUMENT_QUERY_BY_PATH_BASE = "SELECT * FROM Document WHERE ecm:path = '%s'";

    /**
     * Documents to delete in cleanup step. Key is the document id and value is its path.
     *
     * @since 9.3
     */
    private static final Map<String, String> documentsToDelete = new HashMap<>();

    private static final List<String> usersToDelete = new ArrayList<>();

    private static final List<String> groupsToDelete = new ArrayList<>();

    protected static final Map<String, Set<String>> directoryEntryIdsToDelete = new HashMap<>();

    protected static final Log log = LogFactory.getLog(RestHelper.class);

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
        // delete by ids
        documentsToDelete.keySet().forEach(RestHelper::deleteDocument);
        documentsToDelete.clear();
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
        if (documentsToDelete.values().stream().noneMatch(path::startsWith)) {
            documentsToDelete.put(id, path);
        }
    }

    /**
     * @since 9.3
     */
    public static void removeDocumentToDelete(String idOrPath) {
        if (idOrPath.startsWith("/")) {
            documentsToDelete.values().remove(idOrPath);
        } else {
            documentsToDelete.remove(idOrPath);
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

    /**
     * @since 9.10
     */
    public static void removeDirectoryEntryToDelete(String directoryName, String entryId) {
        directoryEntryIdsToDelete.getOrDefault(directoryName, Collections.emptySet()).remove(entryId);
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

        User user = new User();
        user.setUserName(username);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCompany(company);
        user.setEmail(finalEmail);
        if (StringUtils.isNotBlank(group)) {
            user.setGroups(Collections.singletonList(group));
        }

        user = CLIENT.userManager().createUser(user);

        String userId = user.getId();
        usersToDelete.add(userId);
        return userId;
    }

    public static void deleteUser(String username) {
        try {
            CLIENT.userManager().deleteUser(username);
        } catch (NuxeoClientRemoteException e) {
            if (e.getStatus() == HttpStatus.SC_NOT_FOUND) {
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
        return exists(() -> CLIENT.userManager().fetchUser(username));
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

        CLIENT.userManager().createGroup(group);
        groupsToDelete.add(name);
    }

    public static void deleteGroup(String name) {
        try {
            CLIENT.userManager().deleteGroup(name);
        } catch (NuxeoClientRemoteException e) {
            if (e.getStatus() == HttpStatus.SC_NOT_FOUND) {
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
        return exists(() -> CLIENT.userManager().fetchGroup(groupName));
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
        Document document = Document.createWithName(title, type);
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
        if (idOrPath.startsWith("/")) {
            // @yannis : temporary way to avoid DocumentNotFoundException in server log before NXP-19658
            Documents documents = CLIENT.repository().query(String.format(DOCUMENT_QUERY_BY_PATH_BASE, idOrPath));
            if (documents.size() > 0) {
                CLIENT.repository().deleteDocument(documents.getDocument(0));
            }
        } else {
            CLIENT.repository().deleteDocument(idOrPath);
        }
    }

    public static void addPermission(String idOrPath, String username, String permission) {
        ACE ace = new ACE();
        ace.setUsername(username);
        ace.setPermission(permission);

        fetchDocumentByIdOrPath(idOrPath).addPermission(ace);
    }

    public static void removePermissions(String idOrPath, String username) {
        fetchDocumentByIdOrPath(idOrPath).removePermission(username);
    }

    /**
     * @since 9.3
     */
    public static void followLifecycleTransition(String idOrPath, String transitionName) {
        CLIENT.operation("Document.FollowLifecycleTransition")
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
            workflows = CLIENT.repository().fetchWorkflowInstancesByDocId(idOrPath);
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

    /**
     * Runs a page provider on Nuxeo instance and return the total size of documents.
     *
     * @return the total size of documents
     * @since 9.3
     */
    public static int countQueryPageProvider(String providerName) {
        Documents result = CLIENT.repository().queryByProvider(providerName, "1", "0", "-1", "dc:title", "ASC", null);
        return result.getTotalSize();
    }

    // ------------------
    // Directory Services
    // ------------------

    /**
     * @since 9.2
     */
    public static String createDirectoryEntry(String directoryName, Map<String, String> properties) {
        DirectoryEntry entry = new DirectoryEntry();
        entry.setProperties(properties);
        entry = CLIENT.directoryManager().directory(directoryName).createEntry(entry);
        String entryId = entry.getId();
        addDirectoryEntryToDelete(directoryName, entryId);
        return entryId;
    }

    /**
     * @since 9.3
     */
    public static Map<String, Object> fetchDirectoryEntryProperties(String directoryName, String entryId) {
        return CLIENT.directoryManager().directory(directoryName).fetchEntry(entryId).getProperties();
    }

    /**
     * @since 9.10
     */
    public static void updateDirectoryEntry(String directoryName, String entryId, Map<String, String> properties) {
        DirectoryEntry entry = new DirectoryEntry();
        entry.setProperties(properties);
        entry.putIdProperty(entryId);
        CLIENT.directoryManager().directory(directoryName).updateEntry(entry);
    }

    /**
     * @since 9.2
     */
    public static void deleteDirectoryEntry(String directoryName, String entryId) {
        CLIENT.directoryManager().directory(directoryName).deleteEntry(entryId);
    }

    /**
     * @since 9.10
     */
    public static void deleteDirectoryEntries(String directoryName) {
        CLIENT.directoryManager().directory(directoryName).fetchEntries().getDirectoryEntries().forEach(entry -> {
            entry.delete();
            removeDirectoryEntryToDelete(directoryName, entry.getId());
        });
    }

    // ------------------
    // Operation Services
    // ------------------

    /**
     * @since 9.3
     */
    public static void operation(String operationId, Map<String, Object> parameters) {
        CLIENT.operation(operationId).parameters(parameters).execute();
    }

    /**
     * Logs on server with <code>RestHelper</code> as source and <code>warn</code> as level.
     *
     * @since 9.3
     */
    public static void logOnServer(String message) {
        logOnServer("warn", message);
    }

    /**
     * Logs on server with <code>RestHelper</code> as source.
     */
    public static void logOnServer(String level, String message) {
        logOnServer("RestHelper", level, message);
    }

    /**
     * @param source the logger source, usually RestHelper or WebDriver
     * @param level the log level
     * @since 9.3
     */
    public static void logOnServer(String source, String level, String message) {
        CLIENT.operation("Log")
              // For compatibility
              .param("category", RestHelper.class.getName())
              .param("level", level)
              .param("message", String.format("----- %s: %s", source, message))
              .execute();
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
        } catch (NuxeoClientRemoteException nce) {
            if (nce.getStatus() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            throw nce;
        }
    }

}
