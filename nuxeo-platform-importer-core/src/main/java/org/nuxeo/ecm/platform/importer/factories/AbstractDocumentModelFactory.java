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

package org.nuxeo.ecm.platform.importer.factories;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Base class for classes implementing {@code ImporterDocumentModelFactory}.
 * Contains common methods.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractDocumentModelFactory implements
        ImporterDocumentModelFactory {

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * isTargetDocumentModelFolderish
     * (org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    public boolean isTargetDocumentModelFolderish(SourceNode node) {
        return node.isFolderish();
    }

    /**
     * Returns a valid Nuxeo name from the given {@code fileName}.
     */
    protected String getValidNameFromFileName(String fileName) {
        String name = IdUtils.generateId(fileName, "-", true, 100);
        name = name.replace("'", "");
        name = name.replace("(", "");
        name = name.replace(")", "");
        name = name.replace("+", "");
        return name;
    }

    protected void setDocumentProperties(Map<String, Serializable> properties,
            DocumentModel doc) throws ClientException {
        if (properties != null) {
            for (String pName : properties.keySet()) {
                doc.setPropertyValue(pName, properties.get(pName));
            }
        }
    }

}
