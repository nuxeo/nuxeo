/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Command Link with an overriden getActionExpression method. This is used to get the action expression querying the
 * parent of this component, an {@link UIOutputFile} component which values may be changing if it is itself within an
 * {@link UIInputFile} component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIOutputFileCommandLink extends HtmlCommandLink {

    public static final String COMPONENT_TYPE = UIOutputFileCommandLink.class.getName();

    @Override
    public MethodExpression getActionExpression() {
        UIComponent parent = getParent();
        if (parent instanceof UIOutputFile) {
            UIOutputFile outputFile = (UIOutputFile) parent;
            FacesContext context = FacesContext.getCurrentInstance();
            return new DownloadMethodExpression(outputFile.getBlobExpression(context),
                    outputFile.getFileNameExpression(context));
        }
        return super.getActionExpression();
    }

}
