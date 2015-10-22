package org.nuxeo.ecm.platform.rendition.service;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.convert.api.ConversionService;

import java.util.Calendar;

@Operation(id = DummyDocToTxt.ID, category = Constants.CAT_CONVERSION, label = "Convert Doc To Txt", description = "very dummy just for tests !")
public class DummyDocToTxt {

    public static final String ID = "DummyDoc.ToTxt";

    @Context
    protected ConversionService service;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {
        String content = doc.getTitle();
        String desc = "";
        Calendar expired = null;
        try {
            desc = (String) doc.getPropertyValue("dc:description");
            expired = (Calendar) doc.getPropertyValue("dc:expired");
        } catch (PropertyException ignored) {}
        if (StringUtils.isNotBlank(desc)) {
            content += String.format("%n" + desc);
        }
        if (expired != null) {
            long millis = expired.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
            if (millis > 0) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return Blobs.createBlob(content);
    }

}
