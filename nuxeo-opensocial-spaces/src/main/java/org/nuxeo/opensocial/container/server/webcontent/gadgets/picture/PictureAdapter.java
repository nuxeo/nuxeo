package org.nuxeo.opensocial.container.server.webcontent.gadgets.picture;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;

/**
 * @author Stéphane Fourrier
 */
public class PictureAdapter extends AbstractWebContentAdapter<PictureData> {

    public PictureAdapter(DocumentModel doc) {
        super(doc);
    }

    public void feedFrom(PictureData data) throws ClientException {
        super.setMetadataFrom(data);
        doc.setPropertyValue("picture:caption", data.getPictureTitle());
        doc.setPropertyValue("dc:source", data.getPictureLink());
        doc.setPropertyValue("dc:description", data.getPictureLegend());

        if (!data.getFiles().isEmpty()) {
            Serializable pictureFile = data.getFiles().get(0);
            doc.setPropertyValue("file:content", pictureFile);
        } else if (doc.getPropertyValue("file:content") == null) {
            InputStream resourceAsStream = getClass().getResourceAsStream(
                    "/gadget/picture/thumbnail.png");
            try {
                Blob file = new FileBlob(resourceAsStream);
                doc.setPropertyValue("file:content", (Serializable) file);
            } catch (IOException e) {
                throw new ClientException("Cannot get default picture !",
                        e.getCause());
            }
        }
    }

    public PictureData getData() throws ClientException {
        PictureData data = new PictureData();

        super.getMetadataFor(data);

        data.setPictureTitle((String) doc.getPropertyValue("picture:caption"));
        data.setPictureLink((String) doc.getPropertyValue("dc:source"));
        data.setPictureLegend((String) doc.getPropertyValue("dc:description"));

        return data;
    }
}
