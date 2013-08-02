package org.nuxeo.osgi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.nio.CompoundExceptionBuilder;
import org.nuxeo.osgi.nio.FilterBuilder;
import org.nuxeo.osgi.nio.RecursiveDirectoryStream;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiBundleFile {

    protected Log log = LogFactory.getLog(OSGiBundleFile.class);

    protected final Path path;

    protected final Path rootPath;

    protected final Manifest mf;

    public OSGiBundleFile(Path path) throws BundleException {
        this(path, rootPath(path));
    }

    protected static Path rootPath(Path path) throws BundleException {
        if (path.toFile().isDirectory()) {
            return path;
        }
        try {
            return FileSystems.newFileSystem(path,
                    OSGiBundleFile.class.getClassLoader()).getPath("/");
        } catch (IOException e) {
            throw new BundleException("Cannot get access to root of " + path, e);
        }
    }

    protected OSGiBundleFile(Path file, Path root) throws BundleException {
        path = file.normalize();
        rootPath = root.normalize();
        try {
            mf = loadManifest();
        } catch (IOException e) {
            throw new BundleException("Cannot load manifest from ["
                    + root.getFileSystem() + "," + root + "]", e);
        }
    }

    protected Manifest loadManifest() throws IOException {
        Path mfPath = rootPath.resolve("META-INF/MANIFEST.MF");
        try (InputStream input = Files.newInputStream(mfPath,
                StandardOpenOption.READ);) {
            return new Manifest(input);
        } catch (FileNotFoundException | NoSuchFileException error) {
            return new Manifest();
        }
    }

    public String getSymbolicName() {
        return mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public String getLocation() {
        return path.toUri().toASCIIString();
    }

    public URL getURL() {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Cannot get " + rootPath
                    + " location");
        }
    }

    public File getFile() {
        return rootPath.toFile();
    }

    public Manifest getManifest() {
        return mf;
    }

    public URL getEntry(String name) {
        Path path = rootPath.resolve(name);
        try {
            URL location = path.toUri().toURL();
            try {
                location.openStream().close();
            } catch (IOException e) {
                return null;
            }
            return location;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid file " + rootPath, e);
        }
    }

    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException(
                "The operation BundleFile.geEntryPaths() is not yet implemented");
    }

    public Enumeration<URL> findEntries(final String name, final String pattern,
            boolean recurse) {

        final Path path = "/".equals(name) ? rootPath : rootPath.resolve(name);

        return new Enumeration<URL>() {
            final DirectoryStream.Filter<Path> filter = new FilterBuilder<Path>(
                    rootPath.getFileSystem()).newFilter(pattern);

            final RecursiveDirectoryStream<Path> dirStream = new RecursiveDirectoryStream<Path>(
                    path, filter);

            final Iterator<Path> it = dirStream.iterator();

            @Override
            public boolean hasMoreElements() {
                boolean hasNext = it.hasNext();
                if (hasNext == false) {
                    try {
                        dirStream.close();
                    } catch (IOException e) {
                        ;
                    }
                    return false;
                }
                return true;
            }

            @Override
            public URL nextElement() {
                Path path = it.next();
                try {
                    return path.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Cannot convert " + path
                            + " to URL", e);
                }
            }
        };
    }

    public OSGiBundleFile[] getNestedBundles(OSGiSystemContext osgi, File tmpDir)
            throws BundleException {
        Attributes attrs = mf.getMainAttributes();
        String cp = attrs.getValue(Constants.BUNDLE_CLASSPATH);
        if (cp == null) {
            return new OSGiBundleFile[0];
        }
        String[] paths = StringUtils.split(cp, ',');
        List<OSGiBundleFile> files = new ArrayList<OSGiBundleFile>(paths.length);
        CompoundExceptionBuilder<BundleException> errors = new OSGiCompoundBundleExceptionBuilder();
        for (int i = 0; i < paths.length; ++i) {
            Path path = rootPath.resolve(paths[i].trim()).normalize();
            if (rootPath.equals(path)) {
                continue; // not a nested bundle
            }
            try {
                files.add(osgi.factory.newFile(path));
            } catch (BundleException e) {
                errors.add(e);
            }
        }
        errors.throwOnError();
        return files.toArray(new OSGiBundleFile[files.size()]);
    }

    public OSGiBundleFile[] findNestedBundles(OSGiSystemContext osgi,
            File tmpDir) throws BundleException {
        Set<OSGiBundleFile> files = new HashSet<OSGiBundleFile>();
        CompoundExceptionBuilder<BundleException> errors = new OSGiCompoundBundleExceptionBuilder();
        try (RecursiveDirectoryStream<Path> stream = new RecursiveDirectoryStream<Path>(
                rootPath, null)) {
            for (Path path : stream) {
                try {
                    files.add(osgi.factory.newFile(path));
                } catch (BundleException e) {
                    errors.add(e);
                }
            }
        } catch (IOException e) {
            throw new BundleException("Cannot find nested bundle of "
                    + rootPath);
        }
        return files.toArray(new OSGiBundleFile[files.size()]);
    }

    @Override
    public String toString() {
        return "OSGiBundleFile [fs=" + rootPath.getFileSystem() + ",root="
                + rootPath + "]";
    }

}
