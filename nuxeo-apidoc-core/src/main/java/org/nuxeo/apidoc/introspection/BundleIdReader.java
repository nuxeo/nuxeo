/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.apidoc.introspection;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BundleIdReader {

    private static final Log log = LogFactory.getLog(BundleIdReader.class);

    protected final Map<String, Long> ids = new HashMap<String, Long>();

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
            log.error("The bundle.ids file is corrupted. Resetting bundle ids.");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public List<String> getBundleNames() {
        List<String> names = new ArrayList<String>();
        names.addAll(ids.keySet());
        return names;
    }

}
