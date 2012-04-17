package org.nuxeo.template.jaxrs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
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
        try {
            CoreSession session = getCoreSession();
            TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
            return tps.getAvailableTemplates(session, null);
        } catch (Exception e) {
            log.error("Error while getting templates", e);
            return new ArrayList<TemplateSourceDocument>();
        }

        /*
         * StringBuffer sb = new StringBuffer();
         * 
         * sb.append("["); for (TemplateSourceDocument t : templates) {
         * sb.append("{"); sb.append("\"label\":" + "\"" + t.getLabel() +
         * "\","); sb.append("\"name\":" + "\"" + t.getName() + "\",");
         * sb.append("\"id\":" + "\"" + t.getId() + "\""); sb.append("},"); }
         * 
         * String result = sb.toString(); result = result.substring(0,
         * result.length()-2) + "]";
         * 
         * return result;
         */
    }

}
