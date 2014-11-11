package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.LogFactory;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.OSGiBundleFile;
import org.nuxeo.osgi.OSGiDefaultFactory;
import org.nuxeo.osgi.OSGiSystemContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiTestLoader {

    protected final OSGiAdapter adapter = newAdapter();

    protected OSGiAdapter newAdapter() {
        Properties env = new Properties();
        env.put(OSGiAdapter.HOME_DIR, "target/nxosgi");
        env.put(OSGiAdapter.BUNDLE_FACTORY,
                OSGiHarnessBundleFactory.class.getName());
        env.put(OSGiAdapter.BOOT_DELEGATION,
                "java,javax,org.osgi,org.nuxeo.osgi,org.junit");
        try {
            OSGiAdapter adapter = new OSGiAdapter(env);
            adapter.initialize();
            installClasspath(adapter);
            return adapter;
        } catch (IOException | BundleException | URISyntaxException cause) {
            throw new Error("Cannot load osgi", cause);
        }
    }

    public static class OSGiHarnessBundleFactory extends OSGiDefaultFactory {

        public OSGiHarnessBundleFactory(OSGiSystemContext osgi) {
            super(osgi);
        }

        @Override
        public OSGiBundleFile newFile(Path path) throws BundleException {
            if ("test-classes".equals(path.getFileName().toString())) {
                Path testPath = path.resolve("META-INF/MANIFEST.MF");
                if (!testPath.toFile().exists()) {
                    return new OSGiTestDirectoryBundleFile(path);
                }
            }
            return super.newFile(path);
        }

    }

    public static class OSGiTestDirectoryBundleFile extends OSGiBundleFile {

        public OSGiTestDirectoryBundleFile(Path path) throws BundleException {
            super(path);
        }

        @Override
        protected Manifest loadManifest() throws IOException {
            Path testPath = rootPath.resolve("META-INF/MANIFEST.MF");
            if (testPath.toFile().exists()) {
                return super.loadManifest();
            }
            Path mainPath = rootPath.resolveSibling("classes").resolve(
                    "META-INF/MANIFEST.MF");
            Manifest mainMF;
            try (InputStream mfInput = new FileInputStream(mainPath.toFile())) {
                mainMF = new Manifest(mfInput);
            }
            Attributes mainAttrs = mainMF.getMainAttributes();
            String symbolicName = mainAttrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
            int optionsIndex = symbolicName.indexOf(';');
            if (optionsIndex > 0) {
                symbolicName = symbolicName.substring(0, optionsIndex);
            }
            String version = mainAttrs.getValue(Constants.BUNDLE_VERSION);
            Manifest mf = new Manifest();
            Attributes attrs = mf.getMainAttributes();
            attrs.putValue(Constants.BUNDLE_SYMBOLICNAME,
                    symbolicName.concat(".m2test"));
            attrs.putValue(Constants.BUNDLE_VERSION, version);
            attrs.putValue(Constants.FRAGMENT_HOST, symbolicName);
            return mf;
        }
    }

    protected Bundle activateBundles(Class<?> clazz,
            Set<Class<?>> activatedClasses, Set<Bundle> activatedBundles,
            AssertionError error) {
        if (clazz == null) {
            return null;
        }
        if (clazz.equals(Object.class)) {
            return null;
        }
        if (clazz.isPrimitive()) {
            return null;
        }
        if (activatedClasses.contains(clazz)) {
            return null;
        }
        if (clazz.isArray()) {
            return activateBundles(clazz.getComponentType(), activatedClasses,
                    activatedBundles, error);
        }

        activatedClasses.add(clazz);

        activateBundles(clazz.getSuperclass(), activatedClasses,
                activatedBundles, error);

        for (Field f : clazz.getDeclaredFields()) {
            activateBundles(f.getType(), activatedClasses, activatedBundles,
                    error);
            for (Annotation a : f.getDeclaredAnnotations()) {
                activateBundles(a.annotationType(), activatedClasses,
                        activatedBundles, error);
            }
        }

        for (Annotation a : clazz.getDeclaredAnnotations()) {
            activateBundles(a.annotationType(), activatedClasses,
                    activatedBundles, error);
        }

        for (Method m : clazz.getDeclaredMethods()) {
            activateBundles(m.getReturnType(), activatedClasses,
                    activatedBundles, error);
            for (Class<?> ptype : m.getParameterTypes()) {
                activateBundles(ptype, activatedClasses, activatedBundles,
                        error);
            }
        }

        String name = clazz.getName();
        String path = "/" + name.replace('.', '/') + ".class";
        URL resource = clazz.getResource(path);
        String protocol = resource.getProtocol();
        String location = resource.getPath();
        location = location.substring(0, location.length() - path.length());
        switch (protocol) {
        case "jar":
            location = location.substring(0, location.length() - 1);
            break;
        case "file":
            location = "file://" + location;
            break;
        default:
            error.addSuppressed(new AssertionError("Unknown protocol in "
                    + resource));
            return null;
        }
        Bundle bundle = adapter.getSytemContext().getBundle(location);
        if (bundle == null) {
            return null;
        }
        if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null
                && (bundle.getState() & (Bundle.ACTIVE)) == 0) {
            try {
                if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) == 0) {
                    bundle.start();
                }
                activatedBundles.add(bundle);
            } catch (BundleException cause) {
                error.addSuppressed(cause);
            }
        }

        return bundle;
    }

    @SuppressWarnings("unchecked")
    protected <T> Class<T> reloadClass(Class<T> clazz) {
        AssertionError error = new AssertionError("Cannot load " + clazz
                + " in osgi");
        Bundle bundle = activateBundles(clazz, new HashSet<Class<?>>(),
                new HashSet<Bundle>(), error);
        if (bundle == null || error.getSuppressed().length > 0) {
            throw error;
        }
        try {
            return (Class<T>) bundle.loadClass(clazz.getName());
        } catch (ClassNotFoundException cause) {
            error.addSuppressed(cause);
            throw error;
        }
    }

    protected Set<URI> scanClasspath() throws IOException, URISyntaxException,
            BundleException {
        ClassLoader classLoader = OSGiTestLoader.class.getClassLoader();
        Set<URI> files = new HashSet<URI>();
        if (classLoader instanceof URLClassLoader) {
            scanURLClasspath(classLoader, files);
        } else if (classLoader.getClass().getName().equals(
                "org.apache.tools.ant.AntClassLoader")) {
            scanAntClasspath(classLoader, files);
        } else {
            LogFactory.getLog(OSGiTestLoader.class).warn(
                    "Unknown classloader type: "
                            + classLoader.getClass().getName()
                            + "\nWon't be able to load OSGI bundles");
            return Collections.emptySet();
        }
        scanSurefireClasspath(files);
        return files;
    }

    protected void installClasspath(OSGiAdapter adapter)
            throws BundleException, IOException, URISyntaxException {
        Set<URI> classpath = scanClasspath();
        for (URI file : classpath) {
            adapter.install(file);
        }
    }

    protected void scanSurefireClasspath(Set<URI> files) throws IOException {
        // special cases such as Surefire with useManifestOnlyJar or Jacoco
        // Look for nuxeo-runtime
        boolean found = false;
        JarFile surefirebooterJar = null;
        for (URI uri : files) {
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
                    for (int i = 0; i < cpe.length; i++) {
                        // Don't need to add 'file:' with maven surefire
                        // >= 2.4.2
                        String newUrl = cpe[i].startsWith("file:") ? cpe[i]
                                : "file:" + cpe[i];
                        files.add(URI.create(newUrl));
                    }
                }
            } catch (Exception e) {
                // skip
            } finally {
                surefirebooterJar.close();
            }
        }
    }

    protected void scanAntClasspath(ClassLoader classLoader, Set<URI> files) {
        String cp;
        try {
            Method method = classLoader.getClass().getMethod("getClasspath");
            cp = (String) method.invoke(classLoader);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new Error("Cannot scan ant classpath", e);
        }
        String[] paths = cp.split(File.pathSeparator);
        for (int i = 0; i < paths.length; i++) {
            files.add(URI.create("file:" + paths[i]));
        }
    }

    protected void scanURLClasspath(ClassLoader classLoader, Set<URI> files)
            throws URISyntaxException {
        URL[] classpath = ((URLClassLoader) classLoader).getURLs();
        for (URL entry : classpath) {
            files.add(entry.toURI());
        }
    }

    public void run(Class<?> classToRun, RunNotifier notifier) {
        try {
            adapter.start();
        } catch (BundleException cause) {
            throw new AssertionError("Cannot startup osgi", cause);
        }
        try {
            classToRun = reloadClass(classToRun);
            Class<? extends Runner> runnerClass = reloadClass(FeaturesRunner.class);
            Runner runner = runnerClass.getConstructor(Class.class).newInstance(classToRun);
            runner.run(notifier);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException cause) {
            throw new AssertionError("Cannot run in osgi " + classToRun, cause);
        } finally {
            try {
                adapter.shutdown();
            } catch (BundleException | IOException cause) {
                throw new AssertionError("Cannot shutdown osgi", cause);
            }
        }
    }

}
