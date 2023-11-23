/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.rendition;

import static org.nuxeo.ecm.core.io.download.DownloadService.EXTENDED_INFO_RENDITION;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.blobholder.DownloadContextBlobHolder;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.rendition.work.RenditionWork;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.2
 */
@WebObject(type = "rendition")
public class RenditionObject extends DefaultObject {

    public static final String LEGACY_RENDERING_PROP = "nuxeo.rendition.legacy.enabled";

    @Context
    protected HttpServletRequest servletRequest;

    protected String renditionName;

    protected DocumentModel doc;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length == 2;
        doc = (DocumentModel) args[0];
        renditionName = (String) args[1];
    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter.isAssignableFrom(Blob.class)) {
            if (Framework.isBooleanPropertyTrue(LEGACY_RENDERING_PROP)) {
                return adapter.cast(getLegacyRenditionBlob());
            }
            return adapter.cast(getRenditionBlob());
        }
        return super.getAdapter(adapter);
    }

    protected Blob getLegacyRenditionBlob() {
        RenditionService renditionService = Framework.getService(RenditionService.class);
        Rendition rendition = renditionService.getRendition(doc, renditionName);
        if (rendition == null) {
            throw new WebResourceNotFoundException(String.format("No rendition '%s' was found", renditionName));
        }
        return rendition.getBlob();
    }

    protected Blob getRenditionBlob() {
        try {
            RenditionWork work = new RenditionWork(doc, renditionName);
            return work.getRendition(Duration.ofMinutes(1));
        } catch (IllegalArgumentException e) {
            throw new IllegalParameterException(e.getMessage());
        } catch (TimeoutException | DocumentNotFoundException e) {
            throw new WebResourceNotFoundException(e.getMessage());
        }
    }

    /**
     * Get the rendition blob.
     * <p>
     * In legacy mode ({@code nuxeo.rendition.legacy.enabled=true}) all renditions are performed on the front nodes with
     * the risk of overflow when processing lots of large renditions on the fly.
     * <p>
     * In normal mode renditions are processed asynchronously in worker nodes and cached in order to scale, for light
     * renditions that are already preprocessed (like thumbnails) it is possible to use the {@code sync=true} parameter
     * to force rendition on front node avoiding possible async latency.
     */
    @GET
    public Object doGet(@Context Request request, @QueryParam("sync") boolean sync) {
        Blob blob;
        if (sync || Framework.isBooleanPropertyTrue(LEGACY_RENDERING_PROP)) {
            blob = getLegacyRenditionBlob();
        } else {
            blob = getRenditionBlob();
        }
        if (blob == null) {
            throw new WebResourceNotFoundException(
                    String.format("No Blob was found for rendition '%s'", renditionName));
        }
        DownloadContextBlobHolder blobHolder = new DownloadContextBlobHolder(blob);
        blobHolder.setDocument(doc);
        blobHolder.setReason("rendition");
        blobHolder.setExtendedInfos(Map.of(EXTENDED_INFO_RENDITION, renditionName));
        return blobHolder;
    }
}
