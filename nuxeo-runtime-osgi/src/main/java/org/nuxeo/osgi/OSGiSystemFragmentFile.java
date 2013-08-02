package org.nuxeo.osgi;

import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.BundleException;

public class OSGiSystemFragmentFile extends OSGiBundleFile {

    public OSGiSystemFragmentFile(Path path) throws BundleException {
        super(path);
    }

    @Override
    public Manifest loadManifest() {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Bundle-SymbolicName", "org.nuxeo.osgi.config");
        attrs.putValue("Bundle-Name", "Nuxeo App System Working Fragment");
        attrs.putValue("Bundle-Vendor", "Nuxeo");
        attrs.putValue("Bundle-Version", "1.0.0");
        attrs.putValue("Fragment-Host", "org.nuxeo.osgi");
        return mf;
    }
}
