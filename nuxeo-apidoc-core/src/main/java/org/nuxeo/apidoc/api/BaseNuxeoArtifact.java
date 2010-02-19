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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.apidoc.api;

import org.nuxeo.apidoc.documentation.AssociatedDocumensImpl;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public abstract class BaseNuxeoArtifact implements NuxeoArtifact {

    protected AssociatedDocuments associatedDocuments;

    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {

        if (associatedDocuments==null) {
            associatedDocuments = new AssociatedDocumensImpl(this, session);
        }
        return associatedDocuments;
    }

    public abstract String getId();

}
