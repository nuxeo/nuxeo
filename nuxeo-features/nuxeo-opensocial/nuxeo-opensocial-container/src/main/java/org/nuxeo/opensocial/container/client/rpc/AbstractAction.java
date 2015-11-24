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

import java.util.Map;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

/**
 * Abstract class that must be extended by all other {@code Action}s.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractAction<T extends Result> implements Action<T> {

    protected ContainerContext containerContext;

    public AbstractAction(ContainerContext containerContext) {
        this.containerContext = containerContext;
    }

    protected AbstractAction() {
    }

    public String getSpaceId() {
        return containerContext.getSpaceId();
    }

    public String getRepositoryName() {
        return containerContext.getRepositoryName();
    }

    public String getDocumentContextId() {
        return containerContext.getDocumentContextId();
    }

    public String getUserLanguage() {
        return containerContext.getUserLanguage();
    }

    public Map<String, String> getParameters() {
        return containerContext.getParameters();
    }

}
