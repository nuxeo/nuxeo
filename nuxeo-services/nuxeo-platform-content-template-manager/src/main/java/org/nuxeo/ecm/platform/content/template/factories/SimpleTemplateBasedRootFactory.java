package org.nuxeo.ecm.platform.content.template.factories;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.content.template.listener.RepositoryInitializationListener;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;

/**
 * Specific factory for Root Since some other
 * {@link RepositoryInitializationListener} have run before, root won't be empty
 * but we may still have to run this initializer.
 *
 * @author Thierry Delprat
 *
 */
public class SimpleTemplateBasedRootFactory extends SimpleTemplateBasedFactory {

    @Override
    public void createContentStructure(DocumentModel eventDoc)
            throws ClientException {
        super.initSession(eventDoc);

        if (shouldCreateContent(eventDoc)) {
            for (TemplateItemDescriptor item : template) {
                String itemPath = eventDoc.getPathAsString();
                if (item.getPath() != null) {
                    itemPath = itemPath + "/" + item.getPath();
                }
                DocumentModel newChild = session.createDocumentModel(itemPath,
                        item.getId(), item.getTypeName());
                newChild.setProperty("dublincore", "title", item.getTitle());
                newChild.setProperty("dublincore", "description",
                        item.getDescription());
                setProperties(item.getProperties(), newChild);
                newChild = session.createDocument(newChild);
                setAcl(item.getAcl(), newChild.getRef());
            }
            // init root ACL if really empty
            setAcl(acl, eventDoc.getRef());
        }
    }

    /**
     * Returns {@code false} if the type of one of the children documents
     * matches a template item type, {@code true} otherwise.
     *
     * @param eventDoc
     * @throws ClientException
     */
    protected boolean shouldCreateContent(DocumentModel eventDoc)
            throws ClientException {
        for (TemplateItemDescriptor item : template) {
            DocumentModelList existingDocsOfTheSameType = session.getChildren(
                    eventDoc.getRef(), item.getTypeName());
            if (!existingDocsOfTheSameType.isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
