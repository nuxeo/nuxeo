/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.api;

import java.io.Serializable;

/**
 * @author Anahide Tchertchian
 */
public class MockPagedListItem implements Serializable {

    private static final long serialVersionUID = 1L;

    final String name;

    final long position;

    public MockPagedListItem(String name, long position) {
        super();
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public long getPosition() {
        return position;
    }

}
