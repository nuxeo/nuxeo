/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleManager;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.sql.Model;

/**
 * SQL Life Cycle Manager.
 *
 * @author Florent Guillaume
 */
public class SQLLifeCycleManager implements LifeCycleManager {

    public String getPolicy(Document document) throws LifeCycleException {
        try {
            return document.getString(Model.SYSTEM_LIFECYCLE_POLICY_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get policy", e);
        }
    }

    public void setPolicy(Document document, String policy)
            throws LifeCycleException {
        try {
            document.setString(Model.SYSTEM_LIFECYCLE_POLICY_PROP, policy);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set policy", e);
        }
    }

    public String getState(Document document) throws LifeCycleException {
        try {
            return document.getString(Model.SYSTEM_LIFECYCLE_STATE_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get state", e);
        }
    }

    public void setState(Document document, String state)
            throws LifeCycleException {
        try {
            document.setString(Model.SYSTEM_LIFECYCLE_STATE_PROP, state);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set state", e);
        }
    }

}
