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
package org.nuxeo.build.osgi.gen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProjectGenerator {

    public static final String PROJECT_NAME_SUFFIX = "-osgi";

    public File root;

    public File pom;

    /**
     * Source project root
     */
    public File src;

    public File osgiRoot;

    public File parentPom;

    public String pathToParentPom; // path in the form ../../..
    public String pathToSrc; // path in the form ../../..

    public PomLoader loader;


    public ProjectGenerator(File nuxeoRoot, File parentPom, File osgiRoot, File root) throws Exception {
        this.osgiRoot = osgiRoot.getCanonicalFile();
        this.root = root.getCanonicalFile();
        this.pom = new File(root, "pom.xml");
        this.parentPom = parentPom.getCanonicalFile();
        String rpath = getRelativePath(osgiRoot, root);
        this.src = new File(nuxeoRoot, rpath).getCanonicalFile();
        String pathToNuxeo = getDescendingRelativePath(nuxeoRoot, root);
        this.pathToSrc = pathToNuxeo+File.separator+rpath;
        this.pathToParentPom = getDescendingRelativePath(this.parentPom.getParentFile().getCanonicalFile(), root)+File.separator+parentPom.getName();
        loader = new PomLoader(new File(src, "pom.xml"));
    }

    public String pathToSrcFile(String ... segments) {
        StringBuilder buf = new StringBuilder(pathToSrc);
        for (String segment : segments) {
            buf.append(File.separator).append(segment);
        }
        return buf.toString();
    }

    public static String getRelativePath(File parent, File file) {
        String p1 = parent.getAbsolutePath();
        String p2 = file.getAbsolutePath();
        if (p1.endsWith(File.separator)) {
            p1 = p1.substring(0, p1.length()-1);
        }
        if (p2.endsWith(File.separator)) {
            p2 = p2.substring(0, p2.length()-1);
        }
        if (!p2.startsWith(p1)) {
            throw new IllegalArgumentException("Invalid path "+file+". Not a sub path of "+parent);
        }
        return p2.substring(p1.length()+1);
    }


    public static String getDescendingRelativePath(File parent, File file) {
        String path = getRelativePath(parent, file);
        String[] ar = StringUtils.split(path, File.separatorChar, false);
        if (ar.length == 0) {
            return ".";
        }
        if (ar.length == 1) {
            return "..";
        }
        StringBuilder buf = new StringBuilder();
        buf.append("..");
        for (int i=1; i<ar.length; i++) {
            buf.append(File.separator).append("..");
        }
        return buf.toString();
    }

    public String getEclipseLinkPrefix() {
        int p = pathToSrc.lastIndexOf("..");
        if (p == -1) {
            throw new IllegalArgumentException("BUG? invalid pathToSrc: "+pathToSrc);
        }
        String prefix = pathToSrc.substring(0, p+2);
        String path = pathToSrc.substring(p+2);
        String[] ar = StringUtils.split(prefix, File.separatorChar, false);
        return "PARENT-"+ar.length+"-PROJECT_LOC"+path+File.separator;
    }

    public File getSourceManifest() throws IOException {
        return new File(src, "src"+File.separator+"main"+File.separator+"resources"+File.separator+"META-INF"+File.separator+"MANIFEST.MF").getCanonicalFile();
    }

    public File getManifest() throws IOException {
        return new File(root, "META-INF"+File.separator+"MANIFEST.MF").getCanonicalFile();
    }

    public void generate(Map<String,String> parentVars, boolean clean) {
        try {
            if (clean && root.isDirectory()) {
                FileUtils.deleteTree(root);
            }
            doGenerate(parentVars);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Failed to generate project: "+root);
        }
    }

    public void doGenerate(Map<String,String> parentVars) throws Exception {
        root.mkdirs();
        Map<String,String> vars = new HashMap<String, String>(parentVars);
        String artifactId = loader.getArtifactId();
        String version = loader.getVersion();
        if (version == null) {
            version = parentVars.get("parentVersion");
        }
        String groupId = loader.getGroupId();
        if (groupId == null) {
            groupId = parentVars.get("parentGroupId");
        }
        vars.put("artifactId", artifactId);
        vars.put("groupId", groupId);
        vars.put("version", version);
        vars.put("name", loader.getProjectName());
        vars.put("description", loader.getProjectDescription());
        vars.put("pathToParentPom", pathToParentPom);
        vars.put("pathToSrc", pathToSrc);

        vars.put("projectName", artifactId+PROJECT_NAME_SUFFIX);
        if (version.endsWith("-SNAPSHOT")) {
            version = version.substring(0, version.length()-"-SNAPSHOT".length()).concat(".qualifier");
        }
        //TODO bundle version is not a template ...
        // we must copy MANIFEST.MF from now and replace the original Bundle-Version
        vars.put("bundleVersion", version);

        vars.put("javaPath", pathToSrcFile("src", "main", "java"));
        vars.put("resourcesPath", pathToSrcFile("src", "main", "resources"));

        String prefix = getEclipseLinkPrefix();
        vars.put("javaLink", prefix+"src"+File.separator+"main"+File.separator+"java");
        vars.put("resourcesLink", prefix+"src"+File.separator+"main"+File.separator+"resources");
        // all vars are setup -> start copying and processing templates

        copyTemplate(root, "/templates/.project", ".project", vars);
        copyTemplate(root, "/templates/.classpath", ".classpath", vars);
        copyTemplate(root, "/templates/build.properties", "build.properties", vars);
        copyTemplate(root, "/templates/pom.xml", "pom.xml", vars);
        copyManifest(getSourceManifest(), getManifest(), version);
    }

    protected static void copyTemplate(File dir, String path, String filePath, Map<String,String> vars) throws IOException {
        URL url = ProjectGenerator.class.getResource(path);
        if (url == null) {
            throw new IllegalArgumentException("Resource nout found: "+path);
        }
        copyTemplate(url, new File(dir, filePath), vars);
    }

    protected void copyManifest(File src, File mf, String v) throws IOException {
        mf.getParentFile().mkdirs();
        String content = FileUtils.readFile(src);
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

    public static void generate(File nuxeoRoot, File pom, File osgiRoot, boolean clean) throws Exception {
        nuxeoRoot = nuxeoRoot.getCanonicalFile();
        pom = pom.getCanonicalFile();
        osgiRoot = osgiRoot.getCanonicalFile();
        System.out.println("====== Generate PDE projects ======");
        System.out.println("Nuxeo Root: "+nuxeoRoot);
        System.out.println("Parent POM: "+pom);
        System.out.println("PDE Projects Root: "+osgiRoot);
        System.out.println("===================================");
        PomLoader loader = new PomLoader(pom);
        HashMap<String, String> vars = new HashMap<String, String>();
        vars.put("parentVersion", loader.getVersion());
        vars.put("parentArtifactId", loader.getArtifactId());
        vars.put("parentGroupId", loader.getGroupId());
        for (File root : loader.getModuleFiles()) {
            System.out.println("Generating " + root);
            new ProjectGenerator(nuxeoRoot, pom, osgiRoot, root).generate(vars, clean);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 && args.length > 4) {
            System.err.println("Usage: ProjectGenerator [-clean] nuxeoRoot pom osgiRoot");
            return;
        }
        boolean clean = false;
        String nuxeoRoot = null;
        String pom = null;
        String osgiRoot = null;
        if (args.length == 4) {
            if (!"-clean".equals(args[0])) {
                System.err.println("Usage: ProjectGenerator [-clean] nuxeoRoot pom osgiRoot");
                return;
            }
            clean = true;
            nuxeoRoot = args[1];
            pom = args[2];
            osgiRoot = args[3];
        } else {
            nuxeoRoot = args[0];
            pom = args[1];
            osgiRoot = args[2];
        }

        generate(new File(nuxeoRoot), new File(pom), new File(osgiRoot), clean);
    }

}
