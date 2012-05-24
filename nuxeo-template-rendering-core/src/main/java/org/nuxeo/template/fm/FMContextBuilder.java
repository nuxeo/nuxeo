package org.nuxeo.template.fm;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.template.context.AbstractContextBuilder;
import org.nuxeo.template.context.DocumentWrapper;

public class FMContextBuilder extends AbstractContextBuilder {

    protected static final Log log = LogFactory.getLog(FMContextBuilder.class);

    protected DocumentWrapper nuxeoWrapper;

    public FMContextBuilder() {
        final DocumentObjectWrapper fmWrapper = new DocumentObjectWrapper(null);

        nuxeoWrapper = new DocumentWrapper() {
            @Override
            public Object wrap(DocumentModel doc) throws Exception {
                return fmWrapper.wrap(doc);
            }

            public Object wrap(List<LogEntry> auditEntries) throws Exception {
                return fmWrapper.wrap(auditEntries);
            }

        };
    }

    public Map<String, Object> build(DocumentModel doc) throws Exception {

        return build(doc, nuxeoWrapper);
    }

}
