/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.core.search.api.client.search.results.document;

import java.io.IOException;

import org.nuxeo.runtime.services.streaming.StringSource;

/**
 * A StringSource with the length.
 *
 * @author arussel
 */
public class ExtendedStringSource extends StringSource {

    private final Integer length;

    public ExtendedStringSource(String string, Integer length) {
        super(string);
        this.length = length;
    }

    @Override
    public long getLength() throws IOException {
        return length == null ? super.getLength() : length;
    }

}
