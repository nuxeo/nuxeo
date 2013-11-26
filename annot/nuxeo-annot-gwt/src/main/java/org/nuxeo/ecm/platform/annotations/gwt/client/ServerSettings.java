/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.gwt.client;

import com.google.gwt.i18n.client.Dictionary;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
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
