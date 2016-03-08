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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.lazy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.AutomationRenderer;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of an asynchronous {@link RenditionProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractLazyCachableRenditionProvider implements RenditionProvider {

    public static final String WORKERID_KEY = "workerid";

    public static final String CACHE_NAME = "LazyRenditionCache";

    protected static Log log = LogFactory.getLog(AbstractLazyCachableRenditionProvider.class);

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition def) {

        // build the key
        String key = buildRenditionKey(doc, def);

        // see if rendition is already in process

        TransientStoreService tss = Framework.getService(TransientStoreService.class);

        TransientStore ts = tss.getStore(CACHE_NAME);

        if (ts == null) {
            throw new NuxeoException("Unable to find Transient Store  " + CACHE_NAME);
        }

        if (!ts.exists(key)) {
            Work work = getRenditionWork(key, doc, def);
            ts.putParameter(key, WORKERID_KEY, work.getId());
            List<Blob> blobs = new ArrayList<Blob>();
            StringBlob emptyBlob = new StringBlob("");
            emptyBlob.setFilename("inprogress");
            emptyBlob.setMimeType("text/plain;" + LazyRendition.EMPTY_MARKER);
            blobs.add(emptyBlob);
            ts.putBlobs(key, blobs);
            Framework.getService(WorkManager.class).schedule(work);
        } else {
            ts.release(key);
        }

        return ts.getBlobs(key);
     }

    @Override
    public String getVariant(DocumentModel doc, RenditionDefinition definition) {
        return AutomationRenderer.getVariant(doc, definition);
    }

    protected String buildRenditionKey(DocumentModel doc, RenditionDefinition def) {

        StringBuffer sb = new StringBuffer(doc.getId());
        sb.append("::");
        String modificationDatePropertyName = def.getSourceDocumentModificationDatePropertyName();
        Calendar modif = (Calendar) doc.getPropertyValue(modificationDatePropertyName);
        if (modif != null) {
            sb.append(modif.getTimeInMillis());
            sb.append("::");
        }
        String variant = getVariant(doc, def);
        if (variant != null) {
            sb.append(variant);
            sb.append("::");
        }
        sb.append(def.getName());

        return getDigest(sb.toString());
    }

    protected String getDigest(String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return key;
        }
        byte[] buf = digest.digest(key.getBytes());
        return toHexString(buf);
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    protected String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    /**
     * Return the {@link Work} that will compute the {@link Rendition}. {@link AbstractRenditionBuilderWork} can be used
     * as a base class
     *
     * @param key the key used to rendition
     * @param doc the target {@link DocumentModel}
     * @param def the {@link RenditionDefinition}
     * @return
     */
    protected abstract Work getRenditionWork(final String key, final DocumentModel doc, final RenditionDefinition def);

}
