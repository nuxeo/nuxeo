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
package org.nuxeo.runtime.jboss.deployer.structure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;
import org.nuxeo.runtime.jboss.deployer.Utils;

/**
 * Helper to parse and load classpaths
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathMatcher {

    protected final Logger log = Logger.getLogger(getClass());

    protected final List<String> exactPaths;

    protected final Map<String, PathPattern> patterns;

    public PathMatcher() {
        exactPaths = new ArrayList<String>();
        patterns = new HashMap<String, PathPattern>();
    }

    public List<String> getExactPaths() {
        return exactPaths;
    }

    public void addExactPath(String path) {
        exactPaths.add(path);
    }

    public void addPattern(String path) {
        int i = path.indexOf('*');
        if (i == -1) {
            addExactPath(path);
        } else {
            addPattern(PathPattern.parse(path));
        }
    }

    public boolean isEmpty() {
        return patterns.isEmpty() && exactPaths.isEmpty();
    }

    public PathPattern addPattern(PathPattern pattern) {
        PathPattern existing = patterns.get(pattern.getPath());
        if (existing != null) {
            existing.setNext(pattern);
        } else {
            patterns.put(pattern.getPath(), pattern);
        }
        return pattern;
    }

    public void addPatterns(String[] paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            addPattern(path);
        }
    }

    public Map<String, PathPattern> getPatterns() {
        return patterns;
    }

    /**
     * Parse and add patterns from a class path expression. Example of
     * expression: <code>lib/*.jar:bundles/*:nuxeo-core-*.rar</code>
     *
     * @param pathExpr a class path pattenr expression
     */
    public void addPatterns(String pathExpr) {
        if (pathExpr == null || pathExpr.length() == 0) {
            return;
        }
        String[] ar = Utils.split(pathExpr, ':', true);
        for (String path : ar) {
            addPattern(path);
        }
    }

    /**
     * Returns null if nothing matches.
     */
    public List<VirtualFile> getMatches(VirtualFile root) throws IOException {
        List<VirtualFile> result = new ArrayList<VirtualFile>();
        for (String path : exactPaths) {
            VirtualFile file = root.getChild(path);
            if (file != null) {
                result.add(file);
            } else {
                log.warn("Path pattern not matched: " + path
                        + " in deployment " + root.getName());
            }
        }
        for (final PathPattern pattern : patterns.values()) {
            String path = pattern.getPath();
            VirtualFile dir = path.length() == 0 ? root : root.getChild(path);
            if (dir != null) {
                result.addAll(dir.getChildren(new VirtualFileFilter() {
                    @Override
                    public boolean accepts(VirtualFile file) {
                        return pattern.match(file.getName());
                    }
                }));
            } else {
                log.warn("Path pattern not matched: " + path
                        + " in deployment " + root.getName());
            }
        }
        return result;
    }

    public List<File> getMatchesAsFiles(File root) throws IOException {
        List<File> result = new ArrayList<File>();
        for (String path : exactPaths) {
            File file = new File(root, path);
            if (file.exists()) {
                result.add(file);
            } else {
                log.warn("Path pattern not matched: " + path
                        + " in deployment " + root.getName());
            }
        }
        for (final PathPattern pattern : patterns.values()) {
            String path = pattern.getPath();
            File dir = path.length() == 0 ? root : new File(root, path);
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    if (pattern.match(file)) {
                        result.add(new File(dir, file));
                    }
                }
            } else {
                log.warn("Path pattern not matched: " + path
                        + " in deployment " + root.getName());
            }
        }
        return result;
    }

    public List<String> getMatchesAsPaths(File root) throws IOException {
        List<String> result = new ArrayList<String>();
        for (String path : exactPaths) {
            File file = new File(root, path);
            if (file.exists()) {
                result.add(path);
            } else {
                log.warn("Path pattern not matched: " + path
                        + " in deployment " + root.getName());
            }
        }
        for (final PathPattern pattern : patterns.values()) {
            String path = pattern.getPath();
            File dir = path.length() == 0 ? root : new File(root, path);
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    if (pattern.match(file)) {
                        result.add(path + '/' + file);
                    }
                }
            } else {
                log.warn("Path pattern not matched: " + path
                        + " in deployment " + root.getName());
            }
        }
        return result;
    }

    public List<File> getAbsoluteMatches() throws IOException {
        List<File> result = new ArrayList<File>();
        for (String path : exactPaths) {
            File file = new File(path);
            if (file.exists()) {
                result.add(file);
            } else {
                log.warn("Absolute path pattern not matched: " + path);
            }
        }
        for (final PathPattern pattern : patterns.values()) {
            String path = pattern.getPath();
            File dir = path.length() == 0 ? new File("/") : new File(path);
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    if (pattern.match(file)) {
                        result.add(new File(dir, file));
                    }
                }
            } else {
                log.warn("Absolute path pattern not matched: " + path);
            }
        }
        return result;
    }

}
