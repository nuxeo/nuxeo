

import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RMILoaderHandlerBug {

    private static final Map pathToURLsCache = new WeakHashMap(5);

    private static String urlsToPath(URL[] urls) {
        if (urls.length == 0) {
            return null;
        } else if (urls.length == 1) {
            return urls[0].toExternalForm();
        } else {
            StringBuffer path = new StringBuffer(urls[0].toExternalForm());
            for (int i = 1; i < urls.length; i++) {
                path.append(' ');
                path.append(urls[i].toExternalForm());
            }
            return path.toString();
        }
    }

    /**
     * Convert a string containing a space-separated list of URLs into a
     * corresponding array of URL objects, throwing a MalformedURLException
     * if any of the URLs are invalid.
     */
    private static URL[] pathToURLs(String path)
    throws MalformedURLException
    {
        synchronized (pathToURLsCache) {
            Object[] v = (Object[]) pathToURLsCache.get(path);
            if (v != null) {
                return ((URL[])v[0]);
            }
        }
        StringTokenizer st = new StringTokenizer(path); // divide by spaces
        URL[] urls = new URL[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            urls[i] = new URL(st.nextToken());
        }
        synchronized (pathToURLsCache) {
            pathToURLsCache.put(path,
                    new Object[] {urls, new SoftReference(path)});
        }
        return urls;
    }


    public static void main(String[] args) {
        String path = "file:///C:/Program Files/MyApp";
        String codebase = null;
        try {
            System.out.println(new URL("file:///C:/Program Files/MyApp").toExternalForm());
            codebase = urlsToPath(new URL[] {new URL(path) });
            System.out.println("urlsToPath succeded: ["+codebase+"]");
        } catch (MalformedURLException e) {
            System.out.println("urlsToPath failed: "+e.getMessage());
        }

        try {
            URL[] urls = pathToURLs(codebase);
            if (urls.length == 1) {
                System.out.println("pathToURLs succeded: ["+urls[0]+"]");
            }
        } catch (MalformedURLException e) {
            System.out.println("pathToURLs failed: "+e.getMessage());
        }

    }
}
