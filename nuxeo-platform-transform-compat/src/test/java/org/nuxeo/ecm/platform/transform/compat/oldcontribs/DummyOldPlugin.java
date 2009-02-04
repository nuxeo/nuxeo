package org.nuxeo.ecm.platform.transform.compat.oldcontribs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;

public class DummyOldPlugin extends AbstractPlugin implements Plugin {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        setSpecificOptions(options);

        List<TransformDocument> result = new ArrayList<TransformDocument>();

        String blobContent = sources[0].getBlob().getString();

        String output = "<html> DummyTest:" + blobContent + "</html>";

        TransformDocument tdoc = new TransformDocumentImpl(new StringBlob(output));

        result.add(tdoc);

        return result;

    }




}