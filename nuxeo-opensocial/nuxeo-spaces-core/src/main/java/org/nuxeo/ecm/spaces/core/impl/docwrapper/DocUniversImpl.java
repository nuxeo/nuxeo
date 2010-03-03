/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.AbstractUnivers;
import org.nuxeo.ecm.spaces.api.Univers;

public class DocUniversImpl extends AbstractUnivers {

    private final DocumentModel doc;

    DocUniversImpl(DocumentModel doc) {
        this.doc = doc;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public boolean isEqualTo(Univers univers) {
        return univers.getId() != null && univers.getId().equals(getId());
    }

    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (ClientException e) {
            return "";
        }
    }

    public String getId() {
        return doc.getId();
    }

    public String getName() {
        return doc.getName();
    }

    public String getTitle() {
        try {
            return doc.getTitle();
        } catch (ClientException e) {
            return "";
        }
    }

}
