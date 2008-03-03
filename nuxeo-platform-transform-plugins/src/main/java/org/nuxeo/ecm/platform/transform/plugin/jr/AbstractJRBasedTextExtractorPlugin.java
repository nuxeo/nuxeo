package org.nuxeo.ecm.platform.transform.plugin.jr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

public abstract class AbstractJRBasedTextExtractorPlugin extends AbstractPlugin {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AbstractJRBasedTextExtractorPlugin.class);

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        List<TransformDocument> trs = new ArrayList<TransformDocument>();
        if (sources.length < 0 || sources[0] == null) {
            return trs;
        }

        SimpleTimer timer = new SimpleTimer();
        Blob blob=null;
        try {
            timer.start();

            trs = super.transform(options, sources);

            for (TransformDocument td : sources)
            {
                blob = td.getBlob();
                Blob outblob = extractTextFromBlob(blob);
                trs.add(new TransformDocumentImpl(outblob));
            }

        } finally {
            blob=null;
            timer.stop();
            log.debug("Transformation terminated." + timer);
        }

        return trs;
    }

    protected Blob extractTextFromBlob(Blob blob) throws IOException
    {
        File f = null;
        OutputStream fas = null;
        Reader reader=null;

        try
        {
       TextExtractor extractor = getExtractor();

        reader = extractor.extractText(blob.getStream(), blob.getMimeType(), null);

        f = File.createTempFile("jr-2text", ".txt");
        fas = new FileOutputStream(f);
        org.apache.commons.io.IOUtils.copy(reader, fas, "UTF-8");
        Blob outblob = new FileBlob(new FileInputStream(f));
        outblob.setMimeType(getDestinationMimeType());
        return outblob;
        }
        finally
        {
            if (reader!=null)
            {
                try {
                    reader.close();
                } catch (IOException e) {
                   log.error("Error when closing reader", e);
                }
            }
            blob=null;
            if (f != null) {
                f.delete();
            }
            if (fas != null) {
                try {
                    fas.close();
                } catch (IOException e) {
                    log.error("Error when closing FileOutputStream", e);
                }
            }
        }

    }

    protected abstract TextExtractor getExtractor();
}
