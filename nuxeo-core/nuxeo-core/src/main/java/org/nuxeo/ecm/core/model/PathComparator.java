/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class PathComparator implements Comparator<Document>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Document o1, Document o2) {
        // The character "/" should be considered as the highest discriminant to
        // sort paths. So we replace it with the first unicode character
        String path1 = o1.getPath().replace("/", "\u0000");
        return path1.compareTo(o2.getPath().replace("/", "\u0000"));
    }

}
