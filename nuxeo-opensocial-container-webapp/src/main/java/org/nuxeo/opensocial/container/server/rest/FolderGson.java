package org.nuxeo.opensocial.container.server.rest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author Stéphane Fourrier
 */
public class FolderGson {
    public String id;

    public String name;

    public String title;

    public String folderIconUrl = "";

    public String creator;

    public String previewDocId;

    public FolderGson() {
    }

    public FolderGson(DocumentModel folder, String previewDocId)
            throws ClientException {
        this.id = folder.getId();
        this.name = folder.getName();
        this.title = (String) folder.getPropertyValue("dc:title");
        this.folderIconUrl = (String) folder.getPropertyValue("common:icon");
        this.creator = (String) folder.getPropertyValue("dublincore:creator");
        this.previewDocId = previewDocId;
    }
}
