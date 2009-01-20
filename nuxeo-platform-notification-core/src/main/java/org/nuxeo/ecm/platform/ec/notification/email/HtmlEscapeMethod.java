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
 *     tmartins
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.email;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class HtmlEscapeMethod implements TemplateMethodModel {

    public Object exec(List arg0) throws TemplateModelException {
        if (arg0.size() != 1) {
            throw new IllegalArgumentException();
        }
        String str = (String) arg0.get(0);
        return StringEscapeUtils.escapeHtml(str);
    }

}
