package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.application.StandaloneBundleLoader;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.RootRuntimeBundle;
import org.nuxeo.runtime.test.TestRuntime;
import org.osgi.framework.Bundle;

public class TestRuntimeHarness {

    private static final Log log = LogFactory.getLog(TestRuntimeHarness.class);

    protected RuntimeService runtime;

    protected List<URL> urls = new ArrayList<URL>();

    private File workingDir;

    private static int counter = 0;

    private StandaloneBundleLoader bundleLoader;

    private Set<URI> readUris;

    private Map<String, BundleFile> bundles;



    public enum RepoType {
        JCR, SQL
    }

    private final RepoType defaultRepoType = RepoType.JCR;
    private final RepoType repoType;

    private static DatabaseHelper dbHelper = DatabaseHelper.DATABASE;


    private ClassLoader classLoader = TestRuntimeHarness.class.getClassLoader();

    public TestRuntimeHarness() {
        super();
        repoType = defaultRepoType;
    }

    public TestRuntimeHarness(RepoType repoType) {
        this.repoType = repoType;
    }

    /**
     * Compatibility version for old runtime harness
     * @deprecated
     * @throws Exception
     */
    public void start() throws Exception {
        this.start(true);
    }

    /**
     *	Starts nuxeo runtime
     * @param startCore wether to start nuxeo core stuff (compat)
     * @throws Exception
     */
    void start(boolean startCore) throws Exception {
        wipeRuntime();
        initUrls();
        if (urls == null) {
            initTestRuntime();
        } else {
            initOsgiRuntime();
        }
        Environment.setDefault(new Environment(getWorkingDir()));
        if(startCore) {
            deployBundle("org.nuxeo.ecm.core.schema");
            deployBundle("org.nuxeo.ecm.core.api");
            deployBundle("org.nuxeo.ecm.core.event");
            deployBundle("org.nuxeo.ecm.core");
            if(repoType == RepoType.JCR)  {
                deployBundle("org.nuxeo.ecm.core.jcr");
                deployBundle("org.nuxeo.ecm.core.jcr-connector");
            } else if( repoType == RepoType.SQL) {
                deployBundle("org.nuxeo.ecm.core.storage.sql");
                deployContrib("org.nuxeo.ecm.core.storage.sql.test", dbHelper.getDeploymentContrib());
                dbHelper.setUp();


                if(dbHelper instanceof DatabasePostgreSQL) {
                    Class.forName("org.postgresql.Driver");
                    String url = String.format("jdbc:postgresql://%s:%s/%s", "localhost",
                            "5432", "nuxeojunittests");
                    Connection connection = DriverManager.getConnection(url, "postgres",
                            "");
                    Statement st = connection.createStatement();
                    String sql = "CREATE LANGUAGE plpgsql";
                    log.debug(sql);
                    st.execute(sql);
                    st.close();
                    connection.close();
                }


            }
        }



    }



    public void stop() throws Exception {
        wipeRuntime();
        if (workingDir != null) {
            FileUtils.deleteTree(workingDir);
        }
        readUris = null;
        bundles = null;
    }

    private static synchronized String generateId() {
        long stamp = System.currentTimeMillis();
        counter++;
        return Long.toHexString(stamp) + '-'
                + System.identityHashCode(System.class) + '.' + counter;
    }

    protected void initOsgiRuntime() throws Exception {
        try {
            workingDir = File.createTempFile("NXOSGITestFramework",
                    generateId());
            workingDir.delete();
        } catch (IOException e) {
            log.error("Could not init working directory", e);
            throw e;
        }
        OSGiAdapter osgi = new OSGiAdapter(workingDir);
        bundleLoader = new StandaloneBundleLoader(osgi, classLoader);
        Thread.currentThread().setContextClassLoader(
                bundleLoader.getSharedClassLoader().getLoader());

        bundleLoader.setScanForNestedJARs(false); // for now
        bundleLoader.setExtractNestedJARs(false);

        BundleFile bundleFile = lookupBundle("org.nuxeo.runtime");
        Bundle bundle = new RootRuntimeBundle(osgi, bundleFile, bundleLoader
                .getClass().getClassLoader(), true);
        bundle.start();
        runtime = Framework.getRuntime();
        assertNotNull(runtime);

        deployContrib(bundleFile, "OSGI-INF/DeploymentService.xml");
        deployContrib(bundleFile, "OSGI-INF/LoginComponent.xml");
        deployContrib(bundleFile, "OSGI-INF/ServiceManagement.xml");
        deployContrib(bundleFile, "OSGI-INF/EventService.xml");
        deployContrib(bundleFile, "OSGI-INF/DefaultJBossBindings.xml");
    }

