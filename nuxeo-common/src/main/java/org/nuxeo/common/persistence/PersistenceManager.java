/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.common.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PersistenceManager {

    private final File dir;
    private final Map<String, PersistentObject> registry;

    public PersistenceManager(File dir) {
        this.dir = dir;
        registry = new Hashtable<String, PersistentObject>();
    }

    public File getStorageDirectory() {
        return dir;
    }

    public void registerPersistentObject(String id, PersistentObject object) {
        registry.put(id, object);
    }

    public void removePersistentObject(String id) {
        registry.remove(id);
    }

    public Collection<PersistentObject> getPersistentObjects() {
        return registry.values();
    }

    public void start() throws Exception {
        for (Map.Entry<String, PersistentObject> entry : registry.entrySet()) {
            String id = entry.getKey();
            PersistentObject obj = entry.getValue();
            restorePersistentObject(id, obj);
        }
    }

    public void stop() throws Exception {
        for (Map.Entry<String, PersistentObject> entry : registry.entrySet()) {
            String id = entry.getKey();
            PersistentObject obj = entry.getValue();
            storePersistentObject(id, obj);
        }
    }

    public File getPersistenceFile(String id) {
        return new File(dir, id + ".xml");
    }

    protected void restorePersistentObject(String id, PersistentObject object) throws Exception {
        Reader reader = null;
        Memento memento = null;
        try {
            File file = getPersistenceFile(id);
            if (!file.isFile()) { // first time restoreState was called
                object.restoreState(null);
                return;
            }
            reader = new BufferedReader(new FileReader(file));
            memento =  XMLMemento.createReadRoot(reader, dir.getAbsolutePath());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        if (memento != null) {
            object.restoreState(memento);
        }
    }

    protected void storePersistentObject(String id, PersistentObject object) throws Exception {
        Memento memento = XMLMemento.createWriteRoot("persistence");
        object.saveState(memento);

        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(getPersistenceFile(id)));
            ((XMLMemento) memento).save(writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
