package org.nuxeo.template.web.url;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.url.AbstractRenditionRestHelper;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

@Name("templateRestHelper")
@Scope(ScopeType.PAGE)
public class TemplateRestHelper extends AbstractRenditionRestHelper {

    private static final long serialVersionUID = 1L;

    protected Blob renderAsBlob(DocumentModel doc, String renditionName)
            throws Exception {

        TemplateBasedDocument template = doc.getAdapter(TemplateBasedDocument.class);
        if (template != null) {
            return template.renderWithTemplate(renditionName);
        }
        return null;
    }
}
