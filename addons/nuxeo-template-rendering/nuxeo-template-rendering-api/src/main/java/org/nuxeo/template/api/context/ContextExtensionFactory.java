package org.nuxeo.template.api.context;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Factory used to register new Context Extensions The resturned Object will be
 * directly accessible inside the Rendering context
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public interface ContextExtensionFactory {

    /**
     * Called before redering to let you add objects inside the rendering
     * context.
     * <p>
     * If the method returns an object, it will be automatically pushed inside
     * the context using the name defined in the contribution.
     * </p>
     * <p>
     * you can also directly push in the provided ctx map
     * </p>
     * 
     * @param currentDocument
     * @param wrapper
     * @param ctx
     * @return
     */
    Object getExtension(DocumentModel currentDocument, DocumentWrapper wrapper,
            Map<String, Object> ctx);
}
