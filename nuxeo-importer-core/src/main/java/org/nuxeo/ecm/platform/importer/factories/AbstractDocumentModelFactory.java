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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for classes implementing {@code ImporterDocumentModelFactory}.
 * Contains common methods.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractDocumentModelFactory implements
        ImporterDocumentModelFactory {

    private static final Log log = LogFactory.getLog(AbstractDocumentModelFactory.class);

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

    protected final FilenameNormalizer filenameNormalizer = 
       "true".equals(Framework.getProperty("nuxeo.importer.compatFilenames"))
            ? new CompatFilenameNormalizer()
            : new DefaultFilenameNormalizer();
     
    protected interface FilenameNormalizer {
        String normalize(String name) throws ClientException;
    }
    
    protected static class CompatFilenameNormalizer implements FilenameNormalizer {

        @Override
        public String normalize(String name) {
            name = IdUtils.generateId(name, "-", true, 100);
            name = name.replace("'", "");
            name = name.replace("(", "");
            name = name.replace(")", "");
            name = name.replace("+", "");
            return name;
        }
        
    }
    
    protected static class DefaultFilenameNormalizer implements FilenameNormalizer {

        @Override
        public String normalize(String name) throws ClientException {
            DocumentModel fake = new DocumentModelImpl("/", name, "File");
            return Framework.getLocalService(PathSegmentService.class).generatePathSegment(
                    fake);
        }
    }
    
    /**
     * Returns a valid Nuxeo name from the given {@code fileName}.
     * @throws ClientException 
     * @throws PropertyException 
     */
    protected String getValidNameFromFileName(String fileName) throws ClientException {
        return filenameNormalizer.normalize(fileName);
    }

    /**
     * Set all the properties to the given {@code doc}. The key is field xpath,
     * the value is the value to set on the document.
     *
     */
    protected DocumentModel setDocumentProperties(CoreSession session,
            Map<String, Serializable> properties, DocumentModel doc)
            throws ClientException {
        if (properties != null) {

            for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
                try {
                    doc.setPropertyValue(entry.getKey(), entry.getValue());
                } catch (PropertyNotFoundException e) {
                    String message = String.format(
                            "Property '%s' not found on document type: %s. Skipping it.",
                            entry.getKey(), doc.getType());
                    log.debug(message);
                }
            }
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}
