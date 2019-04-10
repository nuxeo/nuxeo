/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import org.nuxeo.binary.metadata.api.BinaryMetadataProcessor;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("processor")
public class MetadataProcessorDescriptor {

    @XNode("@id")
    protected String id;

    protected BinaryMetadataProcessor processor;

    @XNode("@class")
    public void setClass(Class<? extends BinaryMetadataProcessor> aType) throws InstantiationException,
            IllegalAccessException {
        processor = aType.newInstance();
    }

    public String getId() {
        return id;
    }
}
