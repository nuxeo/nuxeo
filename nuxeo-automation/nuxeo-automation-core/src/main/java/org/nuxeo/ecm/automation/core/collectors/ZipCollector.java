/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.collectors;

import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Collects blobs into a zip blob.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ZipCollector implements OutputCollector<Blob, Blob> {

    @Override
    public boolean add(Blob obj) {
        return false;
    }

    @Override
    public Blob getOutput() {
        return null; //TODO
    }

}
