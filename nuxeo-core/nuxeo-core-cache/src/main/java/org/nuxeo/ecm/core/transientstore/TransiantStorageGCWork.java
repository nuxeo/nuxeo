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
package org.nuxeo.ecm.core.transientstore;

import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class TransiantStorageGCWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    @Override
    public String getTitle() {
        return "Transient Store GC";
    }

    @Override
    public void work() {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        tss.doGC();
    }

}
