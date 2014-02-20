/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import javax.faces.component.ActionSource;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.MetaRuleset;

import org.nuxeo.ecm.platform.ui.web.tag.jsf.MetaActionSourceRule;

/**
 * Ajax Component tag handler that wires an action source attributes to a
 * {@link MetaActionSourceRule} set.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public class AjaxMetaActionSourceTagHandler extends AjaxComponentHandler {

    public AjaxMetaActionSourceTagHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    protected MetaRuleset createMetaRuleset(Class type) {
        MetaRuleset mr = super.createMetaRuleset(type);
        if (ActionSource.class.isAssignableFrom(type)) {
            mr.addRule(MetaActionSourceRule.Instance);
        }
        return mr;
    }
}
