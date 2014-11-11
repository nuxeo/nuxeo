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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.util.List;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocRefMethod implements TemplateMethodModelEx {

    public Object exec(List arguments) throws TemplateModelException {
        if (arguments.size() != 1) {
            throw new TemplateModelException(
                    "Invalid number of arguments for docRef(id) method");
        }
        String value = null;
        SimpleScalar scalar = (SimpleScalar) arguments.get(0);
        if (scalar != null) {
            value = scalar.getAsString();
        } else {
            throw new TemplateModelException("the argument is not defined");
        }

        if (value.startsWith("/")) {
            return new PathRef(value);
        } else {
            return new IdRef(value);
        }
    }

}
