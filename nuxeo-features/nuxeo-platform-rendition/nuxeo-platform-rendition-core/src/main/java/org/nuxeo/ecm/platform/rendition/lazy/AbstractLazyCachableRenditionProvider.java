/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.work.api.Work.State;
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
            WorkManager wm = Framework.getService(WorkManager.class);
            ts.putParameter(key, WORKERID_KEY, work.getId());
            wm.schedule(work);
        } else {
            if (ts.isCompleted(key)) {
                List<Blob> blobs = ts.getBlobs(key);
                ts.release(key);
                return blobs;
            } else {
                WorkManager wm = Framework.getService(WorkManager.class);
                String workId = (String) ts.getParameter(key, WORKERID_KEY);
                Work work = wm.find(workId, State.COMPLETED);
                if (work == null) {
                    work = wm.find(workId, State.FAILED);
                }
                if (work != null) {
                    State state = work.getWorkInstanceState();
                    if (state == State.FAILED) {
                        // return an empty Blob
                        List<Blob> blobs = new ArrayList<Blob>();
                        StringBlob emptyBlob = new StringBlob("");
                        emptyBlob.setFilename("error");
                        emptyBlob.setMimeType("text/plain;" + LazyRendition.ERROR_MARKER);
                        blobs.add(emptyBlob);
                        ts.release(key);
                        return blobs;
                    }
                }
            }
        }
        // return an empty Blob
        List<Blob> blobs = new ArrayList<Blob>();
        StringBlob emptyBlob = new StringBlob("");
        emptyBlob.setFilename("inprogress");
        emptyBlob.setMimeType("text/plain;" + LazyRendition.EMPTY_MARKER);
        blobs.add(emptyBlob);
        return blobs;
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
            long millis = modif.getTimeInMillis();
            // the date may have been rounded by the storage layer, normalize it to the second
            millis -= millis % 1000;
            sb.append(millis);
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
