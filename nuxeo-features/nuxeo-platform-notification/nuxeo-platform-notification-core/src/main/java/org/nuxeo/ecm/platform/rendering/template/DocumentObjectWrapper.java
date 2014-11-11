/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.template;

import org.nuxeo.ecm.core.api.DocumentModel;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentObjectWrapper extends DefaultObjectWrapper {

    @Override
    protected TemplateModel handleUnknownType(Object obj)
            throws TemplateModelException {
        if (obj instanceof DocumentModel) {
            return new DocumentModelAdapter((DocumentModel)obj);
        }
        return super.handleUnknownType(obj);
    }

}
