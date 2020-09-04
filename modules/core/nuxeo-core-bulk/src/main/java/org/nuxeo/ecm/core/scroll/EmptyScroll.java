/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.core.scroll;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * Executes the scroll on an empty result set, useful for external scrollers.
 *
 * @since 11.3
 */
public class EmptyScroll implements Scroll {

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        // nothing
    }

    @Override
    public void close() {
        // nothing
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public List<String> next() {
        throw new NoSuchElementException();
    }
}
