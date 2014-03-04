package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * JSon writer that outputs an elasticsearch bulk format, response header
 * contains information about the number of results and pagination.
 * 
 * 
 * @since 5.9.3
 */
@Provider
@Produces({ "application/json+esentity" })
public class JsonESDocumentListWriter extends JsonDocumentListWriter {
    private static final Log log = LogFactory.getLog(JsonESDocumentListWriter.class);

    @Context
    private HttpServletResponse response;

    @Override
    public void writeDocuments(OutputStream out, List<DocumentModel> docs,
            String[] schemas) throws Exception {
        writeDocs(factory.createJsonGenerator(out, JsonEncoding.UTF8), docs, schemas);
    }

    public void writeDocs(JsonGenerator jg, List<DocumentModel> docs, String[] schemas)
            throws Exception {

        if (docs instanceof PaginableDocumentModelList) {
            PaginableDocumentModelList provider = (PaginableDocumentModelList) docs;
            response.setHeader("X-NXCurrentPageIndex",
                    Long.valueOf(provider.getCurrentPageIndex()).toString());
            response.setHeader("X-NXnumberOfPages",
                    Long.valueOf(provider.getNumberOfPages()).toString());
            response.setHeader("X-NXResultsCount",
                    Long.valueOf(provider.getResultsCount()).toString());
            response.setHeader("X-NXPageSize", Long.valueOf(provider.size()).toString());
            response.setHeader("X-NXIsLastPageAvailable",
                    Boolean.valueOf(provider.isLastPageAvailable()).toString());
            response.setHeader("X-NXHasError", Boolean.valueOf(provider.hasError())
                    .toString());
            response.setHeader("X-NXErrorMessage", provider.getErrorMessage());

            DocumentViewCodecManager documentViewCodecManager = Framework
                    .getLocalService(DocumentViewCodecManager.class);
            String codecName = null;
            if (documentViewCodecManager == null) {
                log.warn("Service 'DocumentViewCodecManager' not available : documentUrl won't be generated");
            } else {
                String documentLinkBuilder = provider.getDocumentLinkBuilder();
                codecName = isBlank(documentLinkBuilder) ? documentViewCodecManager
                        .getDefaultCodecName() : documentLinkBuilder;
            }
            for (DocumentModel doc : docs) {
                // write ES action
                jg.writeStartObject();
                jg.writeObjectFieldStart("index");
                // TODO get index and type name from request
                jg.writeStringField("_index", "nuxeo");
                jg.writeStringField("_type", "doc");
                jg.writeStringField("_id", doc.getId());
                jg.writeEndObject();
                jg.writeEndObject();
                jg.writeRaw('\n');
                // write ES data
                DocumentLocation docLoc = new DocumentLocationImpl(doc);
                Map<String, String> contextParameters = new HashMap<String, String>();
                if (documentViewCodecManager != null) {
                    DocumentView docView = new DocumentViewImpl(docLoc, doc.getAdapter(
                            TypeInfo.class).getDefaultView());
                    String documentURL = VirtualHostHelper.getContextPathProperty()
                            + "/"
                            + documentViewCodecManager.getUrlFromDocumentView(codecName,
                                    docView, false, null);
                    contextParameters.put("documentURL", documentURL);
                }
                JsonESDocumentWriter.writeESDocument(jg, doc, schemas, contextParameters);
                jg.writeRaw('\n');
            }
        } else {
            response.setHeader("X-NXCurrentPageIndex", "1");
            response.setHeader("X-NXnumberOfPages", "1");
            response.setHeader("X-NXIsLastPageAvailable", "1");
            response.setHeader("X-NXResultsCount", Integer.valueOf(docs.size())
                    .toString());
            response.setHeader("X-NXPageSize", Integer.valueOf(docs.size()).toString());
            response.setHeader("X-NXHasError", "false");
            for (DocumentModel doc : docs) {
                JsonESDocumentWriter.writeESDocument(jg, doc, schemas, null);
                jg.writeRaw('\n');
            }
        }
        jg.flush();
    }
}
