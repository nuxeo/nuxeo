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
 * $Id: DocumentLocationImpl.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;

public class DocumentLocationImpl implements DocumentLocation {

    private final String serverLocationName;

    private final DocumentRef docRef;

    public DocumentLocationImpl(final String serverLocationName,
            final DocumentRef docRef) {
        this.serverLocationName = serverLocationName;
        this.docRef = docRef;
    }

    public DocumentRef getDocRef() {
        return docRef;
    }

    public String getServerLocationName() {
        return serverLocationName;
    }

}
