package org.nuxeo.ecm.platform.transform.compat.oldcontribs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

public class DummyOldPlugin2 extends DummyOldPlugin {

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

            String output = "DummyTest2:" + blobContent;

            TransformDocument tdoc = new TransformDocumentImpl(new StringBlob(output));

            result.add(tdoc);

            return result;
        }

}
