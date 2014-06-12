package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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
@Produces({ JsonESDocumentListWriter.MIME_TYPE })
public class JsonESDocumentListWriter extends JsonDocumentListWriter {

    private static final Log log = LogFactory.getLog(JsonESDocumentListWriter.class);

    public static final String MIME_TYPE = "application/json+esentity";

    public static final String HEADER_ERROR_MESSAGE = "X-NXErrorMessage";

    public static final String HEADER_HAS_ERROR = "X-NXHasError";

    public static final String HEADER_PAGE_SIZE = "X-NXPageSize";

    public static final String HEADER_RESULTS_COUNT = "X-NXResultsCount";

    public static final String HEADER_IS_LAST_PAGE_AVAILABLE = "X-NXIsLastPageAvailable";

    public static final String HEADER_NUMBER_OF_PAGES = "X-NXnumberOfPages";

    public static final String HEADER_CURRENT_PAGE_INDEX = "X-NXCurrentPageIndex";

    public static final String DEFAULT_ES_INDEX = "nuxeo";

    public static final String DEFAULT_ES_TYPE = "doc";

    @Context
    private HttpServletResponse response;

    @Context
    private HttpServletRequest request;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return super.isWriteable(type, genericType, annotations, mediaType)
                && MIME_TYPE.equals(mediaType.toString());
    }

    /**
     * @since 5.9.5
     */
    @Override
    public void writeDocuments(OutputStream out, List<DocumentModel> docs,
            String[] schemas, HttpHeaders headers) throws Exception {
        writeDocs(factory.createJsonGenerator(out, JsonEncoding.UTF8), docs,
                schemas);
    }

    @Override
    public void writeDocuments(OutputStream out, List<DocumentModel> docs,
            String[] schemas) throws Exception {
        writeDocs(factory.createJsonGenerator(out, JsonEncoding.UTF8), docs,
                schemas, null);
    }

    /**
     * @since 5.9.5
     */
    public void writeDocs(JsonGenerator jg, List<DocumentModel> docs,
            String[] schemas) throws Exception {
        writeDocs(jg, docs, schemas, null);
    }

    public void writeDocs(JsonGenerator jg, List<DocumentModel> docs,
            String[] schemas, HttpHeaders headers) throws Exception {
        String esIndex = request.getParameter("esIndex");
        if (esIndex == null) {
            esIndex = DEFAULT_ES_INDEX;
        }
        String esType = request.getParameter("esType");
        if (esType == null) {
            esType = DEFAULT_ES_TYPE;
        }

        if (docs instanceof PaginableDocumentModelList) {
            PaginableDocumentModelList provider = (PaginableDocumentModelList) docs;
            response.setHeader(HEADER_CURRENT_PAGE_INDEX,
                    Long.valueOf(provider.getCurrentPageIndex()).toString());
            response.setHeader(HEADER_NUMBER_OF_PAGES,
                    Long.valueOf(provider.getNumberOfPages()).toString());
            response.setHeader(HEADER_RESULTS_COUNT,
                    Long.valueOf(provider.getResultsCount()).toString());
            response.setHeader(HEADER_PAGE_SIZE,
                    Long.valueOf(provider.size()).toString());
            response.setHeader(HEADER_IS_LAST_PAGE_AVAILABLE,
                    Boolean.valueOf(provider.isLastPageAvailable()).toString());
            response.setHeader(HEADER_HAS_ERROR,
                    Boolean.valueOf(provider.hasError()).toString());
            response.setHeader(HEADER_ERROR_MESSAGE, provider.getErrorMessage());

            DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
            String codecName = null;
            if (documentViewCodecManager == null) {
                log.warn("Service 'DocumentViewCodecManager' not available : documentUrl won't be generated");
            } else {
                String documentLinkBuilder = provider.getDocumentLinkBuilder();
                codecName = isBlank(documentLinkBuilder) ? documentViewCodecManager.getDefaultCodecName()
                        : documentLinkBuilder;
            }
            for (DocumentModel doc : docs) {
                // write ES action
                jg.writeStartObject();
                jg.writeObjectFieldStart("index");
                // TODO get index and type name from request
                jg.writeStringField("_index", esIndex);
                jg.writeStringField("_type", esType);
                jg.writeStringField("_id", doc.getId());
                jg.writeEndObject();
                jg.writeEndObject();
                jg.writeRaw('\n');
                // write ES data
                DocumentLocation docLoc = new DocumentLocationImpl(doc);
                Map<String, String> contextParameters = new HashMap<String, String>();
                if (documentViewCodecManager != null) {
                    DocumentView docView = new DocumentViewImpl(docLoc,
                            doc.getAdapter(TypeInfo.class).getDefaultView());
                    String documentURL = VirtualHostHelper.getContextPathProperty()
                            + "/"
                            + documentViewCodecManager.getUrlFromDocumentView(
                                    codecName, docView, false, null);
                    contextParameters.put("ecm:documentUrl", documentURL);
                }
                JsonESDocumentWriter.writeESDocument(jg, doc, schemas,
                        contextParameters);
                jg.writeRaw('\n');
            }
        } else {
            response.setHeader(HEADER_CURRENT_PAGE_INDEX, "1");
            response.setHeader(HEADER_NUMBER_OF_PAGES, "1");
            response.setHeader(HEADER_IS_LAST_PAGE_AVAILABLE, "1");
            response.setHeader(HEADER_RESULTS_COUNT,
                    Integer.valueOf(docs.size()).toString());
            response.setHeader(HEADER_PAGE_SIZE,
                    Integer.valueOf(docs.size()).toString());
            response.setHeader(HEADER_HAS_ERROR, "false");
            for (DocumentModel doc : docs) {
                JsonESDocumentWriter.writeESDocument(jg, doc, schemas, null);
                jg.writeRaw('\n');
            }
        }
        jg.flush();
    }
}
