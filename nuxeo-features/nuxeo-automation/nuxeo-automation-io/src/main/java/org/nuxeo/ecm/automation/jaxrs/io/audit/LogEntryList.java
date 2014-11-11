package org.nuxeo.ecm.automation.jaxrs.io.audit;

import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;

public class LogEntryList extends PaginablePageProvider<LogEntry>{

    private static final long serialVersionUID = 1L;

    public LogEntryList(PageProvider<LogEntry> pageProvider) {
        super(pageProvider);
    }
}
