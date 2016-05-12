/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.functionaltests;

import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.acl.ACE;
import org.nuxeo.client.api.objects.user.Group;
import org.nuxeo.client.api.objects.user.User;
import org.nuxeo.client.internals.spi.NuxeoClientException;

/**
 * @since 8.3
 */
public class RestHelper {

    private static final NuxeoClient CLIENT = new NuxeoClient(NUXEO_URL, ADMINISTRATOR, ADMINISTRATOR);

    private static final String USER_WORKSPACE_PATH_FORMAT = "/default-domain/UserWorkspaces/%s";

    private static final List<String> documentIdsToDelete = new ArrayList<>();

    private static final List<String> documentPathsToDelete = new ArrayList<>();

    private static final List<String> usersToDelete = new ArrayList<>();

    private static final List<String> groupsToDelete = new ArrayList<>();

    protected static final Log log = LogFactory.getLog(RestHelper.class);

    private RestHelper() {
        // helper class
    }

    public static void cleanup() {
        cleanupDocuments();
        cleanupUsers();
        cleanupGroups();
    }

    public static void cleanupDocuments() {
        documentIdsToDelete.forEach(RestHelper::deleteDocument);
        documentIdsToDelete.clear();
        documentPathsToDelete.clear();
    }

    public static void cleanupUsers() {
        for (String user : usersToDelete) {
            try{
                RestHelper.deleteDocument(String.format(USER_WORKSPACE_PATH_FORMAT, user));
            }catch(NuxeoClientException e){
                log.warn("User workspace not deleted for "+user+" (propably not found)");
            }
        }
        usersToDelete.forEach(RestHelper::deleteUser);
        usersToDelete.clear();
    }

    public static void cleanupGroups() {
        groupsToDelete.forEach(RestHelper::deleteGroup);
        groupsToDelete.clear();
    }

    public static String createUser(String username, String password) {
        return createUser(username, password, null, null, null, null, null);
    }

    public static String createUser(String username, String password, String firstName, String lastName,
            String company, String email, String group) {
        User user = new User();
        user.setUserName(username);
        //TODO add something to replace user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCompany(company);
        user.setEmail(email);
        user.setGroups(Collections.singletonList(group));

        String id = CLIENT.getUserManager().createUser(user).getId();
        usersToDelete.add(id);
        return id;
    }

    public static void deleteUser(String username) {
        CLIENT.getUserManager().deleteUser(username);
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
        CLIENT.getUserManager().deleteGroup(name);
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
            CLIENT.repository().deleteDocument(CLIENT.repository().fetchDocumentByPath(idOrPath));
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

        //@yannis : temporary fix for setting permission before JAVACLIENT-90 is done
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

}
