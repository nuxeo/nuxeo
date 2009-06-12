package org.nuxeo.dam.webapp.fileimporter;

import java.io.File;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

public class UploadItemMock extends UploadItem {

    private static final long serialVersionUID = 1L;

    protected Blob blob;

    public UploadItemMock(String fileName, String contentType, Object object) {
        super(fileName, contentType, object);

        File file = (File) object;

        blob = new FileBlob(file);
        blob.setFilename(file.getName());
    }


    public static final UploadEvent getUploadEvent(File file) {
        UploadItem item = new UploadItem(file.getName(), null, file);
        UIComponent component = new UIData();
        return new UploadEvent(component, item);
    }
}

