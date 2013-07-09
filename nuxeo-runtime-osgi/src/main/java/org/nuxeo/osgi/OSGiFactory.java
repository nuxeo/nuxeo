package org.nuxeo.osgi;

import java.nio.file.Path;

import org.osgi.framework.BundleException;

public interface OSGiFactory {

    OSGiBundleFile newFile(Path path) throws BundleException;

    OSGiManifestReader newReader(OSGiBundleFile file) throws BundleException;

    OSGiBundle newBundle(OSGiBundleFile file) throws BundleException;

    OSGiBundleContext newContext(OSGiBundleHost bundle) throws BundleException;

    OSGiLoader newLoader(OSGiBundleContext context) throws BundleException;
}
