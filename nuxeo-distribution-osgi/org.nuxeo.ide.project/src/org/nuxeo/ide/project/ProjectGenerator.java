/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ide.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ide.project.utils.FileUtils;
import org.nuxeo.ide.project.utils.StringUtils;
import org.nuxeo.ide.project.wiz.ProjectEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProjectGenerator {

    public static final String PLUGIN_PACKAGING = "";
    public static final String FRAGMENT_PACKAGING = "";
    public static final String PROJECT_NAME_SUFFIX = "-osgi";
    public static final String OSGI_DIR = "osgi";

    protected File parentPom;
    protected File parentDir;
    protected PomLoader parentLoader;
    protected Map<String,String> parentVars;

    protected boolean clean;

    public ProjectGenerator(File pom) throws Exception {
        this (pom, false);
    }

    public ProjectGenerator(File pom, boolean clean) throws Exception {
        this.clean = clean;
        this.parentPom = pom;
        this.parentDir = pom.getParentFile();
        parentLoader = new PomLoader(pom);
        if (!"pom".equals(parentLoader.getPackaging())) {
            throw new IllegalArgumentException("Not a parent pom: "+pom);
        }
        parentVars = new HashMap<String, String>();
        parentVars.put("parentVersion", parentLoader.getVersion());
        parentVars.put("parentArtifactId", parentLoader.getArtifactId());
        parentVars.put("parentGroupId", parentLoader.getGroupId());
    }

    public void run() throws Exception {
        List<File> files = parentLoader.getModuleFiles();
        if (files != null) {
            for (File file : files) {
                file = file.getParentFile();
                doRun(file, new File(file, "pom.xml"));
            }
        }
    }

    public void generateProject(ProjectEntry entry) throws Exception {
        File file = entry.getFile().getParentFile();
        doRun(file, new File(file, "pom.xml"));
    }

    protected void doRun(File dir, File pom) throws Exception {
        PomLoader loader = new PomLoader(pom);
        if ("pom".equals(loader.getPackaging())) {
            List<File> files = loader.getModuleFiles();
            if (files != null) {
                for (File file : files) {
                    file = file.getParentFile();
                    doRun(file, new File(file, "pom.xml"));
                }
            }
        } else {
            buildProject(dir, pom);
        }
    }

    protected void buildProject(File dir, File pom) throws Exception {
        File osgi = new File(dir, OSGI_DIR);
        boolean alreadyExists = osgi.isDirectory();
        if (clean && alreadyExists) {
            FileUtils.deleteTree(osgi);
        } else if (alreadyExists) {
            return;
        }
        osgi.mkdirs();
        PomLoader loader = new PomLoader(pom);
        String artifactId = loader.getArtifactId();
        String version = loader.getVersion();
        if (version == null) {
            version = parentVars.get("parentVersion");
        }
        String gropuId = loader.getGroupId();
        if (gropuId == null) {
            gropuId = parentVars.get("parentGroupId");
        }
        Map<String,String> vars = new HashMap<String, String>(parentVars);
        vars.put("artifactId", artifactId);
        vars.put("groupId", loader.getGroupId());
        vars.put("version", version);
        vars.put("name", loader.getProjectName());
        vars.put("description", loader.getProjectDescription());
        vars.put("relativePath", makeRelativePath(parentDir, osgi, parentPom.getName()));

        vars.put("projectName", artifactId+PROJECT_NAME_SUFFIX);
        if (version.endsWith("-SNAPSHOT")) {
            version = version.substring(0, version.length()-"-SNAPSHOT".length()).concat(".qualifier");
        }
        //TODO bundle version is not a template ...
        // we must copy MANIFEST.MF from now and replace the original Bundle-Version
        vars.put("bundleVersion", version);

        // all vars are setup -> start copying and processing templates

        copyTemplate(osgi, "/templates/.project", ".project", vars);
        copyTemplate(osgi, "/templates/.classpath", ".classpath", vars);
        copyTemplate(osgi, "/templates/build.properties", "build.properties", vars);
        copyTemplate(osgi, "/templates/pom.xml", "pom.xml", vars);
        copyManifest(osgi, version);
    }


    protected static String makeRelativePath(File root, File file, String fileName) {
        String p1 = root.getAbsolutePath();
        String p2 = file.getAbsolutePath();
        if (p1.endsWith("/")) {
            p1 = p1.substring(0, p1.length()-2);
        }
        if (p2.endsWith("/")) {
            p2 = p2.substring(0, p2.length()-2);
        }
        if (!p2.startsWith(p1)) {
            throw new IllegalArgumentException("Submodule not inside parent module directory: "+root+" -> "+file);
        }
        String p = p2.substring(p1.length()+1); // the relative path from p1
        String[] ar = StringUtils.split(p, '/', false);
        String[] result = new String[ar.length+1];
        for (int i=0; i<result.length; i++) {
            result[i] = "..";
        }
        result[result.length-1] = fileName;
        return StringUtils.join(result, '/');
    }

    protected static void copyTemplate(File dir, String path, String filePath, Map<String,String> vars) throws IOException {
        URL url = ProjectGenerator.class.getResource(path);
        if (url == null) {
            throw new IllegalArgumentException("Resource nout found: "+path);
        }
        copyTemplate(url, new File(dir, filePath), vars);
    }

    protected void copyManifest(File osgi, String v) throws IOException {
        File mf = FileUtils.getFile(osgi, "META-INF", "MANIFEST.MF");
        mf.getParentFile().mkdirs();
        String content = FileUtils.readFile(FileUtils.getFile(osgi, "..", "src", "main", "resources", "META-INF", "MANIFEST.MF"));
        FileUtils.writeFile(mf, content.replace("0.0.0.SNAPSHOT", v));
    }

    protected static void copyTemplate(URL url, File toFile, Map<String,String> vars) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("Null url");
        }
        toFile.getParentFile().mkdirs();
        InputStream in = url.openStream();
        try {
            String content = FileUtils.read(in);
            content = StringUtils.expandVars(content, vars);
            FileUtils.writeFile(toFile, content);
        } finally {
            in.close();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 && args.length > 2) {
            System.err.println("Usage: ProjectGenerator path_to_parent_pom.");
        }
        new ProjectGenerator(new File(args[0]), true).run();
    }

}
