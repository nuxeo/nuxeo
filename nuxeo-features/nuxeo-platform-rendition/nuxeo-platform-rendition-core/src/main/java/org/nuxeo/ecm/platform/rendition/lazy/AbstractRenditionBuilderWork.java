/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.lazy;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.rendition.service.RenditionServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractRenditionBuilderWork extends AbstractWork implements Work, Serializable {

    private static final long serialVersionUID = 1L;

    protected final String key;

    protected final DocumentRef docRef;

    protected final String repositoryName;

    protected final String renditionName;

    protected final String originatingUsername;

    protected static Log log = LogFactory.getLog(AbstractRenditionBuilderWork.class);

    public AbstractRenditionBuilderWork(String key, DocumentModel doc, RenditionDefinition def) {
        this.key = key;
        docRef = doc.getRef();
        repositoryName = doc.getRepositoryName();
        renditionName = def.getName();
        originatingUsername = doc.getCoreSession().getPrincipal().getName();
    }

    @Override
    public String getId() {
        return "rendition:" + key;
    }

    @Override
    public String getTitle() {
        return "Lazy Rendition for " + renditionName + " on " + docRef.toString();
    }

    @Override
    public String getUserId() {
        return originatingUsername;
    }

    protected String getTransientStoreName() {
        return AbstractLazyCachableRenditionProvider.CACHE_NAME;
    }

    @Override
    public void work() {
        session = CoreInstance.openCoreSession(repositoryName, originatingUsername);
        DocumentModel doc = session.getDocument(docRef);

        RenditionService rs = Framework.getService(RenditionService.class);
        RenditionDefinition def = ((RenditionServiceImpl) rs).getRenditionDefinition(renditionName);

        List<Blob> blobs = doComputeRendition(session, doc, def);

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(getTransientStoreName());

        if (!ts.exists(key)) {
            throw new NuxeoException("Rendition TransientStore entry can not be null");
        }
        ts.putBlobs(key, blobs);
        ts.setCompleted(key, true);
    }

    /**
     * Does the actual Rendition Computation : this code will be called from inside an Asynchronous Work
     *
     * @param session
     * @param doc
     * @param def
     * @return
     */
    protected abstract List<Blob> doComputeRendition(CoreSession session, DocumentModel doc, RenditionDefinition def);

}
