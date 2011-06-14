/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ContainerContext implements Serializable {

    protected String spaceId;

    protected String repositoryName;

    protected String documentContextId;

    protected String userLanguage;

    protected Map<String, String> parameters;

    private ContainerContext() {
    }

    public ContainerContext(String spaceId, String repositoryName,
            String documentContextId, String userLanguage) {
        this(spaceId, repositoryName, documentContextId, userLanguage,
                new HashMap<String, String>());
    }

    public ContainerContext(String spaceId, String repositoryName,
            String documentContextId, String userLanguage,
            Map<String, String> parameters) {
        this.spaceId = spaceId;
        this.repositoryName = repositoryName;
        this.documentContextId = documentContextId;
        this.userLanguage = userLanguage;
        this.parameters = parameters;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getDocumentContextId() {
        return documentContextId;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }
}
