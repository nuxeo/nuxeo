package org.nuxeo.apidoc.documentation;

public class JavaDocHelper {

    protected String defaultPrefix;
    protected String docVersion;

    public static final String BASE_URL = "http://www.nuxeo.org/api/";
    public static final String CM_BASE = "nuxeo-case-management";
    public static final String DM_BASE = "nuxeo";
    public static final String DAM_BASE = "nuxeo-dam";

    public static final String DEFAULT_DIST = DM_BASE;
    public static final String DEFAULT_VERSION = "5.4";

    public JavaDocHelper(String prefix, String version) {
        defaultPrefix= prefix;
        docVersion=version;
    }

    public String getBaseUrl(String className) {

        String base = defaultPrefix;

        if (className.contains("org.nuxeo.cm")) {
            base = CM_BASE;
        } else if (className.contains("org.nuxeo.dam")) {
            base = DAM_BASE;
        } else {
            base = DEFAULT_DIST;
        }

        return BASE_URL + base + "/" + docVersion;
    }

    public static JavaDocHelper getHelper(String distribName, String distribVersion) {

        String base = DEFAULT_DIST ;
        String version = DEFAULT_VERSION;

        if (distribName.toUpperCase().contains("CM") || distribName.toUpperCase().contains("CASE")) {
            base =CM_BASE;
        }
        else if (distribName.toUpperCase().contains("DAM")) {
            base =DAM_BASE;
        }

        version = distribVersion.substring(0,3);
        return new JavaDocHelper(base, version);
    }

}
