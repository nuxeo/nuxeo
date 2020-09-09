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

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * @since 11.3
 */
public class EmptyScrollRequest implements ScrollRequest {

    protected static final String SCROLL_TYPE = "empty";

    protected static final String SCROLL_NAME = "list";

    protected EmptyScrollRequest() {
        // nothing
    }

    @Override
    public String getType() {
        return SCROLL_TYPE;
    }

    @Override
    public String getName() {
        return SCROLL_NAME;
    }

    @Override
    public int getSize() {
        return 0;
    }

    public static EmptyScrollRequest of() {
        return new EmptyScrollRequest();
    }
}
