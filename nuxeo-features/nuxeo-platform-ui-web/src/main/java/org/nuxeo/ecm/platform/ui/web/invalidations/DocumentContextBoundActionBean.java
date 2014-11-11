package org.nuxeo.ecm.platform.ui.web.invalidations;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 *
 * Base class for Seam beans that use the Automatic invalidation system
 *
 * @author tiry
 *
 */
public abstract class DocumentContextBoundActionBean {

    private DocumentModel currentDocument;

    private NavigationContext navigationContext;

    protected NavigationContext getNavigationContext()
    {
        if (navigationContext==null)
            navigationContext = (NavigationContext) Component.getInstance("navigationContext",ScopeType.CONVERSATION);
        return navigationContext;
    }

    protected DocumentModel getCurrentDocument()
    {
        return currentDocument;
    }

    @DocumentContextInvalidation
    public void onContextChange(DocumentModel doc)
    {
        if (doc==null)
        {
            currentDocument=null;
            resetBeanCache(null);
            return;
        }
        else if (currentDocument==null)
        {
            currentDocument=doc;
            resetBeanCache(doc);
            return;
        }
        if (!doc.getRef().equals(currentDocument.getRef()))
        {
            currentDocument=doc;
            resetBeanCache(doc);
        }
    }

    protected abstract void resetBeanCache(DocumentModel newCurrentDocumentModel);

}
