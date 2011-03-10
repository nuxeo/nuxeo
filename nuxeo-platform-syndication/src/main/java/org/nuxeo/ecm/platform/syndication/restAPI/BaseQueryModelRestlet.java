/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

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
import org.nuxeo.ecm.platform.syndication.serializer.DocumentModelListSerializer;
import org.nuxeo.ecm.platform.syndication.serializer.ResultSummary;
import org.nuxeo.ecm.platform.syndication.serializer.SerializerHelper;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Form;
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

    private static final String defaultColumns = "dublincore.title,dublincore.description,dublincore.creator,url,icon,dublincore.created,dublincore.modified";

    private static final String defaultFormat = "XML";

    private static QueryModelService qmService;

    private static final Log log = LogFactory.getLog(BaseQueryModelRestlet.class);

    protected boolean sortAscending = false;

    protected String sortColomn = "";

    protected abstract String getQueryModelName(Request req);

    protected String getDefaultColumns() {
        return defaultColumns;
    }

    protected String getDefaultFormat() {
        return defaultFormat;
    }

    protected CoreSession getCoreSession(Request req, Response res,
            String repoName) {
        try {
            Repository repository;

            if (repoName == null) {
                repository = Framework.getService(RepositoryManager.class).getDefaultRepository();
            } else {
                repository = Framework.getService(RepositoryManager.class).getRepository(
                        repoName);
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
        if (qmService == null) {
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
            Form form = req.getResourceRef().getQueryAsForm();
            String pageS = form.getFirstValue(PAGE_PARAM);
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
            String format = form.getFirstValue("format");
            if (format == null) {
                format = getDefaultFormat();
            }

            // get Columns definition
            String columnsDefinition = form.getFirstValue("columns");
            if (columnsDefinition == null) {
                columnsDefinition = defaultColumns;
            }

            // get format
            String lang = form.getFirstValue("lang");

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

            if (lang != null) {
                String[] cols = columnsDefinition.split(DocumentModelListSerializer.colDefinitonDelimiter);
                List<String> labels = new ArrayList<String>();
                for (String col : cols) {
                    labels.add("label." + col);
                }
                // format result
                SerializerHelper.formatResult(summary, dmList, res, format,
                        columnsDefinition, getHttpRequest(req), labels, lang);
            }
            else {
                // format result
                SerializerHelper.formatResult(summary, dmList, res, format,
                        columnsDefinition, getHttpRequest(req));
            }
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

    protected QueryModelService getQueryModelService(DOMDocument result,
            Response res) {
        if (qmService == null) {
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
        List<Object> strParams = extractQueryParameters(request);
        if (strParams != null && !strParams.isEmpty()) {
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

    protected DocumentModel extractNamedParameters(Request req, String docType)
            throws PropertyException {
        DocumentModel doc = new DocumentModelImpl(docType);

        for (Schema schema : getSchemaManager().getDocumentType(docType).getSchemas()) {
            DataModel dm = new DataModelImpl(schema.getName());

            for (Field field : schema.getFields()) {
                final String localName = field.getName().getLocalName();
                String fName = schema.getName() + ':' + localName;
                String fValue = req.getResourceRef().getQueryAsForm().getFirstValue(
                        fName);
                if (fValue != null) {
                    if (localName.equalsIgnoreCase("coverage")
                            || localName.equalsIgnoreCase("subjects")
                            || localName.equalsIgnoreCase("currentLifeCycleStates")) {
                        dm.setData(localName, new Object[] { fValue });
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

    protected List<Object> extractQueryParameters(Request req) {
        List<Object> qp = new ArrayList<Object>();
        Form form = req.getResourceRef().getQueryAsForm();
        List<String> rp = new ArrayList<String>(form.getNames());

        // Sort QP as order is important
        Collections.sort(rp);

        for (String k : rp) {
            if (k.startsWith(QPKEY)) {
                String param = form.getFirstValue(k);
                if (param != null) {
                    if (param.equals(QPUSER)) {
                        qp.add(getUserPrincipal(req).getName());
                    } else {
                        qp.add(param);
                    }
                }
            }
            if (k.startsWith(SORT_PARAM_COLOMN)) {
                sortColomn = form.getFirstValue(k);
            }
            if (k.startsWith(SORT_PARAM_ASCENDING)) {
                String param = form.getFirstValue(k);
                if ("true".equalsIgnoreCase(param) || "1".equals(param)) {
                    sortAscending = true;
                } else if ("false".equalsIgnoreCase(param) || "0".equals(param)) {
                    sortAscending = false;
                }
            }
        }
        return qp;
    }
}
