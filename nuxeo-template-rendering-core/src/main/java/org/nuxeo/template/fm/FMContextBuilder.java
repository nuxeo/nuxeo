package org.nuxeo.template.fm;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.template.api.context.DocumentWrapper;
import org.nuxeo.template.context.AbstractContextBuilder;

import freemarker.template.TemplateModelException;

public class FMContextBuilder extends AbstractContextBuilder {

    protected static final Log log = LogFactory.getLog(FMContextBuilder.class);

    protected DocumentWrapper nuxeoWrapper;

    public FMContextBuilder() {
        final DocumentObjectWrapper fmWrapper = new DocumentObjectWrapper(null);

        nuxeoWrapper = new DocumentWrapper() {
            @Override
            public Object wrap(DocumentModel doc) {
                try {
                    return fmWrapper.wrap(doc);
                } catch (TemplateModelException e) {
                    throw new NuxeoException(e);
                }
            }

            public Object wrap(List<LogEntry> auditEntries) {
                try {
                    return fmWrapper.wrap(auditEntries);
                } catch (TemplateModelException e) {
                    throw new NuxeoException(e);
                }
            }
        };
    }

    @Override
    protected DocumentWrapper getWrapper() {
        return nuxeoWrapper;
    }

}
