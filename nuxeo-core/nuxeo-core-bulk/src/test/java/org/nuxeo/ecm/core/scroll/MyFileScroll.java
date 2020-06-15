/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.scroll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * Scroll lines of a file, the scroll query is the path of the file.
 */
public class MyFileScroll implements Scroll {

    private static final Logger log = LogManager.getLogger(MyFileScroll.class);

    protected LineIterator iterator;

    protected int size;

    @Override
    public void init(ScrollRequest scrollRequest, Map<String, String> map) {
        if (!(scrollRequest instanceof GenericScrollRequest)) {
            throw new IllegalArgumentException(
                    "Requires a GenericScrollRequest got a " + scrollRequest.getClass().getCanonicalName());
        }
        GenericScrollRequest request = (GenericScrollRequest) scrollRequest;
        File file = new File(request.getQuery());
        size = request.getSize();
        try {
            iterator = FileUtils.lineIterator(file, "UTF-8");
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file " + file, e);
        }
        log.info("Scroll init: {}, size: {}", file.getAbsolutePath(), size);
    }

    @Override
    public void close() {
        if (iterator != null) {
            try {
                iterator.close();
            } catch (IOException e) {
                // we don't mind on close
            }
        }
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public List<String> next() {
        List<String> ret = new ArrayList<>(size);
        while (iterator.hasNext() && ret.size() < size) {
            ret.add(iterator.nextLine());
        }
        if (ret.isEmpty()) {
            throw new NoSuchElementException();
        }
        log.debug("next: {}", ret);
        return ret;
    }
}
