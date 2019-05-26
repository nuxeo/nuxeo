/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.model;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */

public class Container {

    private String xpath;

    private int offset;

    public static Container fromString(String endContainer) {
        String[] values = endContainer.split(", ");
        return new Container(values[0], Integer.parseInt(values[1]));
    }

    public Container(String xpath, int offset) {
        this.xpath = xpath;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public String getXpath() {
        return xpath;
    }

    public String generateString() {
        return xpath + ", " + offset;
    }

}
