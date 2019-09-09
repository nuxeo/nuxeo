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
 *
 *      Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.download.DownloadService.DownloadContext;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.restapi.server.jaxrs.blob.BlobObject;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.2
 */
@WebAdapter(name = PreviewAdapter.NAME, type = "previewAdapter")
@Produces({ MediaType.APPLICATION_JSON })
public class PreviewAdapter extends DefaultAdapter {

    public static final String NAME = "preview";

    @GET
    public Object preview(@QueryParam("blobPostProcessing") boolean postProcessing, @Context HttpServletRequest request,
            @Context HttpServletResponse response) {

        DocumentBlobHolder bh = getBlobHolderToPreview();
        if (bh == null) {
            return Response.status(NOT_FOUND).build();
        }

        // if it's a managed blob try to use the embed uri for preview
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            URI uri = blobManager.getURI(bh.getBlob(), BlobManager.UsageHint.EMBED, null);
            if (uri != null) {
                return Response.seeOther(uri).build();
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        List<Blob> previewBlobs = getPreviewBlobs(bh, postProcessing);
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            return buildPreviewNotAvailable();
        }

        try {
            Blob blob = previewBlobs.get(0);
            DownloadContext context = DownloadContext.newBuilder(request, response)
                                                     .doc(bh.getDocument())
                                                     .xpath(bh.getXpath())
                                                     .blob(blob)
                                                     .reason("preview")
                                                     .inline(true)
                                                     .build();
            Framework.getService(DownloadService.class).downloadBlob(context);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        return Response.ok().build();
    }

    protected Object buildPreviewNotAvailable() {
        return Response.status(NOT_FOUND)
                       .entity(getTemplate("preview/preview_not_available.ftl"))
                       .type("text/html")
                       .build();
    }

    @GET
    @Path("{subPath}")
    public Object subPath(@PathParam("subPath") String subPath,
            @QueryParam("blobPostProcessing") boolean postProcessing, @Context HttpServletRequest request,
            @Context HttpServletResponse response) {

        DocumentBlobHolder bh = getBlobHolderToPreview();
        if (bh == null) {
            return Response.status(NOT_FOUND).build();
        }

        List<Blob> previewBlobs = getPreviewBlobs(bh, postProcessing);
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            return buildPreviewNotAvailable();
        }

        // find blob
        Optional<Blob> subBlob = previewBlobs.stream().filter(b -> subPath.equals(b.getFilename())).findFirst();

        if (!subBlob.isPresent()) {
            return buildPreviewNotAvailable();
        }

        try {
            Blob blob = subBlob.get();
            DownloadContext context = DownloadContext.newBuilder(request, response)
                                                     .doc(bh.getDocument())
                                                     .xpath(bh.getXpath())
                                                     .blob(blob)
                                                     .reason("preview")
                                                     .extendedInfos(Collections.singletonMap("subPath", subPath))
                                                     .inline(true)
                                                     .build();
            Framework.getService(DownloadService.class).downloadBlob(context);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        return Response.ok().build();
    }

    private List<Blob> getPreviewBlobs(DocumentBlobHolder bh, boolean blobPostProcessing) {
        DocumentModel doc = bh.getDocument();
        String xpath = bh.getXpath();
        HtmlPreviewAdapter preview;

        try {
            if (isBlobTarget() && !isBlobHolder(doc, xpath)) {
                preview = PreviewHelper.getBlobPreviewAdapter(doc);
                return preview.getFilePreviewBlobs(xpath, blobPostProcessing);
            }

            preview = doc.getAdapter(HtmlPreviewAdapter.class);
            if (preview == null) {
                return Collections.emptyList();
            }

            return preview.getFilePreviewBlobs(blobPostProcessing);
        } catch (PreviewException e) {
            return Collections.emptyList();
        }
    }

    private DocumentBlobHolder getBlobHolderToPreview() {
        Resource target = getTarget();
        if (isBlobTarget()) {
            return ((BlobObject) target).getBlobHolder();
        } else {
            DocumentModel doc = target.getAdapter(DocumentModel.class);
            return (DocumentBlobHolder) doc.getAdapter(BlobHolder.class);
        }
    }

    private boolean isBlobTarget() {
        return getTarget().isInstanceOf("blob");
    }

    private boolean isBlobHolder(DocumentModel doc, String xpath) {
        DocumentBlobHolder bh = (DocumentBlobHolder) doc.getAdapter(BlobHolder.class);
        return bh != null && bh.getXpath().equals(xpath);
    }
}
