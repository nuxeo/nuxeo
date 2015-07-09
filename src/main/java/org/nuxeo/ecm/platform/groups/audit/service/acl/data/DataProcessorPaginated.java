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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

public class DataProcessorPaginated extends DataProcessor {
    public static int MAX_DOCUMENTS = ExcelBuilder.MAX_ROW - 2;

    public static int DEFAULT_PAGE_SIZE = 1000;

    public static int EXCEL_RENDERING_RESERVED_TIME = 90; // seconds

    protected static int UNBOUNDED_PROCESS_TIME = -1;

    protected int pageSize = DEFAULT_PAGE_SIZE;

    public DataProcessorPaginated(IContentFilter filter) {
        this(filter, DEFAULT_PAGE_SIZE);
    }

    public DataProcessorPaginated(IContentFilter filter, int pageSize) {
        super(filter);
        this.pageSize = pageSize;
    }

    @Override
    protected void doAnalyze(CoreSession session, DocumentModel root, int timeout) {
        // get data
        DataFetch fetch = new DataFetch();
        CoreQueryDocumentPageProvider pages = fetch.getAllChildrenPaginated(session, root, pageSize, false);
        initSummarySet();

        // analyse root
        processDocument(root);

        // handling processing time
        t.tic();
        int maxProcessTime = UNBOUNDED_PROCESS_TIME;
        if (timeout > 0) {
            maxProcessTime = timeout - EXCEL_RENDERING_RESERVED_TIME;
            if (maxProcessTime <= 0) {
                throw new IllegalArgumentException("can't start a time bounded process with a timeout < "
                        + EXCEL_RENDERING_RESERVED_TIME + "(time period reserved for excel rendering)");
            }
        }

        // process children documents
        status = ProcessorStatus.SUCCESS;
        // iterate over pages
        overPages: do {
            log.debug("will get page " + p);
            final List<DocumentModel> page = pages.getCurrentPage();
            log.debug("page retrieved with query: " + pages.getCurrentQuery());
            log.debug("page size: " + page.size());

            // iterate over current page content
            for (DocumentModel m : page) {
                processDocument(m);
                t.toc(); // update elapsed time

                // verify exit conditions
                if (getNumberOfDocuments() == MAX_DOCUMENTS) {
                    // log.debug("will interrupt doc)
                    status = ProcessorStatus.ERROR_TOO_MANY_DOCUMENTS;
                    break overPages;
                }
                if (maxProcessTime != UNBOUNDED_PROCESS_TIME && t.toc() >= maxProcessTime) {
                    status = ProcessorStatus.ERROR_TOO_LONG_PROCESS;
                    break overPages;
                }
            }
            pages.nextPage();
            log.debug("done page " + (p++));
        } while (pages.isNextPageAvailable());
    }

    protected int p = 0;

    @Override
    public void initSummarySet() {
        allDocuments = new TreeSet<DocumentSummary>(new Comparator<DocumentSummary>() {
            @Override
            public int compare(DocumentSummary arg0, DocumentSummary arg1) {
                final String dp0 = arg0.getPath();
                final String dp1 = arg1.getPath();
                return dp0.compareTo(dp1);
            }
        });
    }
}
