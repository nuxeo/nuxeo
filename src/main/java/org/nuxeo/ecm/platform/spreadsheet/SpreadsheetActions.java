/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.spreadsheet;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Restful actions for Nuxeo Spreadsheet
 *
 * @since 5.9.6
 */
@Name("spreadsheetActions")
@Scope(EVENT)
public class SpreadsheetActions implements Serializable {

    @In(create = true)
    protected ContentViewService contentViewService;

    public String urlFor(ContentView contentView)
            throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();

        // Set layout name
        params.put("layout", contentView.getCurrentResultLayout().getName());

        // Set the columns
        List<String> columns = contentView.getCurrentResultLayoutColumns();
        if (columns != null) {
            params.put("columns", Joiner.on(',').join(columns));
        }

        // Set the pageprovider name
        params.put("provider", contentView.getPageProvider().getName());

        // Set the query
        String query = getQuery(contentView);
        if (query != null) {
            query = URLEncoder.encode(query, "UTF-8");
            query = query.replaceAll("\\+", "%20");
        }
        params.put("query", query);

        return VirtualHostHelper.getContextPathProperty() + "/spreadsheet?" +
                Joiner.on('&').withKeyValueSeparator("=").join(params);
    }

    protected String getQuery(ContentView contentView) {
        PageProvider pp = contentView.getPageProvider();

        // We only support query based page providers for now
        if (!(pp instanceof CoreQueryAndFetchPageProvider ||
              pp instanceof CoreQueryDocumentPageProvider)) {
            return "";
        }

        SortInfo[] sortArray = null;
        List<SortInfo> sortInfos = pp.getSortInfos();
        if (sortInfos != null) {
            sortArray = sortInfos.toArray(new SortInfo[] { });
        }
        String query;
        PageProviderDefinition def = pp.getDefinition();
        if (def.getWhereClause() == null) {
            query = NXQLQueryBuilder.getQuery(def.getPattern(),
                    pp.getParameters(), def.getQuotePatternParameters(),
                    def.getEscapePatternParameters(), pp.getSearchDocumentModel(), sortArray);
        } else {
            DocumentModel searchDocumentModel = pp.getSearchDocumentModel();
            if (searchDocumentModel == null) {
                throw new ClientException(String.format(
                        "Cannot build query of provider '%s': "
                                + "no search document model is set",
                        pp.getName()));
            }
            query = NXQLQueryBuilder.getQuery(searchDocumentModel,
                    def.getWhereClause(), pp.getParameters(), sortArray);
        }

        return query;
    }
}
