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
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
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

    protected String key;

    protected final DocumentRef docRef;

    protected final String repositoryName;

    protected final String renditionName;

    protected static final String CATEGORY = "renditionBuilder";

    protected static Log log = LogFactory.getLog(AbstractRenditionBuilderWork.class);

    public static final String CATEGORY = "renditionBuilder";

    public AbstractRenditionBuilderWork(String key, DocumentModel doc, RenditionDefinition def) {
        this.key = key;
        docRef = doc.getRef();
        repositoryName = doc.getRepositoryName();
        renditionName = def.getName();
        setOriginatingUsername(doc.getCoreSession().getPrincipal().getName());
        this.id = buildId(doc, def);
    }

    protected String buildId(DocumentModel doc, RenditionDefinition def) {
        StringBuffer sb = new StringBuffer("rendition:");
        sb.append(doc.getId());
        String variant = def.getProvider().getVariant(doc, def);
        if (variant != null) {
            sb.append("::");
            sb.append(variant);
        }
        sb.append("::");
        sb.append(def.getName());
        return sb.toString();
    }

    @Override
    public String getTitle() {
        return "Lazy Rendition for " + renditionName + " on " + docRef.toString()
                + " on behalf of " + originatingUsername;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
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

        RenditionProvider renditionProvider = def.getProvider();
        if (renditionProvider instanceof AbstractLazyCachableRenditionProvider) {
            String rendKey = ((AbstractLazyCachableRenditionProvider) renditionProvider).buildRenditionKey(doc, def);
            if (!rendKey.equals(key)) {
                refreshStoreEntry(rendKey);
                this.key = rendKey;
            }
        }

        List<Blob> blobs = doComputeRendition(session, doc, def);
        updateAndCompleteStoreEntry(blobs);
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
        updateAndCompleteStoreEntry(blobs);
    }

    void refreshStoreEntry(String renditionKey) {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(getTransientStoreName());

        // Release old key
        if (ts.exists(key)) {
            ts.release(key);
        }
        ts.putParameter(renditionKey, AbstractLazyCachableRenditionProvider.WORKERID_KEY, getId());
        List<Blob> blobs = new ArrayList<Blob>();
        StringBlob emptyBlob = new StringBlob("");
        emptyBlob.setFilename("inprogress");
        emptyBlob.setMimeType("text/plain;" + LazyRendition.EMPTY_MARKER);
        blobs.add(emptyBlob);
        ts.putBlobs(renditionKey, blobs);
    }

    void updateAndCompleteStoreEntry(List<Blob> blobs) {
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
