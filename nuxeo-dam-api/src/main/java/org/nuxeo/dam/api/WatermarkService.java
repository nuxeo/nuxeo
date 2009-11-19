package org.nuxeo.dam.api;

import java.io.File;
import java.io.IOException;

public interface WatermarkService {

    public File getWatermarkFile() throws IOException;
}
