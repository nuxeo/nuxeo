package org.nuxeo.dam.core.watermark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.dam.api.WatermarkService;

public class WatermarkServiceImpl implements WatermarkService {

    private File file;

    public File getWatermarkFile() throws IOException {
        if (file == null) {
            file = new File(System.getProperty("java.io.tmpdir"),
                    UUID.randomUUID().toString());
            InputStream is = getClass().getClassLoader().getResourceAsStream(
                    "watermark/image/dam_logo.png");
            FileUtils.copyToFile(is, file);
            is.close();

            file.deleteOnExit();
        }

        return file;
    }
}
