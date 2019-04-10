package org.nuxeo.template.automation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.template.adapters.doc.TemplateBindings;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

/**
 * Operation to wrapp the rendition process
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Operation(id = RenderWithTemplateOperation.ID, category = Constants.CAT_CONVERSION, label = "Render with template", description = "Render the target document with the associated template if any. Returns the rendered Blob or the main Blob if no template is associated to the document.")
public class RenderWithTemplateOperation {

    public static final String ID = "TemplateProcessor.Render";

    @Context
    protected OperationContext ctx;

    @Param(name = "templateName", required = false)
    protected String templateName = TemplateBindings.DEFAULT_BINDING;

    @Param(name = "store", required = false, values = "false")
    protected Boolean store = false;

    @Param(name = "save", required = false, values = "true")
    protected Boolean save = true;

    @OperationMethod
    public Blob run(DocumentModel targetDocument) throws Exception {
        TemplateBasedDocument renderable = targetDocument.getAdapter(TemplateBasedDocument.class);
        if (renderable != null) {
            if (store) {
                return renderable.renderAndStoreAsAttachment(templateName, save);
            } else {
                return renderable.renderWithTemplate(templateName);
            }
        } else {
            BlobHolder bh = targetDocument.getAdapter(BlobHolder.class);
            if (bh != null) {
                return bh.getBlob();
            } else {
                return null;
            }
        }
    }
}
