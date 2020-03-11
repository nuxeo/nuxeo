/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.fm;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.template.api.context.DocumentWrapper;
import org.nuxeo.template.context.AbstractContextBuilder;

import freemarker.template.TemplateModelException;

public class FMContextBuilder extends AbstractContextBuilder {

    protected static final Log log = LogFactory.getLog(FMContextBuilder.class);

    protected DocumentWrapper nuxeoWrapper;

    public FMContextBuilder() {
        final DocumentObjectWrapper fmWrapper = new DocumentObjectWrapper(null);

        nuxeoWrapper = new DocumentWrapper() {
            @Override
            public Object wrap(DocumentModel doc) {
                try {
                    return fmWrapper.wrap(doc);
                } catch (TemplateModelException e) {
                    throw new NuxeoException(e);
                }
            }

            @Override
            public Object wrap(List<LogEntry> auditEntries) {
                try {
                    return fmWrapper.wrap(auditEntries);
                } catch (TemplateModelException e) {
                    throw new NuxeoException(e);
                }
            }
        };
    }

    @Override
    protected DocumentWrapper getWrapper() {
        return nuxeoWrapper;
    }

}
