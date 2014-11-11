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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: UIOutputFileCommandLink.java 20116 2007-06-06 09:13:31Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.file;

import javax.el.MethodExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.platform.ui.web.binding.DownloadMethodExpression;

/**
 * Command Link with an overriden getActionExpression method.
 *
 * This is used to get the action expression querying the parent of this
 * component, an {@link UIOutputFile} component which values may be changing if
 * it is itself within an {@link UIInputFile} component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class UIOutputFileCommandLink extends HtmlCommandLink {

    @Override
    public MethodExpression getActionExpression() {
        UIComponent parent = getParent();
        if (parent instanceof UIOutputFile) {
            UIOutputFile outputFile = (UIOutputFile) parent;
            FacesContext context = FacesContext.getCurrentInstance();
            return new DownloadMethodExpression(
                    outputFile.getBlobExpression(context),
                    outputFile.getFileNameExpression(context));
        }
        return super.getActionExpression();
    }

}
