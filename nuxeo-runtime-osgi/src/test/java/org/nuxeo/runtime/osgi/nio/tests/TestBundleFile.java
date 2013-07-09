package org.nuxeo.runtime.osgi.nio.tests;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.osgi.OSGiBundleFile;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class TestBundleFile {

    protected final Path osgiPath = locateOSGiJar();

    protected final Path testPath = locateTestClasses();

    protected Path locateTestClasses() {
        return new File("target/test-classes").toPath();
    }

    protected Path locateOSGiJar() {
        try {
            return locateJar(Constants.class);
        } catch (URISyntaxException | IOException e) {
            throw new Error("Cannot locate osgi jar", e);
        }
    }

    protected Path locateJar(Class<?> clazz) throws IOException,
            URISyntaxException {
        URI location = clazz.getResource(
                "/".concat(clazz.getName().replace('.', '/')).concat(".class")).toURI();
        String path = location.getSchemeSpecificPart();
        URI jarLocation = URI.create(path.substring(0, path.indexOf('!')));
        return new File(jarLocation).toPath();
    }

    @Test
    public void canFindOSGiFiles() throws BundleException {
        assertCanFindFiles(osgiPath);
    }

    @Test
    public void canFindTestFiles() throws BundleException {
        assertCanFindFiles(testPath);
    }

    protected void assertCanFindFiles(Path path) throws BundleException {
        OSGiBundleFile file = new OSGiBundleFile(path);
        Enumeration<URL> urls = file.findEntries("/", "*", true);
        Assert.assertTrue(file + " has no entries ", urls.hasMoreElements());
    }

}
