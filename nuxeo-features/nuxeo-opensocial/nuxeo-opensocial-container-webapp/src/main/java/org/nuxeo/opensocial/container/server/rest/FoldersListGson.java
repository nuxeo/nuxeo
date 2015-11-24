package org.nuxeo.opensocial.container.server.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * @author Stéphane Fourrier
 */
public class FoldersListGson {

    private List<FolderGson> foldersList = new ArrayList<FolderGson>();

    public FoldersListGson(Collection<DocumentModel> children,
            CoreSession session) throws ClientException {
        for (DocumentModel child : children) {
            if (child.hasFacet(FacetNames.FOLDERISH)) {
                String previewDocId = new String("");
                if (session.hasChildren(child.getRef())) {
                    previewDocId = session.getChildren(child.getRef()).get(0).getId();
                }
                foldersList.add(new FolderGson(child, previewDocId));
            }
        }
    }
}
