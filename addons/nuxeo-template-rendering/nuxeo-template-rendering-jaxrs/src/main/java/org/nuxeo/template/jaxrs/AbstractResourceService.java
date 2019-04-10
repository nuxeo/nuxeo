package org.nuxeo.template.jaxrs;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class AbstractResourceService extends DefaultObject {

    protected static Log log = LogFactory.getLog(AbstractResourceService.class);

    protected CoreSession session;

    public AbstractResourceService(CoreSession session) {
        this.session = session;
    }

    protected CoreSession getCoreSession() {
        return session;
    }

    protected List<TemplateSourceDocument> getTemplates() {
        CoreSession session = getCoreSession();
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getAvailableTemplates(session, null);
    }

}
