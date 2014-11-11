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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;

/**
 * All path p[atterns are relative to the deployment root file. Supported
 * patterns are:
 * <ul>
 * <li>exact match patterns: <code>lib/myfile.jar</code>
 * <li>wildcard patterns: <code>lib/*</code>, <code>lib/*.rar</code>.
 * </ul>
 *
 * Note that only a single wildcard character can be used in a pattern. Also,
 * wildcards can be used only on file names - not on directory path segments.
 * Invalid patterns: <code>dir/*file*.jar</code> <code>*dir/file.jar</code>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class PathPattern {

    protected final String path;

    protected PathPattern next;

    protected PathPattern(String path) {
        this.path = path;
    }

    public void setNext(PathPattern next) {
        this.next = next;
    }

    public PathPattern getNext() {
        return next;
    }

    public boolean isExactMatch() {
        return false;
    }

    /**
     * Get the determined part of the path. For exact match this is the exact
     * file path, for wildcard match this is the exact parent path.
     */
    public String getPath() {
        return path;
    }

    public VirtualFile findFile(VirtualFile root) throws IOException {
        VirtualFile dir = path.length() == 0 ? root : root.getChild(path);
        if (dir == null) {
            return null;
        }
        List<VirtualFile> result = dir.getChildren(new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                return match(file.getName());
            }
        });
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public String findFirstMatchingPath(VirtualFile root) throws IOException {
        VirtualFile file = findFile(root);
        if (file != null) {
            return path + "/" + file.getName();
        }
        return null;
    }

    public boolean match(String name) {
        if (doMatch(name)) {
            return true;
        }
        if (next != null) {
            return next.match(name);
        }
        return false;
    }

    /**
     * Match the name of the given path. (Only name segment is matched).
     */
    public abstract boolean doMatch(String name);

    public static PathPattern parse(String pattern) {
        String prefix;
        String name;
        int i = pattern.lastIndexOf('/');
        if (i == -1) {
            prefix = "";
            name = pattern;
        } else {
            prefix = pattern.substring(0, i);
            name = pattern.substring(i + 1);
        }
        // lookup for wildcard
        i = name.indexOf('*');
        if (i == -1) {
            return new ExactMatchPattern(pattern, name);
        } else if (i == pattern.length() - 1) {
            return new SuffixPattern(prefix, name.substring(0, i));
        } else if (i == 0) {
            return new PrefixPattern(prefix, name.substring(1));
        } else {
            return new WildcardPattern(prefix, name.substring(0, i),
                    name.substring(i + 1));
        }
    }

    public static class ExactMatchPattern extends PathPattern {

        protected final String name;

        public ExactMatchPattern(String path, String name) {
            super(path);
            this.name = name;
        }

        @Override
        public boolean isExactMatch() {
            return true;
        }

        @Override
        public boolean doMatch(String name) {
            return this.name.equals(name);
        }
    }

    public static class SuffixPattern extends PathPattern {

        protected final String prefix;

        public SuffixPattern(String path, String name) {
            super(path);
            this.prefix = name;
        }

        @Override
        public boolean doMatch(String name) {
            return name.startsWith(prefix);
        }
    }

    public static class PrefixPattern extends PathPattern {

        protected final String suffix;

        public PrefixPattern(String path, String name) {
            super(path);
            this.suffix = name;
        }

        @Override
        public boolean doMatch(String name) {
            return name.endsWith(suffix);
        }
    }

    public static class WildcardPattern extends PathPattern {

        protected final String prefix;

        protected final String suffix;

        public WildcardPattern(String path, String prefix, String suffix) {
            super(path);
            this.suffix = suffix;
            this.prefix = prefix;
        }

        @Override
        public boolean doMatch(String name) {
            return prefix.length() + suffix.length() <= name.length()
                    && name.startsWith(prefix) && name.endsWith(suffix);
        }

    }

    public static class CompositePattern extends PathPattern {
        protected final List<PathPattern> patterns;

        public CompositePattern(String path) {
            super(path);
            patterns = new ArrayList<PathPattern>();
        }

        public void add(PathPattern pattern) {
            patterns.add(pattern);
        }

        @Override
        public boolean doMatch(String name) {
            for (PathPattern pattern : patterns) {
                if (pattern.doMatch(name)) {
                    return true;
                }
            }
            return false;
        }

    }

}
