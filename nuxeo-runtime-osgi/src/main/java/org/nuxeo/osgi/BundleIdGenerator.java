/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
public class BundleIdGenerator {

    private static final Log log = LogFactory.getLog(BundleIdGenerator.class);

    private final Map<String, Long> ids = new HashMap<String, Long>();

    private long count = 0;

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
