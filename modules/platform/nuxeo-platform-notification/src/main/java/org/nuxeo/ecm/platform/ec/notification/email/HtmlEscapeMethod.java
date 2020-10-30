/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     tmartins
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.email;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class HtmlEscapeMethod implements TemplateMethodModel {

    @Override
    public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
        if (arg0.size() != 1) {
            throw new IllegalArgumentException();
        }
        String str = (String) arg0.get(0);
        return StringEscapeUtils.escapeHtml4(str);
    }

}
