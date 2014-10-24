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
 * $Id: MetaActionSourceTagHandler.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import javax.faces.component.ActionSource;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.MetaRuleset;

import org.nuxeo.ecm.platform.ui.web.tag.jsf.MetaActionSourceRule;

import com.sun.faces.facelets.tag.jsf.html.HtmlComponentHandler;

/**
 * Component tag handler that wires an action source attributes to a
 * {@link MetaActionSourceRule} set.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaActionSourceTagHandler extends HtmlComponentHandler {

    public MetaActionSourceTagHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        MetaRuleset mr = super.createMetaRuleset(type);
        if (ActionSource.class.isAssignableFrom(type)) {
            mr.addRule(MetaActionSourceRule.Instance);
        }
        return mr;
    }
}
