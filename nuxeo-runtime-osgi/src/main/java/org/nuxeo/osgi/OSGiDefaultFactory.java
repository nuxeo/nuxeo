package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.jar.Manifest;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiDefaultFactory implements OSGiFactory {

    protected final OSGiSystemContext osgi;

    public OSGiDefaultFactory(OSGiSystemContext osgi) {
       this.osgi = osgi;
    }

    @Override
    public OSGiManifestReader newReader(OSGiBundleFile file)
            throws BundleException {
        return new OSGiManifestReader(file);
    }

    @Override
    public OSGiBundleFile newFile(Path path) throws BundleException {
        File file = path.toFile();
        if (file.isDirectory()) {
            return new OSGiBundleFile(path);
        }
        try {
            return new OSGiBundleFile(path, FileSystems.newFileSystem(path,
                    OSGiBundleFile.class.getClassLoader()).getPath("/"));
        } catch (IOException e) {
            throw new BundleException("Cannot create bundle file for " + path,
                    e);
        }
    }

    @Override
    public OSGiBundle newBundle(OSGiBundleFile file) throws BundleException {
        Manifest mf = file.getManifest();
        String host = mf.getMainAttributes().getValue(Constants.FRAGMENT_HOST);
        if (host != null) {
            return new OSGiBundleFragment(file);
        }
        String name = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        if (name != null) {
            return new OSGiBundleHost(file);
        }
        return new OSGiBundleLibrary(file);
    }

    @Override
    public OSGiBundleContext newContext(OSGiBundleHost host) throws BundleException {
        if (host.equals(osgi.bundle) ) {
            return host.osgi;
        }
        return new OSGiBundleContext(osgi, host);
    }

    @Override
    public OSGiLoader newLoader(OSGiBundleContext context)
            throws BundleException {
        if (context.equals(osgi) ) {
            return new OSGiLoader(osgi, OSGiSystemContext.class.getClassLoader());
        }
        return new OSGiLoader(context, context.bundle.osgi.loader);
    }

}
