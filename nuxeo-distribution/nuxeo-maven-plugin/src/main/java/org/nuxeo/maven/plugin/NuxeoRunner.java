package org.nuxeo.maven.plugin;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.nuxeo.osgi.BundleManifestReader;
import org.nuxeo.osgi.application.loader.FrameworkLoader;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

public class NuxeoRunner implements Runnable {

    protected static Attributes.Name NUXEO_COMPONENT_NAME = new Attributes.Name(BundleManifestReader.COMPONENT_HEADER);

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        ClassLoader classLoader = thread.getContextClassLoader();
        try {
            File workingDir = Files.createTempDirectory(Paths.get("target"), "nxrunner-").toFile();
            Class<?> loaderClass = classLoader.loadClass(FrameworkLoader.class.getName());
            Method initialize = loaderClass.getDeclaredMethod("initialize", ClassLoader.class, File.class, List.class,
                    Map.class);
            List<File> bundles = new FastClasspathScanner().getUniqueClasspathElements()
                                                           .stream()
                                                           .filter(this::isNuxeoBundle)
                                                           .collect(toList());
            initialize.invoke(null, classLoader, workingDir, bundles, Collections.emptyMap());

            Method start = loaderClass.getDeclaredMethod("start");
            start.invoke(null);
        } catch (Exception e) {
            thread.getThreadGroup().uncaughtException(thread, e);
        }
    }

    protected boolean isNuxeoBundle(File f) {
        Manifest mf;
        try {
            if (f.isFile()) { // jar file
                try (JarFile jf = new JarFile(f)) {
                    mf = jf.getManifest();
                }
                if (mf == null) {
                    return false;
                }
            } else if (f.isDirectory()) { // directory
                f = new File(f, "META-INF/MANIFEST.MF");
                if (!f.isFile()) {
                    return false;
                }
                mf = new Manifest();
                try (FileInputStream input = new FileInputStream(f)) {
                    mf.read(input);
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return mf.getMainAttributes().containsKey(NUXEO_COMPONENT_NAME);
    }

}
