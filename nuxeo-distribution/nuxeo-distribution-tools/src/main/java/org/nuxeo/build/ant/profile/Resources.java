/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.ant.profile;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.nuxeo.build.maven.MavenClientFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Resources extends DataType  implements ResourceCollection {

    protected List<ResourceCollection> sets;
    protected File baseDir;

    public Resources(Project project, File baseDir) {
        this.baseDir = baseDir;
        sets = new ArrayList<ResourceCollection>();
        AntProfileManager mgr = MavenClientFactory.getInstance().getAntProfileManager();
        for (String name : baseDir.list()) {
            if (name.equals("default") || mgr.isProfileActive(name)) {
                FileSet fs = new FileSet();
                fs.setDir(new File(baseDir, name));
                sets.add(fs);
            } else {
                project.log("Skiping disabled resource directory: "+name);
            }
        }
    }

    public boolean isFilesystemOnly() {
        return true;
    }

    public Iterator<Resource> iterator() {
        return new CompositeIterator(sets);
    }

    public int size() {
        int len = 0;
        for (ResourceCollection set : sets) {
            len += set.size();
        }
        return 0;
    }


    public static class CompositeIterator implements Iterator<Resource>{
        Iterator<Resource>[] its;
        int offset;
        @SuppressWarnings("unchecked")
        public CompositeIterator(List<ResourceCollection> cols) {
            its = new Iterator[cols.size()];
            for (int i=0; i<its.length ; i++) {
                its[i] = cols.get(i).iterator();
            }
        }
        public boolean hasNext() {
            if (offset >= its.length) {
                return false;
            }
            if (its[offset].hasNext()) {
                return true;
            }
            if (offset+1 >= its.length) {
                return false;
            }
            if (its[offset+1].hasNext()) {
                return true;
            }
            return false;
        }
        public Resource next() {
            if (offset >= its.length) {
                throw new NoSuchElementException("no more elements");
            }
            if (!its[offset].hasNext()) {
                offset++;
                if (offset >= its.length) {
                    throw new NoSuchElementException("no more elements");
                }
            }
            return its[offset].next();
        }

        public void remove() { throw new UnsupportedOperationException("Remove not supported");};
    }
}
