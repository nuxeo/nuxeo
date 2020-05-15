/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BundleIdReader {

    private static final Logger log = LogManager.getLogger(BundleIdReader.class);

    protected final Map<String, Long> ids = new HashMap<>();

    @SuppressWarnings("unused")
    private long count = 0;

    public synchronized void load(File file) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            count = in.readLong();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                long id = in.readLong();
                ids.put(key, Long.valueOf(id));
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
        List<String> names = new ArrayList<>();
        names.addAll(ids.keySet());
        return names;
    }

}