    protected void initTestRuntime() throws Exception {
        runtime = new TestRuntime();
        Framework.initialize(runtime);
        deployContrib("org.nuxeo.runtime.test", "EventService.xml");
        deployContrib("org.nuxeo.runtime.test", "DeploymentService.xml");
        deployBundle("org.nuxeo.ecm.core.jcr-connector");
    }

    protected void initUrls() {
        if (!(classLoader instanceof URLClassLoader)) {
            log.warn("Unknow classloader type: "
                    + classLoader.getClass().getName()
                    + "\nWon't be able to load OSGI bundles");
            return;
        }
        urls.addAll(Arrays.asList(((URLClassLoader) classLoader).getURLs()));
        // special case for maven surefire with useManifestOnlyJar
        if (urls.size() == 1) {
            urls.addAll(Arrays.asList(loadUrlsFromSurefireBooter(urls.get(0))));
        }
        log.debug(listUrls().toString());
        readUris = new HashSet<URI>();
        bundles = new HashMap<String, BundleFile>();
    }

    private URL[] loadUrlsFromSurefireBooter(URL url) {
        try {
            URI uri = url.toURI();
            if (uri.getScheme().equals("file")
                    && uri.getPath().contains("surefirebooter")) {
                JarFile jar = new JarFile(new File(uri));
                try {
                    String cp = jar.getManifest().getMainAttributes().getValue(
                            Attributes.Name.CLASS_PATH);
                    if (cp != null) {
                        String[] cpe = cp.split(" ");
                        URL[] newUrls = new URL[cpe.length];
                        for (int i = 0; i < cpe.length; i++) {
                            newUrls[i] = new URL(cpe[i]);
                        }
                        return newUrls;
                    }
                } finally {
                    jar.close();
                }
            }
        } catch (Exception e) {
            // skip
        }
        return new URL[0];
    }

    private StringBuilder listUrls() {
        StringBuilder sb = new StringBuilder();
        sb.append("URLs on the classpath: ");
        for (URL url : urls) {
            sb.append(url.toString());
            sb.append('\n');
        }
        return sb;
    }

    /**
     * Makes sure there is no previous runtime hanging around.
     * <p>
     * This happens for instance if a previous test had errors in its
     * <code>setUp()</code>, because <code>tearDown()</code> has not been
     * called.
     *
     * @throws Exception
     */
    protected void wipeRuntime() throws Exception {
        // Make sure there is no active runtime (this might happen if an
        // exception is raised during a previous setUp -> tearDown is not called
        // afterwards).
        runtime = null;
        if (Framework.getRuntime() != null) {
            Framework.shutdown();
        }
    }

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    protected void deployContrib(URL url) {
        assertEquals(runtime, Framework.getRuntime());
        log.info("Deploying contribution from " + url.toString());
        try {
            runtime.getContext().deploy(url);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to deploy contrib " + url.toString());
        }
    }

    protected void deployContrib(BundleFile bundleFile, String contrib) {
        URL url = bundleFile.getEntry(contrib);
        if (url == null) {
            fail(String.format("Could not find entry %s in bundle '%s",
                    contrib, bundleFile.getURL()));
        }
        deployContrib(url);
    }

