/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
