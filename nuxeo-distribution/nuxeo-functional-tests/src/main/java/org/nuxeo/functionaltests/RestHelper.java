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
 *
 */

package org.nuxeo.functionaltests;

import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @since 8.2
 */
public class RestHelper {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final MediaType AUTOMATION_JSON = MediaType.parse("application/json+nxrequest");

    private static final String USER_WORKSPACE_PATH_FORMAT = "/default-domain/UserWorkspaces/%s";

    private static final OkHttpClient client = new OkHttpClient();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> documentIdsToDelete = new ArrayList<>();

    private static final List<String> documentPathsToDelete = new ArrayList<>();

    private static final List<String> usersToDelete = new ArrayList<>();

    private RestHelper() {
        // helper class
    }

    public static void cleanup() {
        cleanupDocuments();
        cleanupUsers();
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

    public static String createUser(String username, String password) {
        return createUser(username, password, null, null, null, null, null);
    }

    public static String createUser(String username, String password, String firstName, String lastName, String company,
            String email, String group) {
        String json = buildUserJSON(username, password, firstName, lastName, company, email, group);

        RequestBody body = RequestBody.create(JSON, json);
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/user" }, "/");
        Request request = newRequest().url(url).post(body).build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new RuntimeException(String.format("Unable to create user '%s'", username));
            }

            try (ResponseBody responseBody = response.body()) {
                JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
                String id = jsonNode.get("id").getTextValue();
                usersToDelete.add(id);
                return id;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Request.Builder newRequest() {
        String credential = Credentials.basic(ADMINISTRATOR, ADMINISTRATOR);
        return new Request.Builder().header("Authorization", credential);
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
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/user", username }, "/");
        Request request = newRequest().url(url).delete().build();
        try {
            client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createDocument(String idOrPath, String type, String title, String description) {
        String json = buildDocumentJSON(type, title, description);

        RequestBody body = RequestBody.create(JSON, json);
        String segment = idOrPath.startsWith("/") ? "path" : "id";
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1", segment, idOrPath }, "/");
        Request request = newRequest().url(url).post(body).build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new RuntimeException(String.format("Unable to create document '%s' in '%s'", title, idOrPath));
            }

            try (ResponseBody responseBody = response.body()) {
                JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
                String docId = jsonNode.get("uid").getTextValue();

                String path = jsonNode.get("path").getTextValue();
                // do we already have to delete one parent?
                boolean toDelete = true;
                for (String docPath : documentPathsToDelete) {
                    if (path.startsWith(docPath)) {
                        toDelete = false;
                        break;
                    }
                }
                if (toDelete) {
                    documentIdsToDelete.add(docId);
                    documentPathsToDelete.add(path);
                }

                return docId;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDocument(String id) {
        String segment = id.startsWith("/") ? "path" : "id";
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1", segment, id }, "/");
        Request request = newRequest().url(url).delete().build();
        try {
            client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String buildDocumentJSON(String type, String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"entity-type\": \"document\"").append(",\n");
        sb.append("\"type\": \"").append(type).append("\",\n");
        sb.append("\"name\": \"").append(title).append("\",\n");
        sb.append("\"properties\": {").append("\n");
        if (description != null) {
            sb.append("\"dc:description\": \"").append(description).append("\",\n");
        }
        sb.append("\"dc:title\": \"").append(title).append("\"\n");
        sb.append("}").append("\n");
        sb.append("}");
        return sb.toString();
    }

    public static void addPermission(String pathOrId, String username, String permission) {
        String json = buildAddPermissionJSON(pathOrId, username, permission);
        RequestBody body = RequestBody.create(AUTOMATION_JSON, json);
        String url = StringUtils.join(
                new String[] { AbstractTest.NUXEO_URL, "api/v1/automation", "Document.AddPermission" }, "/");
        Request request = newRequest().url(url).post(body).build();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new RuntimeException(String.format("Unable to add permission '%s' for user '%s' on '%s'",
                        permission, username, pathOrId));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildAddPermissionJSON(String pathOrId, String username, String permission) {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        sb.append("\"params\": {").append("\n");
        sb.append("\"username\": \"").append(username).append("\",\n");
        sb.append("\"permission\": \"").append(permission).append("\"\n");
        sb.append("}").append(",\n");
        sb.append("\"input\": \"").append(pathOrId).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

}
