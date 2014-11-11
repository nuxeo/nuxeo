package org.nuxeo.ecm.platform.transform.plugin.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

public class PowerpointToTextPlugin extends AbstractPlugin {

    private static final long serialVersionUID = 87687986596859L;

    private static final Log log = LogFactory.getLog(PowerpointToTextPlugin.class);

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        List<TransformDocument> trs = new ArrayList<TransformDocument>();
        if (sources.length < 0 || sources[0] == null) {
            return trs;
        }

        SimpleTimer timer = new SimpleTimer();

        File f = null;
        OutputStream fas = null;
        try {
            timer.start();

            trs = super.transform(options, sources);

            PowerPointExtractor extractor = new PowerPointExtractor(sources[0].getBlob().getStream());

            byte[] bytes = extractor.getText().getBytes();
            f = File.createTempFile("po-ppt2text", ".txt");
            fas = new FileOutputStream(f);
            fas.write(bytes);

            Blob blob = new FileBlob(new FileInputStream(f));
            blob.setMimeType(getDestinationMimeType());
            trs.add(new TransformDocumentImpl(blob));

        } finally {
            if (f != null) {
                f.delete();
            }
            if (fas != null) {
                fas.close();
            }
            timer.stop();
            log.debug("Transformation terminated." + timer);
        }

        return trs;
    }

}
