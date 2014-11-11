package org.nuxeo.ecm.platform.syndication.restAPI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.syndication.serializer.ResultSummary;
import org.nuxeo.ecm.platform.syndication.serializer.SerializerHelper;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;

public abstract class BaseQueryModelRestlet extends BaseStatelessNuxeoRestlet {

    // const for QueryParameters
    protected static final String QPKEY = "QP";

    // const for Sorting
    protected static final String SORT_PARAM_COLOMN = "criteria";

    protected static final String SORT_PARAM_ASCENDING = "ascending";

    protected static final String PAGE_PARAM = "page";

    // const for USER Parameter
    protected static final String QPUSER = "$USER";


    private static final String defaultColumns = "dublincore.title,dublincore.description,url";

    private static final String defaultFormat = "XML";


    protected boolean sortAscending = false;

    protected String sortColomn = "";


    private static QueryModelService qmService;

    private static final Log log = LogFactory.getLog(BaseQueryModelRestlet.class);


    protected abstract String getQueryModelName(Request req);




    protected String getDefaultColumns(){
        return defaultColumns;
    }

    protected String getDefaultFormat(){
        return defaultFormat;
    }


    protected CoreSession getCoreSession(Request req, Response res, String repoName) {
        try {
            Repository repository = null;

            if (repoName==null) {
                repository = Framework.getService(RepositoryManager.class).getDefaultRepository();
            }
            else {
                repository = Framework.getService(RepositoryManager.class).getRepository(repoName);
            }

            if (repository == null) {
                throw new ClientException("Cannot get repository");
            }
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put("principal", getSerializablePrincipal(req));
            return repository.open(context);
        } catch (Exception e) {
            handleError(res, e);
            return null;
        }
    }

    @Override
    public void handle(Request req, Response res) {


        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();


        QueryModelService qmService = getQueryModelService(result, res);
        if (qmService==null) {
            return;
        }

        String qmName = getQueryModelName(req);

        QueryModelDescriptor qmd = qmService.getQueryModelDescriptor(qmName);
        if (qmd == null) {
            handleError(result, res, "can not find QueryModel " + qmName);
            return;
        }

        CoreSession session = getCoreSession(req, res, null);
        try {
            PagedDocumentsProvider provider = getPageDocumentsProvider(session,
                    qmd, req);

            // get Page number
            String pageS = req.getResourceRef().getQueryAsForm().getFirstValue(
                    PAGE_PARAM);
            int page = 0;
            if (pageS != null) {
                try {
                    page = Integer.parseInt(pageS);
                } catch (NumberFormatException e) {
                    page = 0;
                }
            }
            if (page >= provider.getNumberOfPages()) {
                handleError(result, res, "No Page " + page + " available");
                return;
            }

            // get format
            String format = req.getResourceRef().getQueryAsForm().getFirstValue(
                    "format");
            if (format == null) {
                format = getDefaultFormat();
            }

            // get Columns definition
            String columnsDefinition = req.getResourceRef().getQueryAsForm().getFirstValue(
                    "columns");
            if (columnsDefinition == null) {
                columnsDefinition = getDefaultColumns();
            }

            // fetch result
            DocumentModelList dmList = provider.getPage(page);

            ResultSummary summary = new ResultSummary();

            summary.setTitle("Result for search " + qmName);
            summary.setDescription("Result for search " + qmName);
            summary.setAuthor(getUserPrincipal(req).getName());
            summary.setModificationDate(new Date());
            summary.setLink(getRestletFullUrl(req));
            summary.setPages(provider.getNumberOfPages());
            summary.setPageNumber(page);

            // format result
            SerializerHelper.formatResult(summary, dmList, res, format, columnsDefinition, getHttpRequest(req));
        } catch (Exception e) {

            handleError(res, e);
        } finally {
            try {
                Repository.close(session);
            } catch (Exception e) {
                log.error("Repository close failed", e);
            }
        }
    }

    protected QueryModelService getQueryModelService(DOMDocument result,Response res){
        if (qmService==null) {
            try {
                qmService = (QueryModelService) Framework.getRuntime().getComponent(
                        QueryModelService.NAME);
                if (qmService == null) {
                    handleError(result, res, "Unable to get QueryModel Service");
                    return null;
                }
            } catch (Exception e) {
                handleError(result, res, e);
                return null;
            }
        }

        return qmService;
    }

    protected PagedDocumentsProvider getPageDocumentsProvider(
            CoreSession session, QueryModelDescriptor qmd, Request request)
            throws ClientException, QueryException {

        Object[] parameters = null;
        List<String> strParams = extractQueryParameters(request);
        if (strParams!=null && !strParams.isEmpty()) {
            parameters = strParams.toArray();
        }

        SortInfo sorter = null;
        if (sortColomn != null && !"".equals(sortColomn)) {
            sorter = new SortInfo(sortColomn, sortAscending);
        }
        if (qmd.isStateless()) {
            QueryModel qm = new QueryModel(qmd);
            return qm.getResultsProvider(session, parameters, sorter);
        } else {
            String docType = qmd.getDocType();
            DocumentModel doc = extractNamedParameters(request, docType);
            QueryModel qm = new QueryModel(qmd, doc);
            return qm.getResultsProvider(session, null, sorter);
        }
    }


    protected DocumentModel extractNamedParameters(Request req, String docType) throws PropertyException {
        DocumentModel doc = new DocumentModelImpl(docType);

        for (Schema schema : getSchemaManager().getDocumentType(docType).getSchemas()) {
            DataModel dm = new DataModelImpl(schema.getName());

            for (Field field : schema.getFields()) {
                final String localName = field.getName().getLocalName();
                String fName = schema.getName() + ':'
                        + localName;
                String fValue = req.getResourceRef().getQueryAsForm().getFirstValue(
                        fName);
                if (fValue != null) {
                    if (localName.equalsIgnoreCase("coverage")) {
                        dm.setData(localName, new Object[] {fValue});
                    } else {
                        dm.setData(localName, fValue);
                    }
                }
            }
            ((DocumentModelImpl) doc).addDataModel(dm);
        }

        return doc;
    }

    protected static SchemaManager getSchemaManager() {
        return Framework.getRuntime().getService(SchemaManager.class);
    }

    protected  List<String> extractQueryParameters(Request req) {
        List<String> qp = new ArrayList<String>();
        List<String> rp = new ArrayList<String>(
                req.getResourceRef().getQueryAsForm().getNames());

        // Sort QP as order is important
        Collections.sort(rp);

        for (String k : rp) {
            if (k.startsWith(QPKEY)) {
                String param = req.getResourceRef().getQueryAsForm().getFirstValue(
                        k);
                if (param != null) {
                    if (param.equals(QPUSER)) {
                        qp.add(getUserPrincipal(req).getName());
                    } else {
                        qp.add(param);
                    }
                }
            }
            if (k.startsWith(SORT_PARAM_COLOMN)) {
                sortColomn = req.getResourceRef().getQueryAsForm().getFirstValue(
                        k);
            }
            if (k.startsWith(SORT_PARAM_ASCENDING)) {
                String param = req.getResourceRef().getQueryAsForm().getFirstValue(
                        k);
                if (param != null
                        && (param.equals("TRUE") || param.equals("true") || param.equals("1"))) {
                    sortAscending = true;
                } else if (param != null
                        && (param.equals("FALSE") || param.equals("false") || param.equals("0"))) {
                    sortAscending = false;
                }
            }
        }
        return qp;
    }
}
