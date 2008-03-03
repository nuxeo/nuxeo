/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * VersionChangeRequest implementation used in tests. (This is to avoid using
 * nested class that puts surefire plugin in trouble
 * http://jira.codehaus.org/browse/SUREFIRE-44).
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class BasicImplVersionChangeRequest extends BasicVersionChangeRequest {

    private final VersioningActions versioningAction;

    public BasicImplVersionChangeRequest(RequestSource rs, DocumentModel doc,
            VersioningActions versioningAction) {
        super(rs, doc);
        this.versioningAction = versioningAction;
    }

    public VersioningActions getVersioningAction() {
        return versioningAction;
    }

}
