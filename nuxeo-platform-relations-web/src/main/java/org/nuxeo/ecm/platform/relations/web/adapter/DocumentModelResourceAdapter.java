/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentModelResourceAdapter.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.relations.api.Resource;

/**
 * Resource adapter using the document model id.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated class has moved, see
 *             {@link org.nuxeo.ecm.platform.relations.adapters.DocumentModelResourceAdapter}.
 */
@Deprecated
public class DocumentModelResourceAdapter extends
        org.nuxeo.ecm.platform.relations.adapters.DocumentModelResourceAdapter {

    private static final Log log = LogFactory.getLog(DocumentModelResourceAdapter.class);

    private static final long serialVersionUID = -5307418102496342779L;

    @Override
    public Object getResourceRepresentation(Resource resource) {
        log.warn(String.format(
                "This adapter is deprecated, should use %s instead of %s",
                org.nuxeo.ecm.platform.relations.adapters.DocumentModelResourceAdapter.class,
                getClass()));
        return super.getResourceRepresentation(resource);
    }

    @Override
    public Resource getResource(Object object) {
        log.warn(String.format(
                "This adapter is deprecated, should use %s instead of %s",
                org.nuxeo.ecm.platform.relations.adapters.DocumentModelResourceAdapter.class,
                getClass()));
        return super.getResource(object);
    }

    /**
     * Class is deprecated so do not make it match DocumentModel resources as
     * compatibility is not needed here.
     */
    @Override
    public Class<?> getKlass() {
        return null;
    }

}
