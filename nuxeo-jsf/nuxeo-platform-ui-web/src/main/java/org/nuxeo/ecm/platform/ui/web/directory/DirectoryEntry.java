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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;


/**
 * This class is used by ChainSelectOne / ChainSelectMany components.
 * It represents an entry in a directory.
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DirectoryEntry {

    public static final String XVOCABULARY = "xvocabulary";

    private final String parent;

    private final String id;

    private final String label;

    public DirectoryEntry(String schemaName, DocumentModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null");
        }

        if (schemaName == null) {
            throw new IllegalArgumentException("schemaName cannot be null");
        }

        try {
            id = (String) model.getProperty(schemaName, "id");
            label = (String) model.getProperty(schemaName, "label");
            if (XVOCABULARY.equals(schemaName)) {
                parent = (String) model.getProperty(schemaName, "parent");
            } else {
                parent = null;
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getParent() {
        return parent;
    }

}
