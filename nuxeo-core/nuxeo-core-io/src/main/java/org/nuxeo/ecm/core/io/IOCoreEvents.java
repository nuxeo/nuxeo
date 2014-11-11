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

package org.nuxeo.ecm.core.io;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * A helper class to declare and send events related to I/O processing.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 * @deprecated emptied, nuxeo-core-io must not use nuxeo-core
 */
@Deprecated
public class IOCoreEvents {

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DOCUMENT_EXPORTED = "documentExported";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DOCUMENT_IMPORTED = "documentImported";

    // Utility class.
    private IOCoreEvents() {
    }

    /**
     * Sends core events for the eventId for the given docs.
     *
     * @param docs
     * @param repositoryName
     * @param eventId
     * @throws ClientException
     * @deprecated cannot use nuxeo-core
     */
    @Deprecated
    public static void notifyEvents(Collection<DocumentRef> docs,
            String repositoryName, String eventId) {
    }

}
