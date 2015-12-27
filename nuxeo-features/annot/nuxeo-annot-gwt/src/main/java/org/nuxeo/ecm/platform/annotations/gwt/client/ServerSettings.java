/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.gwt.client;

import com.google.gwt.i18n.client.Dictionary;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ServerSettings {

    private static final String SERVER_SETTINGS = "serverSettings";

    private static final String REPOSITORY_NAME = "repositoryName";

    private static final String DOCUMENT_ID = "documentId";

    private static final String CONTEXT_PATH = "contextPath";

    private static ServerSettings current = loadServerSettings();

    private String repositoryName;

    private String documentId;

    private String contextPath;

    protected ServerSettings() {
    }

    public static ServerSettings getCurrent() {
        return current;
    }

    protected static ServerSettings loadServerSettings() {
        Dictionary dictionary = Dictionary.getDictionary(SERVER_SETTINGS);
        String repositoryName = dictionary.get(REPOSITORY_NAME);
        String documentId = dictionary.get(DOCUMENT_ID);
        String contextPath = dictionary.get(CONTEXT_PATH);

        ServerSettings serverSettings = new ServerSettings();
        serverSettings.repositoryName = repositoryName;
        serverSettings.documentId = documentId;
        serverSettings.contextPath = contextPath;
        return serverSettings;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getDocumentId() {
        return documentId;
    }

}