    /**
     * Deploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
   * deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
   * </code>
     * <p>
     * For compatibility reasons the name of the bundle may be a jar name, but
     * this use is discouraged and deprecated.
     *
     * @param bundle
     *            the name of the bundle to peek the contrib in
     * @param contrib
     *            the path to contrib in the bundle.
     * @throws Exception
     */
    public void deployContrib(String bundle, String contrib) throws Exception {
        deployContrib(lookupBundle(bundle), contrib);
    }

    /**
     * Undeploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
   * undeployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
   * </code>
     *
     * @param bundle
     *            the bundle
     * @param contrib
     *            the contribution
     * @throws Exception
     */
    public void undeployContrib(String bundle, String contrib) throws Exception {
        BundleFile b = lookupBundle(bundle);
        URL url = b.getEntry(contrib);
        if (url == null) {
            fail(String.format("Could not find entry %s in bundle '%s'",
                    contrib, b.getURL()));
        }
        runtime.getContext().undeploy(url);
    }

    protected static boolean isVersionSuffix(String s) {
        if (s.length() == 0) {
            return true;
        }
        return s.matches("-(\\d+\\.?)+(-SNAPSHOT)?(\\.\\w+)?");
    }

    /**
     * Deploys a whole OSGI bundle.
     * <p>
     * The lookup is first done on symbolic name, as set in
     * <code>MANIFEST.MF</code> and then falls back to the bundle url (e.g.,
     * <code>nuxeo-platform-search-api</code>) for backwards compatibility.
     *
     * @param bundle
     *            the symbolic name
     * @throws Exception
     */
    public void deployBundle(String bundle) throws Exception {
        BundleFile bundleFile = lookupBundle(bundle);
        bundleLoader.loadBundle(bundleFile);
        bundleLoader.installBundle(bundleFile);
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

    protected BundleFile lookupBundle(String bundleName) throws Exception {
        BundleFile bundleFile = bundles.get(bundleName);
        if (bundleFile != null) {
            return bundleFile;
        }
        for (URL url : urls) {
            URI uri = url.toURI();
            if (readUris.contains(uri)) {
                continue;
            }
            File file = new File(uri);
            readUris.add(uri);
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
                log.debug(String.format("Bundle '%s' has URL %s", symbolicName,
                        url));
                bundles.put(symbolicName, bundleFile);
            }
            if (bundleName.equals(symbolicName)) {
                return bundleFile;
            }
        }
        throw new IllegalArgumentException(String.format(
                "No bundle with symbolic name '%s'", bundleName));
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public void loadProperties(InputStream stream) throws IOException {
        ((OSGiRuntimeService) runtime).loadProperties(stream);
    }

    /**
     * Add a single jar file to this harness's urls searched for when loading
     * bundles.
     *
     * @param jar
     * @throws MalformedURLException
     */
    public void addJar(File jar) throws MalformedURLException {
        urls.add(jar.toURL());
    }

    /**
     * Adds a resource to some directory in this runtime's working directory.
     *
     * @param resourcePath
     *            path to the resource, resolved through current thread's class
     *            loader.
     * @param targetDir
     *            target directory relative to this harness working directory.
     * @param targetFile
     *            TODO
     * @throws IOException
     */
    public void copyFileFromResource(String resourcePath, String targetDir,
            String targetFile) throws IOException {
        File dstDir = new File(getWorkingDir(), targetDir);
        if (!dstDir.exists() && !dstDir.mkdirs())
            throw new IOException("Cannot create target directory " + targetDir);
        InputStream srcConfig = getClass().getResource(resourcePath)
                .openStream();
        File destFile = new File(dstDir, targetFile);
        FileOutputStream dstConfig = new FileOutputStream(destFile);
        FileUtils.copy(srcConfig, dstConfig);
    }

    /**
     * Copy a resource to the configuration directory of the runtime
     *
     * @param resourcePath
     *            path to the resource, resolved through current thread's class
     *            loader.
     * @throws IOException
     */
    public void addConfigurationFromResource(String resourcePath)
            throws IOException {
        copyFileFromResource(resourcePath, "config", FileUtils
                .getFileName(resourcePath));
    }

    public boolean isStarted() {
        return runtime != null;
    }
}
