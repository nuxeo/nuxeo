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
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiBundleIdGenerator {

    private static final Log log = LogFactory.getLog(OSGiBundleIdGenerator.class);

    private final Map<String, Long> ids = new HashMap<String, Long>();

    protected long count = 0;

    public synchronized void load(File file) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(file)));
            count = in.readLong();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                long id = in.readLong();
                ids.put(key, id);
            }
        } catch (FileNotFoundException e) {
            // do nothing - this is the first time the runtime is started
        } catch (IOException e) {
            // may be the file is corrupted
            file.delete();
            log.error("The bundle.ids file is corrupted. reseting bundle ids.");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public synchronized void store(File file) throws IOException {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(file)));
            out.writeLong(count);
            int size = ids.size();
            out.writeInt(size);
            for (Map.Entry<String, Long> entry : ids.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeLong(entry.getValue());
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public synchronized long getBundleId(String name) {
        Long id = ids.get(name);
        if (id == null) {
            id = count++;
            ids.put(name, id);
        }
        return id;
    }

    public synchronized long addBundle(String name) {
        long id = count++;
        ids.put(name, id);
        return id;
    }

    public synchronized boolean contains(String name) {
        return ids.containsKey(name);
    }

}
