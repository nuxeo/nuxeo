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

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @since 8.2
 */
public class RestHelper {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final MediaType AUTOMATION_JSON = MediaType.parse("application/json+nxrequest");

    private static final OkHttpClient client = new OkHttpClient();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestHelper() {
        // helper class
    }

    public static String createUser(String username, String password) throws IOException {
        return createUser(username, password, null, null, null, null, null);
    }

    public static String createUser(String username, String password, String firstName, String lastName,
            String company, String email, String group) throws IOException {
        String json = buildUserJSON(username, password, firstName, lastName, company, email, group);

        RequestBody body = RequestBody.create(JSON, json);
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/user" }, "/");
        Request request = newRequest().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException(String.format("Unable to create user '%s'", username));
        }

        try (ResponseBody responseBody = response.body()) {
            JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
            return jsonNode.get("id").getTextValue();
        }
    }

    private static Request.Builder newRequest() {
        String credential = Credentials.basic("Administrator", "Administrator");
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

    public static void deleteUser(String username) throws IOException {
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/user", username }, "/");
        Request request = newRequest().url(url).delete().build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException(String.format("Unable to delete user '%s'", username));
        }
    }

    public static String createDocument(String parentPath, String type, String title, String description)
            throws IOException {
        String json = buildDocumentJSON(type, title, description);

        RequestBody body = RequestBody.create(JSON, json);
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/path", parentPath }, "/");
        Request request = newRequest().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException(String.format("Unable to create document '%s' in '%s'", title, parentPath));
        }

        try (ResponseBody responseBody = response.body()) {
            JsonNode jsonNode = MAPPER.readTree(responseBody.charStream());
            return jsonNode.get("uid").getTextValue();
        }
    }

    public static void deleteDocument(String id) throws IOException {
        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/id", id }, "/");
        Request request = newRequest().url(url).delete().build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException(String.format("Unable to delete document '%s'", id));
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

    public static void addPermission(String path, String username, String permission) throws IOException {
        String json = buildAddPermissionJSON(path, username, permission);

        RequestBody body = RequestBody.create(AUTOMATION_JSON, json);

        String url = StringUtils.join(new String[] { AbstractTest.NUXEO_URL, "api/v1/automation",
                "Document.AddPermission" }, "/");
        Request request = newRequest().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException(String.format("Unable to add permission '%s' for user '%s' on '%s'", permission,
                    username, path));
        }
    }

    private static String buildAddPermissionJSON(String path, String username, String permission) {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        sb.append("\"params\": {").append("\n");
        sb.append("\"username\": \"").append(username).append("\",\n");
        sb.append("\"permission\": \"").append(permission).append("\"\n");
        sb.append("}").append(",\n");
        sb.append("\"input\": \"").append(path).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

}
