/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Thomas Roger
 */

package org.nuxeo.opensocial.container.client.rpc;

import java.io.Serializable;

/**
 * Contains
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ContainerContext implements Serializable {

    protected String spaceId;

    protected String repositoryName;

    protected String documentContextId;

    private ContainerContext() {
    }

    public ContainerContext(String spaceId, String repositoryName,
            String documentContextId) {
        this.spaceId = spaceId;
        this.repositoryName = repositoryName;
        this.documentContextId = documentContextId;
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

}
