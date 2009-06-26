package org.nuxeo.ecm.platform.picture.convert.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;

public class ImagingTestRessources {

    public static final String TEST_DATA_FOLDER = "test-data/";
    
    public static final List<String> TEST_IMAGE_FILENAMES = Arrays.asList(
            "big_nuxeo_logo.jpg", "big_nuxeo_logo.gif", "big_nuxeo_logo.png");

    public static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assert file.length() > 0;
        return file;
    }

}
