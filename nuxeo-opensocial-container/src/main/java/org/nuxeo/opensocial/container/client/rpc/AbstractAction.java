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

import java.util.Map;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

/**
 * Abstract class that must be extended by all other {@code Action}s.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractAction<T extends Result> implements Action<T> {

    protected String spaceId;

    protected String repositoryName;

    protected String documentContextId;

    protected String userLanguage;

    protected Map<String, String> parameters;

    public AbstractAction(ContainerContext containerContext) {
        this.repositoryName = containerContext.getRepositoryName();
        this.spaceId = containerContext.getSpaceId();
        this.documentContextId = containerContext.getDocumentContextId();
        this.userLanguage = containerContext.getUserLanguage();
        this.parameters = containerContext.getParameters();
    }

    protected AbstractAction() {
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

}
