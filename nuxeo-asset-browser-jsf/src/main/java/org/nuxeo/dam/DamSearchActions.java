/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.json.JSONException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.faceted.search.api.util.JSONMetadataExporter;
import org.nuxeo.ecm.platform.faceted.search.api.util.JSONMetadataHelper;
import org.nuxeo.ecm.platform.forms.layout.io.Base64;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
@Name("damSearchActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamSearchActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamSearchActions.class);

    public static final String DAM_FLAG = "DAM";

    public static final String DAM_CODEC = "docpathdam";

    public static final String CONTENT_VIEW_NAME_PARAMETER = "contentViewName";

    public static final String RESULT_LAYOUT_NAME_PARAMETER = "resultLayoutName";

    public static final String OFFSET_PARAMETER = "offset";

    public static final String PAGE_SIZE_PARAMETER = "pageSize";

    public static final String FILTER_VALUES_PARAMETER = "values";

    public static final String ENCODED_VALUES_ENCODING = "UTF-8";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected RestHelper restHelper;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    protected List<String> contentViewNames;

    protected String currentContentViewName;

    protected String offset;

    protected String pageSize;

    protected String resultLayoutName;

    public String getCurrentContentViewName() {
        if (currentContentViewName == null) {
            List<String> contentViewNames = getContentViewNames();
            if (!contentViewNames.isEmpty()) {
                currentContentViewName = contentViewNames.get(0);
            }
        }
        return currentContentViewName;
    }

    public void setCurrentContentViewName(String currentContentViewName) {
        this.currentContentViewName = currentContentViewName;
    }

    public List<String> getContentViewNames() {
        if (contentViewNames == null) {
            ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
            contentViewNames = new ArrayList<String>(
                    contentViewService.getContentViewNames(DAM_FLAG));
        }
        return contentViewNames;
    }

    public void clearSearch() throws ClientException {
        contentViewActions.reset(getCurrentContentViewName());
        updateCurrentDocument();
    }

    public void refreshAndRewind() throws ClientException {
        contentViewActions.refreshAndRewind(getCurrentContentViewName());
        updateCurrentDocument();
    }

    @SuppressWarnings("unchecked")
    public void updateCurrentDocument() throws ClientException {
        ContentView contentView = contentViewActions.getContentView(getCurrentContentViewName());
        updateCurrentDocument((PageProvider<DocumentModel>) contentView.getCurrentPageProvider());
    }

    public void updateCurrentDocument(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        if (pageProvider == null) {
            return;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        List<DocumentModel> docs = pageProvider.getCurrentPage();
        if (docs.isEmpty()) {
            // no document selected
            navigationContext.setCurrentDocument(null);
        } else if (!docs.contains(currentDocument)) {
            navigationContext.setCurrentDocument(docs.get(0));
        }
    }

    /*
     * ----- AJAX page navigation -----
     */

    public void firstPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.firstPage();
        updateCurrentDocument(pageProvider);
    }

    public void previousPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.previousPage();
        updateCurrentDocument(pageProvider);
    }

    public void nextPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.nextPage();
        updateCurrentDocument(pageProvider);
    }

    public void lastPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.lastPage();
        updateCurrentDocument(pageProvider);
    }

    /*
     * ----- Permanent link generation and loading -----
     */

    protected String encodeValues(String values)
            throws UnsupportedEncodingException {
        String encodedValues = Base64.encodeBytes(values.getBytes(),
                Base64.GZIP | Base64.DONT_BREAK_LINES);
        encodedValues = URLEncoder.encode(encodedValues,
                ENCODED_VALUES_ENCODING);
        return encodedValues;
    }

    protected String decodeValues(String values)
            throws UnsupportedEncodingException {
        String decodedValues = URLDecoder.decode(values,
                ENCODED_VALUES_ENCODING);
        decodedValues = new String(Base64.decode(decodedValues));
        return decodedValues;
    }

    /**
     * Set the metadata of the SearchDocumentModel from an encoded JSON string.
     */
    @SuppressWarnings("unchecked")
    public void setFilterValues(String filterValues) throws ClientException,
            JSONException, UnsupportedEncodingException {
        if (StringUtils.isBlank(filterValues)) {
            updateCurrentDocument();
            return;
        }

        ContentView contentView = contentViewActions.getContentViewWithProvider(
                getCurrentContentViewName(), null, null, null, null);
        DocumentModel searchDocumentModel = contentView.getSearchDocumentModel();
        String decodedValues = decodeValues(filterValues);
        searchDocumentModel = JSONMetadataHelper.setPropertiesFromJson(
                searchDocumentModel, decodedValues);
        contentView.setSearchDocumentModel(searchDocumentModel);

        if (!StringUtils.isBlank(resultLayoutName)) {
            contentView.setCurrentResultLayout(resultLayoutName);
        }

        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        if (!StringUtils.isBlank(pageSize)) {
            try {
                pageProvider.setPageSize(Long.valueOf(pageSize));
            } catch (NumberFormatException e) {
                log.warn(String.format(
                        "Unable to parse '%s' parameter with value '%s'",
                        PAGE_SIZE_PARAMETER, pageSize));
            }
        }
        if (!StringUtils.isBlank(offset)) {
            try {
                pageProvider.setCurrentPageOffset(Long.valueOf(offset));
            } catch (NumberFormatException e) {
                log.warn(String.format(
                        "Unable to parse '%s' parameter with value '%s'",
                        OFFSET_PARAMETER, offset));
            }
        }
        updateCurrentDocument(pageProvider);
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getResultLayoutName() {
        return resultLayoutName;
    }

    public void setResultLayoutName(String resultLayoutName) {
        this.resultLayoutName = resultLayoutName;
    }

    /**
     * Compute a permanent link for the current search.
     */
    @SuppressWarnings("unchecked")
    public String getPermanentLinkUrl() throws ClientException,
            UnsupportedEncodingException {
        String currentContentViewName = getCurrentContentViewName();
        DocumentView docView = new DocumentViewImpl(
                new DocumentLocationImpl(
                        documentManager.getRepositoryName(),
                        new PathRef(
                                navigationContext.getCurrentDocument().getPathAsString())));
        docView.setViewId("assets");
        docView.addParameter(CONTENT_VIEW_NAME_PARAMETER,
                currentContentViewName);
        ContentView contentView = contentViewActions.getContentView(currentContentViewName);
        docView.addParameter(RESULT_LAYOUT_NAME_PARAMETER, contentView.getCurrentResultLayout().getName());
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) contentView.getCurrentPageProvider();
        if (pageProvider != null) {
            docView.addParameter(OFFSET_PARAMETER,
                    String.valueOf(pageProvider.getCurrentPageOffset()));
            docView.addParameter(PAGE_SIZE_PARAMETER,
                    String.valueOf(pageProvider.getPageSize()));
        }
        DocumentModel doc = contentView.getSearchDocumentModel();
        String values = getEncodedValuesFrom(doc);
        docView.addParameter(FILTER_VALUES_PARAMETER, values);
        DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
        return documentViewCodecManager.getUrlFromDocumentView(DAM_CODEC,
                docView, true, BaseURL.getBaseURL());
    }

    /**
     * Returns an encoded JSON string computed from the {@code doc} metadata.
     */
    protected String getEncodedValuesFrom(DocumentModel doc)
            throws ClientException, UnsupportedEncodingException {
        JSONMetadataExporter exporter = new JSONMetadataExporter();
        String values = exporter.run(doc).toString();
        return encodeValues(values);
    }

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String loadPermanentLink(DocumentView docView)
            throws ClientException {
        restHelper.initContextFromRestRequest(docView);
        return "assets";
    }

}
