package org.nuxeo.opensocial.container.server.webcontent.gadgets.picture;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;

/**
 * @author Stéphane Fourrier
 */
public class PictureAdapter extends AbstractWebContentAdapter implements
        WebContentAdapter<PictureData> {

    public PictureAdapter(DocumentModel doc) {
        super(doc);
    }

    public void feedFrom(PictureData data) throws ClientException {
        super.setMetadataFrom(data);
        doc.setPropertyValue("dc:description", data.getUrl());
        doc.setPropertyValue("picture:caption", data.getPictureTitle());
    }

    public PictureData getData() throws ClientException {
        PictureData data = new PictureData();

        super.getMetadataFor(data);
        data.setUrl((String) doc.getPropertyValue("dc:description"));
        data.setPictureTitle((String) doc.getPropertyValue("picture:caption"));

        return data;
    }
}
