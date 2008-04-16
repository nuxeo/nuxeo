package org.nuxeo.ecm.core.url;


import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.ecm.core.url.nxdoc.Handler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class URLFactory {

    public static URL getURL(String url) throws MalformedURLException {
        if (url.startsWith("nxdoc:")) {
            return new URL(null, url, Handler.getInstance());
        } else if (url.startsWith("nxobj:")) {
            return new URL(null, url, org.nuxeo.ecm.core.url.nxobj.Handler.getInstance());
        } else {
            return new URL(url);
        }
    }
}
