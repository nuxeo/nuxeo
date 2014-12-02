/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

public class VersioningHelper {

    private static Log log = LogFactory.getLog(VersioningHelper.class);

    private VersioningHelper() {
        // Helper class
    }

    public static String getVersionLabelFor(DocumentModel doc) {
        VersioningManager service = Framework.getService(VersioningManager.class);
        try {
            return service.getVersionLabel(doc);
        } catch (ClientException e) {
            log.error("Unable to get VersionLabel for: "
                    + doc.getPathAsString(), e);
            return null;
        }
    }

}
