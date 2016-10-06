/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.lazy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.rendition.service.RenditionServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractRenditionBuilderWork extends TransientStoreWork {

    private static final long serialVersionUID = 1L;

    protected final String key;

    protected final DocumentRef docRef;

    protected final String repositoryName;

    protected final String renditionName;

    protected static final String CATEGORY = "renditionBuilder";

    protected static Log log = LogFactory.getLog(AbstractRenditionBuilderWork.class);

    public AbstractRenditionBuilderWork(String key, DocumentModel doc, RenditionDefinition def) {
        this.key = key;
        docRef = doc.getRef();
        repositoryName = doc.getRepositoryName();
        renditionName = def.getName();
        setOriginatingUsername(doc.getCoreSession().getPrincipal().getName());
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
    public String getCategory() {
        return CATEGORY;
    }

    protected String getTransientStoreName() {
        return AbstractLazyCachableRenditionProvider.CACHE_NAME;
    }

    @Override
    public void work() {
        openUserSession();
        DocumentModel doc = session.getDocument(docRef);

        RenditionService rs = Framework.getService(RenditionService.class);
        RenditionDefinition def = ((RenditionServiceImpl) rs).getRenditionDefinition(renditionName);

        List<Blob> blobs = doComputeRendition(session, doc, def);
        doStore(blobs);
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        if (ok) {
            return;
        }
        List<Blob> blobs = new ArrayList<Blob>();
        StringBlob emptyBlob = new StringBlob("");
        emptyBlob.setFilename("error");
        emptyBlob.setMimeType("text/plain;" + LazyRendition.ERROR_MARKER);
        blobs.add(emptyBlob);
        doStore(blobs);
    }

    void doStore(List<Blob> blobs) {
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
