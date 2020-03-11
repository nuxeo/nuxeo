/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.work;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A fat job that cannot be serialized in a stream
 *
 * @since 11.1
 */
public class FatWork extends AbstractWork {

    private static final Logger log = LogManager.getLogger(FatWork.class);

    private static final long serialVersionUID = 1L;

    protected int size;

    protected String veryLongString;

    /**
     * Creates a work instance with the requested size.
     *
     * @param size the work size in byte
     */
    public FatWork(String id, int size) {
        super(id);
        this.size = size;
        setProgress(Progress.PROGRESS_0_PC);
        veryLongString = new String(new char[size]).replace('\0', 'X');
    }

    @Override
    public String getCategory() {
        return SleepWork.CATEGORY;
    }

    @Override
    public String getTitle() {
        return id + " (" + size + " bytes)";
    }

    @Override
    public void work() {
        log.debug(getTitle() + " is running");
        if (veryLongString.length() != size) {
            throw new IllegalStateException("Invalid string size: " + veryLongString.length() + " instead of: " + size);
        }
    }
}
