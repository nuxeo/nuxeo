/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This class is used by ChainSelectOne / ChainSelectMany components. It represents an entry in a directory.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
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

        id = (String) model.getProperty(schemaName, "id");
        label = (String) model.getProperty(schemaName, "label");
        if (XVOCABULARY.equals(schemaName)) {
            parent = (String) model.getProperty(schemaName, "parent");
        } else {
            parent = null;
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
