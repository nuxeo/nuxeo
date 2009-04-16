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

package org.nuxeo.ecm.core.search.api.indexingwrapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class IndexingWrapperManagerComponent extends DefaultComponent implements IndexingWrapperManagerService {

    public static final String INDEXINGWRAPPER_FACTORY_EP = "IndexingWrapperFactories";

    private static final Log log = LogFactory.getLog(IndexingWrapperManagerComponent.class);

    protected final Map<String, DocumentIndexingWrapperFactory> wrapperFactories
            = new HashMap<String, DocumentIndexingWrapperFactory>();

    // EP management
    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
        if (INDEXINGWRAPPER_FACTORY_EP.equals(extensionPoint)) {
            DocumentModelIndexingWrapperDescriptor desc = (DocumentModelIndexingWrapperDescriptor) contribution;
            DocumentIndexingWrapperFactory factory = desc.getNewInstance();
            wrapperFactories.put(desc.getTypeName(), factory);
            log.info("registred new IndexingWrapper for type " + desc.getTypeName());
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
    }

    // service interface
    public DocumentModel getIndexingWrapper(DocumentModel doc) {
        String docType = doc.getType();
        DocumentIndexingWrapperFactory factory = wrapperFactories.get(docType);
        if (factory == null) {
            return doc;
        } else {
            DocumentModel wrapper = factory.getIndexingWrapper(doc);
            if (wrapper == null) {
                return doc;
            } else {
                return wrapper;
            }
        }
    }

}
