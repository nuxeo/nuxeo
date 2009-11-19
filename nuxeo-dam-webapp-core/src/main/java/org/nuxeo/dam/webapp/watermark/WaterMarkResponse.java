package org.nuxeo.dam.webapp.watermark;

import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WIDTH;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.dam.api.WatermarkService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.api.Framework;

public class WaterMarkResponse extends HttpServletResponseWrapper {

    private final Logger log = Logger.getLogger(WaterMarkResponse.class);

    protected File tmpFile = new File(System.getProperty("java.io.tmpdir"),
            UUID.randomUUID().toString());

    protected ServletOutputStream out = null;

    private static ImagingService imagingService;

    private static WatermarkService watermarkService;

    public WaterMarkResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new WaterMarkOutStream(tmpFile);
        }

        return out;
    }

    @Override
    public void flushBuffer() throws IOException {
        try {
            getOutputStream().close();
        } catch (Exception e) {
            // probably has been closed once.
        }

        File result = null;
        File wtmkdFile = null;

        try {

            Map<String, Object> imageMetadata = getImagingService().getImageMetadata(
                    new FileBlob(tmpFile));
            Integer width = (Integer) imageMetadata.get(META_WIDTH);
            Integer height = (Integer) imageMetadata.get(META_HEIGHT);

            File watermarkFile = getWatermarkService().getWatermarkFile();
            result = new File(tmpFile.getPath() + "_result");

            wtmkdFile = ImageWatermarker.watermark(watermarkFile.getPath(),
                    width, height, tmpFile.getPath(), result.getPath());
            FileInputStream fis = new FileInputStream(wtmkdFile);
            OutputStream os = getResponse().getOutputStream();

            FileUtils.copy(fis, os);
            fis.close();
            os.close();

            super.flushBuffer();

        }

        catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        finally {
            tmpFile.delete();
            if (result != null) {
                result.delete();
            }
            if (wtmkdFile != null) {
                wtmkdFile.delete();
            }
        }
    }

    protected ImagingService getImagingService() throws ClientException {
        if (imagingService == null) {
            try {
                imagingService = Framework.getService(ImagingService.class);
            } catch (Exception e) {
                log.error("Unable to get Imaging Service.", e);
            }
        }
        if (imagingService == null) {
            throw new ClientException("Unable to get Imaging Service: null");
        }
        return imagingService;
    }

    protected WatermarkService getWatermarkService() throws ClientException {
        if (watermarkService == null) {
            try {
                watermarkService = Framework.getService(WatermarkService.class);
            } catch (Exception e) {
                log.error("Unable to get Watermark Service.", e);
            }
        }
        if (watermarkService == null) {
            throw new ClientException("Unable to get Watermark Service: null");
        }
        return watermarkService;
    }
}
