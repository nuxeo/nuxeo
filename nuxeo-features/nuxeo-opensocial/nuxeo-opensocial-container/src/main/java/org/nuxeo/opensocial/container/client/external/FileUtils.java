package org.nuxeo.opensocial.container.client.external;

import com.google.gwt.user.client.Random;

public class FileUtils {
    public static final String AUTOMATION_FILES_URL = "site/automation/files/";
    public static final String AUTOMATION_FILES_PATH_ATTR = "?path=%2Fcontent";

    public native static String getBaseUrl() /*-{
        return $wnd.nuxeo.baseURL;
    }-*/;

    public static String buildFileUrl(String id) {
        // TODO We use a hack, to be sure that the image will be reloaded.
        String rdmAttr = "&rdm=" + Random.nextInt(100000);
        return getBaseUrl() + AUTOMATION_FILES_URL + id + AUTOMATION_FILES_PATH_ATTR
                + rdmAttr;
    }
}
