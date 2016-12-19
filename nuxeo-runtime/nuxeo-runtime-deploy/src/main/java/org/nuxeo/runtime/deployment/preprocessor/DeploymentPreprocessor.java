/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     bstefanescu, jcarsique
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.deployment.preprocessor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.DependencyTree;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContextImpl;
import org.nuxeo.runtime.deployment.preprocessor.template.TemplateContribution;
import org.nuxeo.runtime.deployment.preprocessor.template.TemplateParser;

/**
 * Initializer for the deployment skeleton, taking care of creating templates, aggregating default components before
 * runtime is started.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DeploymentPreprocessor {

    public static final String FRAGMENT_FILE = "OSGI-INF/deployment-fragment.xml";

    public static final String CONTAINER_FILE = "META-INF/nuxeo-preprocessor.xml";

    public static final String CONTAINER_FILE_COMPAT = "OSGI-INF/deployment-container.xml";

    private static final Pattern ARTIFACT_NAME_PATTERN = Pattern.compile("-[0-9]+");

    private static final Log log = LogFactory.getLog(DeploymentPreprocessor.class);

    private final File dir;

    private final XMap xmap;

    private ContainerDescriptor root;

    public DeploymentPreprocessor(File dir) {
        this.dir = dir;
        xmap = new XMap();
        xmap.register(ContainerDescriptor.class);
        xmap.register(FragmentDescriptor.class);
    }

    public ContainerDescriptor getRootContainer() {
        return root;
    }

    public void init() throws IOException {
        root = getDefaultContainer(dir);
        if (root != null) {
            // run container commands
            init(root);
        }
    }

    public void init(File metadata, File[] files) throws IOException {
        if (metadata == null) {
            root = getDefaultContainer(dir);
        } else {
            root = getContainer(dir, metadata);
        }
        if (root != null) {
            root.files = files;
            // run container commands
            init(root);
        }
    }

    protected void init(ContainerDescriptor cd) throws IOException {
        cd.context = new CommandContextImpl(cd.directory);
        initContextProperties(cd.context);
        // run container install instructions if any
        if (cd.install != null) {
            cd.install.setLogger(log);
            log.info("Running custom installation for container: " + cd.name);
            cd.install.exec(cd.context);
        }
        if (cd.files != null) {
            init(cd, cd.files);
        } else {
            // scan directories
            if (cd.directories == null || cd.directories.isEmpty()) {
                init(cd, dir);
            } else {
                for (String dirPath : cd.directories) {
                    init(cd, new File(dir, dirPath));
                }
            }
        }
    }

    protected void initContextProperties(CommandContext ctx) {
        ConfigurationGenerator confGen = new ConfigurationGenerator();
        confGen.init();
        Properties props = confGen.getUserConfig();
        for (String key : props.stringPropertyNames()) {
            ctx.put(key, props.getProperty(key));
        }
    }

    protected void processFile(ContainerDescriptor cd, File file) throws IOException {
        String fileName = file.getName();
        FragmentDescriptor fd = null;
        boolean isBundle = false;
        if (fileName.endsWith("-fragment.xml")) {
            fd = getXMLFragment(file);
        } else if (fileName.endsWith("-fragments.xml")) {
            // we allow declaring multiple fragments in the same file
            // this is useful to deploy libraries
            collectXMLFragments(cd, file);
            return;
        } else if (fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".sar")
                || fileName.endsWith(".rar")) {
            isBundle = true;
            if (file.isDirectory()) {
                fd = getDirectoryFragment(file);
            } else {
                fd = getJARFragment(file);
            }
        }
        // register the fragment if any was found
        if (fd != null) {
            fd.fileName = fileName;
            fd.filePath = getRelativeChildPath(cd.directory.getAbsolutePath(), file.getAbsolutePath());
            cd.fragments.add(fd);
            if (fd.templates != null) {
                for (TemplateDescriptor td : fd.templates.values()) {
                    td.baseDir = file;
                    cd.templates.put(td.name, td);
                }
            }
        } else if (isBundle) {
            // create markers - for compatibility with versions < 5.4
            String name = getSymbolicName(file);
            if (name != null) {
                cd.fragments.add(new FragmentDescriptor(name, true));
            }
        }
    }

    protected String getSymbolicName(File file) {
        Manifest mf = JarUtils.getManifest(file);
        if (mf != null) {
            Attributes attrs = mf.getMainAttributes();
            String id = attrs.getValue("Bundle-SymbolicName");
            if (id != null) {
                int p = id.indexOf(';');
                if (p > -1) { // remove properties part if any
                    id = id.substring(0, p);
                }
                return id;
            }
        }
        return null;
    }

    protected String getJarArtifactName(String name) {
        if (name.endsWith(".jar")) {
            name = name.substring(0, name.length() - 4);
        }
        Matcher m = ARTIFACT_NAME_PATTERN.matcher(name);
        if (m.find()) {
            name = name.substring(0, m.start());
        }
        return name;
    }

    protected void init(ContainerDescriptor cd, File[] files) throws IOException {
        for (File file : files) {
            processFile(cd, file);
        }
    }

    protected void init(ContainerDescriptor cd, File dir) throws IOException {
        log.info("Scanning directory: " + dir.getName());
        if (!dir.exists()) {
            log.warn("Directory doesn't exist: " + dir.getPath());
            return;
        }
        // sort input files in alphabetic order -> this way we are sure we get
        // the same deploying order on all machines.
        File[] files = dir.listFiles();
        Arrays.sort(files);
        init(cd, files);
    }

    public void predeploy() throws IOException {
        if (root != null) {
            predeploy(root);
        }
    }

    protected static String listFragmentDescriptor(FragmentDescriptor fd) {
        return fd.name + " (" + fd.fileName + ")";
    }

    protected static void printInfo(FragmentRegistry fragments) {
        List<DependencyTree.Entry<String, FragmentDescriptor>> entries = fragments.getResolvedEntries();
        StringBuilder buf = new StringBuilder("Preprocessing order: ");
        for (DependencyTree.Entry<String, FragmentDescriptor> entry : entries) {
            FragmentDescriptor fd = entry.get();
            if (fd != null && !fd.isMarker()) {
                buf.append("\n\t");
                buf.append(listFragmentDescriptor(entry.get()));
            }
        }
        log.info(buf);

        StringBuilder errors = new StringBuilder();
        List<DependencyTree.Entry<String, FragmentDescriptor>> missing = fragments.getMissingRequirements();
        for (DependencyTree.Entry<String, FragmentDescriptor> entry : missing) {
            buf = new StringBuilder("Unknown bundle: ");
            buf.append(entry.getKey());
            buf.append(" required by: ");
            boolean first = true;
            for (DependencyTree.Entry<String, FragmentDescriptor> dep : entry.getDependsOnMe()) {
                if (!first) {
                    buf.append(", "); // length 2
                }
                first = false;
                buf.append(listFragmentDescriptor(dep.get()));
            }
            log.error(buf);
            errors.append(buf);
            errors.append("\n");
        }
        for (DependencyTree.Entry<String, FragmentDescriptor> entry : fragments.getPendingEntries()) {
            if (!entry.isRegistered()) {
                continue;
            }
            buf = new StringBuilder("Bundle not preprocessed: ");
            buf.append(listFragmentDescriptor(entry.get()));
            buf.append(" waiting for: ");
            boolean first = true;
            for (DependencyTree.Entry<String, FragmentDescriptor> dep : entry.getWaitsFor()) {
                if (!first) {
                    buf.append(", "); // length 2
                }
                first = false;
                buf.append(dep.getKey());
            }
            log.error(buf);
            errors.append(buf);
            errors.append("\n");
        }
        if (errors.length() != 0) {
            // set system property to log startup errors
            // this is read by AbstractRuntimeService
            System.setProperty("org.nuxeo.runtime.deployment.errors", errors.toString());
        }
    }

    protected static void predeploy(ContainerDescriptor cd) throws IOException {
        // run installer and register contributions for each fragment
        List<DependencyTree.Entry<String, FragmentDescriptor>> entries = cd.fragments.getResolvedEntries();
        printInfo(cd.fragments);
        for (DependencyTree.Entry<String, FragmentDescriptor> entry : entries) {
            FragmentDescriptor fd = entry.get();
            if (fd == null || fd.isMarker()) {
                continue; // should be a marker entry like the "all" one.
            }
            cd.context.put("bundle.fileName", fd.filePath);
            cd.context.put("bundle.shortName", fd.fileName);
            cd.context.put("bundle", fd.name);

            // execute install instructions if any
            if (fd.install != null) {
                fd.install.setLogger(log);
                log.info("Running custom installation for fragment: " + fd.name);
                fd.install.exec(cd.context);
            }

            if (fd.contributions == null) {
                continue; // no contributions
            }

            // get fragment contributions and register them
            for (TemplateContribution tc : fd.contributions) {

                // register template contributions if any
                // get the target template
                TemplateDescriptor td = cd.templates.get(tc.getTemplate());
                if (td != null) {
                    if (td.baseDir == null) {
                        td.baseDir = cd.directory;
                    }
                    if (td.template == null) { // template not yet compiled
                        File file = new File(td.baseDir, td.src);
                        // compile it
                        td.template = TemplateParser.parse(file);
                    }
                } else {
                    log.warn("No template '" + tc.getTemplate() + "' found for deployment fragment:  " + fd.name);
                    continue;
                }
                // get the marker where contribution should be inserted
                td.template.update(tc, cd.context);
            }
        }

        // process and write templates
        // fragments where imported. write down templates
        for (TemplateDescriptor td : cd.templates.values()) {
            if (td.baseDir == null) {
                td.baseDir = cd.directory;
            }
            // if required process the template even if no contributions were
            // made
            if (td.template == null && td.isRequired) {
                // compile the template
                File file = new File(td.baseDir, td.src);
                td.template = TemplateParser.parse(file);
            }
            // process the template
            if (td.template != null) {
                File file = new File(td.baseDir, td.installPath);
                file.getParentFile().mkdirs(); // make sure parents exists
                FileUtils.writeFile(file, td.template.getText());
            }
        }

        // process sub containers if any
        for (ContainerDescriptor subCd : cd.subContainers) {
            predeploy(subCd);
        }
    }

    protected FragmentDescriptor getXMLFragment(File file) throws IOException {
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        FragmentDescriptor fd = (FragmentDescriptor) xmap.load(url);
        if (fd != null && fd.name == null) {
            fd.name = file.getName();
        }
        return fd;
    }

    protected void collectXMLFragments(ContainerDescriptor cd, File file) throws IOException {
        String fileName = file.getName();
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Object[] result = xmap.loadAll(url);
        for (Object entry : result) {
            FragmentDescriptor fd = (FragmentDescriptor) entry;
            assert fd != null;
            if (fd.name == null) {
                log.error("Invalid fragments file: " + file.getName()
                        + ". Fragments declared in a -fragments.xml file must have names.");
            } else {
                cd.fragments.add(fd);
                fd.fileName = fileName;
                fd.filePath = getRelativeChildPath(cd.directory.getAbsolutePath(), file.getAbsolutePath());
            }
        }
    }

    protected void processBundleForCompat(FragmentDescriptor fd, File file) {
        // TODO disable for now the warning
        log.warn("Entering compatibility mode - Please update the deployment-fragment.xml in " + file.getName()
                + " to use new dependency management");
        Manifest mf = JarUtils.getManifest(file);
        if (mf != null) {
            fd.name = file.getName();
            processManifest(fd, fd.name, mf);
        } else {
            throw new RuntimeException("Compat: Fragments without a name must reside in an OSGi bundle");
        }
    }

    protected FragmentDescriptor getDirectoryFragment(File directory) throws IOException {
        FragmentDescriptor fd;
        File file = new File(directory.getAbsolutePath() + '/' + FRAGMENT_FILE);
        if (file.isFile()) {
            URL url;
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            fd = (FragmentDescriptor) xmap.load(url);
        } else {
            return null; // don't need preprocessing
        }
        if (fd.name == null) {
            // fallback on symbolic name
            fd.name = getSymbolicName(directory);
        }
        if (fd.name == null) {
            // fallback on artifact id
            fd.name = getJarArtifactName(directory.getName());
        }
        if (fd.version == 0) { // compat with versions < 5.4
            processBundleForCompat(fd, directory);
        }
        return fd;
    }

    protected FragmentDescriptor getJARFragment(File file) throws IOException {
        FragmentDescriptor fd = null;
        try (JarFile jar = new JarFile(file)) {
            ZipEntry ze = jar.getEntry(FRAGMENT_FILE);
            if (ze != null) {
                try (InputStream in = new BufferedInputStream(jar.getInputStream(ze))) {
                    fd = (FragmentDescriptor) xmap.load(in);
                }
                if (fd.name == null) {
                    // fallback on symbolic name
                    fd.name = getSymbolicName(file);
                }
                if (fd.name == null) {
                    // fallback on artifact id
                    fd.name = getJarArtifactName(file.getName());
                }
                if (fd.version == 0) { // compat with versions < 5.4
                    processBundleForCompat(fd, file);
                }
            }
        }
        return fd;
    }

    protected void processManifest(FragmentDescriptor fd, String fileName, Manifest mf) {
        Attributes attrs = mf.getMainAttributes();
        String id = attrs.getValue("Bundle-SymbolicName");
        int p = id.indexOf(';');
        if (p > -1) { // remove properties part if any
            id = id.substring(0, p);
        }
        fd.name = id;
        if (fd.requires != null && !fd.requires.isEmpty()) {
            throw new RuntimeException(
                    "In compatibility mode you must not use <require> tags for OSGi bundles - use Require-Bundle manifest header instead. Bundle: "
                            + fileName);
        }
        // needed to control start-up order (which differs from
        // Require-Bundle)
        String requires = attrs.getValue("Nuxeo-Require");
        if (requires == null) { // if not specific requirement is met use
                                // Require-Bundle
            requires = attrs.getValue("Require-Bundle");
        }
        if (requires != null) {
            String[] ids = StringUtils.split(requires, ',', true);
            fd.requires = new ArrayList<>(ids.length);
            for (int i = 0; i < ids.length; i++) {
                String rid = ids[i];
                p = rid.indexOf(';');
                if (p > -1) { // remove properties part if any
                    ids[i] = rid.substring(0, p);
                }
                fd.requires.add(ids[i]);
            }
        }

        String requiredBy = attrs.getValue("Nuxeo-RequiredBy");
        if (requiredBy != null) {
            String[] ids = StringUtils.split(requiredBy, ',', true);
            for (int i = 0; i < ids.length; i++) {
                String rid = ids[i];
                p = rid.indexOf(';');
                if (p > -1) { // remove properties part if any
                    ids[i] = rid.substring(0, p);
                }
            }
            fd.requiredBy = ids;
        }

    }

    /**
     * Reads a container fragment metadata file and returns the container descriptor.
     */
    protected ContainerDescriptor getContainer(File home, File file) throws IOException {
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        ContainerDescriptor cd = (ContainerDescriptor) xmap.load(url);
        if (cd != null) {
            cd.directory = home;
            if (cd.name == null) {
                cd.name = home.getName();
            }
        }
        return cd;
    }

    protected ContainerDescriptor getDefaultContainer(File directory) throws IOException {
        File file = new File(directory.getAbsolutePath() + '/' + CONTAINER_FILE);
        if (!file.isFile()) {
            file = new File(directory.getAbsolutePath() + '/' + CONTAINER_FILE_COMPAT);
        }
        ContainerDescriptor cd = null;
        if (file.isFile()) {
            cd = getContainer(directory, file);
        }
        return cd;
    }

    public static String getRelativeChildPath(String parent, String child) {
        // TODO optimize this method
        // fix win32 case
        if (parent.indexOf('\\') > -1) {
            parent = parent.replace('\\', '/');
        }
        if (child.indexOf('\\') > -1) {
            child = child.replace('\\', '/');
        } // end fix win32
        Path parentPath = new Path(parent);
        Path childPath = new Path(child);
        if (parentPath.isPrefixOf(childPath)) {
            return childPath.removeFirstSegments(parentPath.segmentCount()).makeRelative().toString();
        }
        return null;
    }

    /**
     * Run preprocessing in the given home directory and using the given list of bundles. Bundles must be ordered by the
     * caller to have same deployment order on all computers.
     * <p>
     * The metadata file is the metadat file to be used to configure the processor. If null the default location will be
     * used (relative to home): {@link #CONTAINER_FILE}.
     */
    public static void process(File home, File metadata, File[] files) throws IOException {
        DeploymentPreprocessor processor = new DeploymentPreprocessor(home);
        // initialize
        processor.init(metadata, files);
        // run preprocessor
        processor.predeploy();
    }

    public static void main(String[] args) throws IOException {
        File root;
        if (args.length > 0) {
            root = new File(args[0]);
        } else {
            root = new File(".");
        }
        System.out.println("Preprocessing: " + root);
        DeploymentPreprocessor processor = new DeploymentPreprocessor(root);
        // initialize
        processor.init();
        // and predeploy
        processor.predeploy();
        System.out.println("Done.");
    }

}
