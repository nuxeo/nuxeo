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
 * $Id: MetaMethodBinding.java 23473 2007-08-06 15:51:53Z tdelprat $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Serializable;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Meta method binding, used to invoke the EL expression that is already the
 * result of a method binding.
 * <p>
 * For instance it is useful to use this binding to provide action links defined
 * in NXActions extensions with links like
 * #{documentAction.createDocument('Domain')}.
 * <p>
 * There is no more than one level of abstraction:
 * <ul>
 * <li> the binding method value can be a standard method binding (with
 * parameters or not) ;
 * <li> the binding method value can result in another binding method value
 * after being invoke, in which case it is reinvoked again using the same
 * context ;
 * <li> no further method invoking will be performed.
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @Deprecated use {@link MetaMethodExpression}
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class MetaMethodBinding extends MethodBinding implements Serializable {

    private static final long serialVersionUID = -2721042412903607760L;

    private static final Log log = LogFactory.getLog(MetaMethodBinding.class);

    private final MethodBinding originalMethodBinding;

    public MetaMethodBinding(MethodBinding originalMethodBinding) {
        this.originalMethodBinding = originalMethodBinding;
    }

    @Override
    public Class getType(FacesContext facesContext) {
        // assume String is returned
        return String.class;
    }

    @Override
    public Object invoke(FacesContext context, Object[] params) {
        Object res = null;
        if (originalMethodBinding != null) {
            res = originalMethodBinding.invoke(context, params);
            if (res instanceof String) {
                MethodBinding newMeth;
                String expression = (String) res;
                if (ComponentTagUtils.isValueReference(expression)) {
                    Application app = context.getApplication();
                    newMeth = app.createMethodBinding(expression, null);
                } else {
                    newMeth = new ActionMethodBinding(expression);
                }
                if (newMeth != null) {
                    //try {
                        res = newMeth.invoke(context, null);
                    /*} catch (Exception err) {
                        log.error(String.format(
                                "Error processing action expression %s: %s",
                                expression, err));
                        res = null;
                    }*/
                }
            }
        }
        return res;
    }

}
