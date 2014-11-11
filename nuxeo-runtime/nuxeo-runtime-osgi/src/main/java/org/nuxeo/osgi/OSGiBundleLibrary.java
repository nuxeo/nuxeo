package org.nuxeo.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.jar.Manifest;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiBundleLibrary extends OSGiBundleHost {

    protected OSGiBundleLibrary(OSGiBundleFile file)
            throws BundleException {
        super(file);
    }

    @Override
    protected Dictionary<String, String> loadHeaders(OSGiBundleFile file,
            Manifest mf) throws BundleException {
        String name;
        Enumeration<URL> poms = file.findEntries("/META-INF",
                "pom.properties", true);
        if (poms.hasMoreElements()) {
            URL pomLocation = poms.nextElement();
            Properties properties = new Properties();
            try (InputStream input = pomLocation.openStream()) {
                properties.load(input);
            } catch (IOException e) {
                ;
            }
            name = properties.get("groupId") + "."
                    + properties.getProperty("artifactId");
        } else {
            name = file.getFileName();
        }
        Hashtable<String,String> dict = new Hashtable<String,String>();
        dict.put(Constants.BUNDLE_SYMBOLICNAME, name);
        return dict;
    }

}
