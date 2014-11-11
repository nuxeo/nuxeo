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
 * $Id: ActionMethodBinding.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

/**
 * Simple action method binding for method evaluations already resolved to a
 * String.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class ActionMethodBinding extends MethodBinding implements Serializable {

    private static final long serialVersionUID = 36959684012420323L;

    private final String result;

    public ActionMethodBinding(String result) {
        this.result = result;
    }

    @Override
    public Object invoke(FacesContext context, Object[] params) {
        return result;
    }

    @Override
    public String getExpressionString() {
        return result;
    }

    @Override
    public Class getType(FacesContext context) {
        return String.class;
    }

}
