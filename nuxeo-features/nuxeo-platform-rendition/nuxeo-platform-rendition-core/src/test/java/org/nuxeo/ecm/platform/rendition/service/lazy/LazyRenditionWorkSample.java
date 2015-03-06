/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.service.lazy;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.rendition.lazy.AbstractRenditionBuilderWork;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class LazyRenditionWorkSample extends AbstractRenditionBuilderWork {

    private static final long serialVersionUID = 1L;

    public LazyRenditionWorkSample(String key, DocumentModel doc, RenditionDefinition def) {
        super(key, doc, def);
    }

    @Override
    protected List<Blob> doComputeRendition(CoreSession session, DocumentModel doc, RenditionDefinition def) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Blob> blobs = new ArrayList<Blob>();
        StringBlob blob = new StringBlob("I am really lazy");
        blob.setFilename("LazyBoy.txt");
        blobs.add(blob);
        return blobs;
    }

}
