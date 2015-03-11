package org.nuxeo.ecm.platform.rendition.extension;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

public class DefaultAutomationRenditionProvider implements RenditionProvider {

    protected static final Log log = LogFactory.getLog(DefaultAutomationRenditionProvider.class);

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        return AutomationRenderer.isRenditionAvailable(doc, def);
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) throws RenditionException {
        return AutomationRenderer.render(doc, definition, null);
    }
}
