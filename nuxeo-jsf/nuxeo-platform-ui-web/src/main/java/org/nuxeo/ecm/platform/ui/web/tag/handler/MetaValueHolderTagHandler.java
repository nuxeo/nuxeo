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
 * $Id: MetaValueHolderTagHandler.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import javax.faces.component.ValueHolder;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.MetaRuleset;

import org.nuxeo.ecm.platform.ui.web.tag.jsf.MetaValueHolderRule;

import com.sun.faces.facelets.tag.jsf.html.HtmlComponentHandler;

/**
 * Component tag handler that wires a value holder rule attributes to a
 * {@link MetaValueHolderRule} set.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaValueHolderTagHandler extends HtmlComponentHandler {

    public MetaValueHolderTagHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        MetaRuleset m = super.createMetaRuleset(type);
        if (ValueHolder.class.isAssignableFrom(type)) {
            m.addRule(MetaValueHolderRule.Instance);
        }
        return m;
    }
}
