package org.nuxeo.runtime.tomcat.adapter.tests.metaindex;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/*
 * JarMetaIndex associates the jar file with a set of what so called
 * "meta-index" of the jar file. Essentially, the meta-index is a list
 * of class prefixes and the plain files contained in META-INF directory (
 * not include the manifest file itself). This will help sun.misc.URLClassPath
 * to quickly locate the resource file and hotspot VM to locate the class file.
 *
 */
class JarMetaIndex {

    protected File file;

    private volatile HashSet<String> indexSet;

    private JarFileKind jarFileKind;

    public JarMetaIndex(String fileName) throws IOException {
        file = new File(fileName);
    }

    /* Returns a HashSet contains the meta index string. */
    HashSet<String> getMetaIndex() throws IOException {
        if (indexSet == null) {
            synchronized (this) {
                if (indexSet != null) {
                    return indexSet;
                }
                indexSet = new HashSet<String>();
                JarFile jar = new JarFile(file);
                try {
                    Enumeration<JarEntry> entries = jar.entries();
                    boolean containsOnlyClass = true;
                    boolean containsOnlyResource = true;
                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) entries.nextElement();
                        String name = entry.getName();
                        /*
                         * We only look at the non-directory entry. MANIFEST
                         * file is also skipped.
                         */
                        if (entry.isDirectory()
                                || name.equals("META-INF/MANIFEST.MF")) {
                            continue;
                        }

                        /*
                         * Once containsOnlyResource or containsOnlyClass turns
                         * to false, no need to check the entry type.
                         */
                        if (containsOnlyResource || containsOnlyClass) {
                            if (name.endsWith(".class")) {
                                containsOnlyResource = false;
                            } else {
                                containsOnlyClass = false;
                            }
                        }

                        /*
                         * Add the full-qualified name of plain files under
                         * META-INF directory to the indexSet.
                         */
                        if (name.startsWith("META-INF")) {
                            indexSet.add(name);
                            continue;
                        }

                        String[] pkgElements = name.split("/");
                        // Last one is the class name; definitely ignoring that
                        if (pkgElements.length > 2) {
                            String meta = null;
                            // Need more information than just first two package
                            // name elements to determine that classes in
                            // deploy.jar are not in rt.jar
                            if (pkgElements.length > 3
                                    && pkgElements[0].equals("com")
                                    && pkgElements[1].equals("sun")) {
                                // Need more precise information to disambiguate
                                // (illegal) references from applications to
                                // obsolete backported collections classes in
                                // com/sun/java/util
                                if (pkgElements.length > 4
                                        && pkgElements[2].equals("java")) {
                                    int bound = 0;
                                    if (pkgElements[3].equals("util")) {
                                        // Take all of the packages
                                        bound = pkgElements.length - 1;
                                    } else {
                                        // Trim it somewhat more
                                        bound = 4;
                                    }
                                    meta = "";
                                    for (int j = 0; j < bound; j++) {
                                        meta += pkgElements[j] + "/";
                                    }
                                } else {
                                    meta = pkgElements[0] + "/"
                                            + pkgElements[1] + "/"
                                            + pkgElements[2] + "/";
                                }
                            } else {
                                meta = pkgElements[0] + "/" + pkgElements[1]
                                        + "/";
                            }
                            indexSet.add(meta);
                        }

                    } // end of "while" loop;

                    /* Set "jarFileKind" attribute. */
                    if (containsOnlyClass) {
                        jarFileKind = JarFileKind.CLASSONLY;
                    } else if (containsOnlyResource) {
                        jarFileKind = JarFileKind.RESOURCEONLY;
                    } else {
                        jarFileKind = JarFileKind.MIXED;
                    }
                } finally {
                    jar.close();
                }
            }
        }
        return indexSet;
    }

    JarFileKind getJarFileKind() throws IOException {
        // Build meta index if it hasn't.
        if (indexSet == null) {
            indexSet = getMetaIndex();
        }
        return jarFileKind;
    }
}