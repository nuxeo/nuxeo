/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.factories;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for classes implementing {@code ImporterDocumentModelFactory}. Contains common methods.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractDocumentModelFactory implements ImporterDocumentModelFactory {

    private static final Log log = LogFactory.getLog(AbstractDocumentModelFactory.class);

    /**
     * By default there is no process bound to a folderish node creation error, and the global import task will
     * continue.
     * <p>
     * You should override this method if you want a specific process to be executed after such an error and/or if you
     * want the global import task to stop immediately after the error occurs, in which case the method should return
     * false.
     * </p>
     */
    @Override
    public boolean processFolderishNodeCreationError(CoreSession session, DocumentModel parent, SourceNode node) {
        log.info(String.format("Nothing to process after error while trying to create the folderish node %s.",
                node.getSourcePath()));
        log.info("Global import task will continue.");
        return true;
    }

    /**
     * By default there is no process bound to a leaf node creation error, and the global import task will continue.
     * <p>
     * You should override this method if you want a specific process to be executed after such an error and/or if you
     * want the global import task to stop immediately after the error occurs, in which case the method should return
     * false.
     * </p>
     */
    @Override
    public boolean processLeafNodeCreationError(CoreSession session, DocumentModel parent, SourceNode node) {
        log.info(String.format("Nothing to process after error while trying to create the leaf node %s.",
                node.getSourcePath()));
        log.info("Global import task will continue.");
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory# isTargetDocumentModelFolderish
     * (org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    public boolean isTargetDocumentModelFolderish(SourceNode node) {
        return node.isFolderish();
    }

    protected final FilenameNormalizer filenameNormalizer = "true".equals(Framework.getProperty("nuxeo.importer.compatFilenames")) ? new CompatFilenameNormalizer()
            : new DefaultFilenameNormalizer();

    protected interface FilenameNormalizer {
        String normalize(String name);
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
        public String normalize(String name) {
            DocumentModel fake = new DocumentModelImpl("/", name, "File");
            return Framework.getService(PathSegmentService.class).generatePathSegment(fake);
        }
    }

    /**
     * Returns a valid Nuxeo name from the given {@code fileName}.
     *
     * @throws PropertyException
     */
    protected String getValidNameFromFileName(String fileName) {
        return filenameNormalizer.normalize(fileName);
    }

    /**
     * Set all the properties to the given {@code doc}. The key is field xpath, the value is the value to set on the
     * document.
     */
    protected DocumentModel setDocumentProperties(CoreSession session, Map<String, Serializable> properties,
            DocumentModel doc) {
        if (properties != null) {

            for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
                try {
                    doc.setPropertyValue(entry.getKey(), entry.getValue());
                } catch (PropertyNotFoundException e) {
                    String message = String.format("Property '%s' not found on document type: %s. Skipping it.",
                            entry.getKey(), doc.getType());
                    log.debug(message);
                }
            }
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}
