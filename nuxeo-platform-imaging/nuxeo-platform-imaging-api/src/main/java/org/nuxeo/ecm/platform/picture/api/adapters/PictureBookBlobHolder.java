package org.nuxeo.ecm.platform.picture.api.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public class PictureBookBlobHolder extends DocumentBlobHolder {

    private CoreSession session;

    public PictureBookBlobHolder(DocumentModel doc, String xPath) {
        super(doc, xPath);
    }

    @Override
    public Blob getBlob() throws ClientException {
        DocumentModel documentModel = getSession().getChildrenIterator(doc.getRef(), "Picture").next();
        PictureResourceAdapter picture = documentModel.getAdapter(PictureResourceAdapter.class);
        return picture.getPictureFromTitle("Original");
    }

    @Override
    public List<Blob> getBlobs() throws ClientException {
        return getBlobs("Original");
    }


    public List<Blob> getBlobs(String title) throws ClientException {
        DocumentModelList docList = getSession().getChildren(doc.getRef(), "Picture");
        List<Blob> blobList = new  ArrayList<Blob>(docList.size());
        for (DocumentModel documentModel : docList) {
                PictureResourceAdapter picture = documentModel.getAdapter(PictureResourceAdapter.class);
                blobList.add(picture.getPictureFromTitle(title));
        }
        return blobList;
    }

    @Override
    public String getHash() throws ClientException {

        Blob blob = getBlob();
        if (blob!=null) {
            String h = blob.getDigest();
            if (h!=null) {
                return h;
            }
        }
        return doc.getId() + xPath + getModificationDate().toString();
    }

    private CoreSession getSession() throws ClientException {
        if (session == null && doc!=null && doc.getSessionId()!=null){
            session = CoreInstance.getInstance().getSession(doc.getSessionId());
        }
        if (session == null){
        try {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            String repoName =null;
            if (doc!=null) {
                repoName = doc.getRepositoryName();
            }
            if (repoName!=null) {
                return rm.getRepository(repoName).open();
            }
            else {
                return rm.getDefaultRepository().open();
            }
        } catch (Exception e) {
            throw new ClientException("Cannot get default repository ", e);
        }
        } else {
            return session;
        }
    }

}
