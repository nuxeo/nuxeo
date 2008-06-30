package org.nuxeo.ecm.platform.forum.web.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface ThreadAdapter {

    public List<DocumentModel> getAllPosts() throws ClientException;

    public List<DocumentModel> getPendingPosts() throws ClientException;

    public List<DocumentModel> getPublishedPosts() throws ClientException;

    public DocumentModel getLastPublishedPost() throws ClientException;

}
