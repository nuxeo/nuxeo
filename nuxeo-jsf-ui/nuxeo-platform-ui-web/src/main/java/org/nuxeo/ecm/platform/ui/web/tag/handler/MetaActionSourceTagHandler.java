/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Component tag handler that wires an action source attributes to a {@link MetaActionSourceRule} set.
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
