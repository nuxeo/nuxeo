package org.nuxeo.ecm.platform.transform.transformer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

public class FullTextExtractor extends AbstractTransformer {

    /**
     *
     */
    private static final long serialVersionUID = 19879898996L;

    private static final Log log = LogFactory.getLog(FullTextExtractor.class);

    private static final long AUTO_DETECT_MT_MAX_SIZE=1024*1024*10;

    private static final String UNDEFINED_MT = "application/octet-stream";

    private static final String TXT_MT = "text/plain";

    private List<String> sourceMimeTypes=null;

    private long getMaxSizeForAutodetectingMimeType()
    {
        // XXX : TODO get it from options
        return AUTO_DETECT_MT_MAX_SIZE;
    }

    @Override
    public String getMimeTypeDestination() {
        return "text/plain";
    }

    @Override
    public List<String> getMimeTypeSources() {
        if (sourceMimeTypes==null)
        {
            List<Plugin> plugins= getNXTransform().getPluginByDestinationMimeTypes(TXT_MT);
            sourceMimeTypes= new ArrayList<String>();
            for (Plugin plugin : plugins)
            {
                sourceMimeTypes.addAll(plugin.getSourceMimeTypes());
            }
        }
        return sourceMimeTypes;
    }

    @Override
    public List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) {

        SimpleTimer timer = new SimpleTimer();
        timer.start();

        List<TransformDocument> results = new ArrayList<TransformDocument>();

        for (TransformDocument source : sources)
        {
            TransformDocument output=null;
            // get MT
            String mt = source.getBlob().getMimeType();
            if (mt==null || mt.equals(UNDEFINED_MT))
            {
                long blobSize = source.getBlob().getLength();
                if (blobSize>getMaxSizeForAutodetectingMimeType())
                {
                    mt=UNDEFINED_MT;
                }
                else
                {
                    try {
                        // reset MT to force computation
                        source.getBlob().setMimeType(null);
                        mt=source.getMimetype();
                    } catch (Exception e) {
                        mt=UNDEFINED_MT;
                    }
                }
            }

            if (!UNDEFINED_MT.equals(mt))
            {
                Plugin plugin = getNXTransform().getPluginByMimeTypes(mt, TXT_MT);
                if (plugin!=null)
                {
                    Map<String, Serializable> mergedOptions = mergeOptionsFor(plugin,
                            options != null ? options.get(plugin.getName()) : null);

                    TransformDocument[] input = new TransformDocument[1];
                    input[0]=source;
                    try {
                        List<TransformDocument> outputs = plugin.transform(mergedOptions, input);
                        output=outputs.get(0);
                    } catch (Exception e) {
                        log.error("Error in FullText extractor while calling plugin " + plugin.getName(), e);
                    }
                }
            }

            if (output==null)
            {
                output = new TransformDocumentImpl();
            }

            results.add(output);
        }

        timer.stop();
        log.info("Global transformation chain terminated for transformer name="
                + name + timer);

        return results;
    }

}
