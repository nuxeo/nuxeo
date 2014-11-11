package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class ClasspathScanner {

    public ClasspathScanner() throws Exception {
        super();
    }

    protected final URL urls[] = introspect();

    protected URL[] introspectClasspath(ClassLoader loader) {
        // normal case
        if (loader instanceof URLClassLoader) {
            return ((URLClassLoader) loader).getURLs();
        }
        // surefire suite runner
        final Class<? extends ClassLoader> loaderClass = loader.getClass();
        if (loaderClass.getName().equals("org.apache.tools.ant.AntClassLoader")) {
            try {
                Method method = loaderClass.getMethod("getClasspath");
                String cp = (String) method.invoke(loader);
                String[] paths = cp.split(File.pathSeparator);
                URL[] urls = new URL[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    urls[i] = new URL("file:" + paths[i]);
                }
                return urls;
            } catch (NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | MalformedURLException cause) {
                throw new AssertionError(
                        "Cannot introspect mavent class loader", cause);
            }
        }
        // try getURLs method
        try {
            Method m = loaderClass.getMethod("getURLs");
            return (URL[]) m.invoke(loader);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException cause) {
            throw new AssertionError("Unsupported classloader type: "
                    + loaderClass.getName()
                    + "\nWon't be able to load OSGI bundles");
        }
    }

    public URL[] introspect() throws Exception {
        ClassLoader classLoader = NXRuntimeTestCase.class.getClassLoader();
        URL[] urls = introspectClasspath(classLoader);
        // special cases such as Surefire with useManifestOnlyJar or Jacoco
        // Look for nuxeo-runtime
        boolean found = false;
        JarFile surefirebooterJar = null;
        for (URL url : urls) {
            URI uri = url.toURI();
            if (uri.getPath().matches(".*/nuxeo-runtime-[^/]*\\.jar")) {
                found = true;
                break;
            } else if (uri.getScheme().equals("file")
                    && uri.getPath().contains("surefirebooter")) {
                surefirebooterJar = new JarFile(new File(uri));
            }
        }
        if (!found && surefirebooterJar != null) {
            try {
                String cp = surefirebooterJar.getManifest().getMainAttributes().getValue(
                        Attributes.Name.CLASS_PATH);
                if (cp != null) {
                    String[] cpe = cp.split(" ");
                    URL[] newUrls = new URL[cpe.length];
                    for (int i = 0; i < cpe.length; i++) {
                        // Don't need to add 'file:' with maven surefire
                        // >= 2.4.2
                        String newUrl = cpe[i].startsWith("file:") ? cpe[i]
                                : "file:" + cpe[i];
                        newUrls[i] = new URL(newUrl);
                    }
                    urls = newUrls;
                }
            } catch (Exception e) {
                // skip
            } finally {
                surefirebooterJar.close();
            }
        }
        final Log log = LogFactory.getLog(ClasspathScanner.class);
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("URLs on the classpath: ");
            for (URL url : urls) {
                sb.append(url.toString());
                sb.append('\n');
            }
            log.debug(sb.toString());
        }
        return urls;
    }

    protected final Set<URI> readUris = new HashSet<URI>();

    public BundleFile lookup(String name)  {
        for (URL url : urls) {
            URI uri = null;
            try {
                uri = url.toURI();
            } catch (URISyntaxException cause) {
                throw new RuntimeException("Cannot access to " + url, cause);
            }
            if (readUris.contains(uri)) {
                continue;
            }
            File file = new File(uri);
            BundleFile bundleFile;
            try {
                if (file.isDirectory()) {
                    bundleFile = new DirectoryBundleFile(file);
                } else {
                    bundleFile = new JarBundleFile(file);
                }
            } catch (IOException e) {
                // no manifest => not a bundle
                continue;
            }
            String symbolicName = readSymbolicName(bundleFile);
            if (symbolicName != null) {
                LogFactory.getLog(ClasspathScanner.class).info(String.format("Bundle '%s' has URL %s", symbolicName,
                        url));
            }
            if (name.equals(symbolicName)) {
                readUris.add(uri);
                return bundleFile;
            }
        }
        throw new IllegalArgumentException("No such bundle with " + name);
    }

    protected String readSymbolicName(BundleFile bf) {
        Manifest manifest = bf.getManifest();
        if (manifest == null) {
            return null;
        }
        Attributes attrs = manifest.getMainAttributes();
        String name = attrs.getValue("Bundle-SymbolicName");
        if (name == null) {
            return null;
        }
        String[] sp = name.split(";", 2);
        return sp[0];
    }

}
