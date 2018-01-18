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
package org.nuxeo.template.xdocreport.jaxrs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class AbstractResourceService extends DefaultObject {

    protected static Log log = LogFactory.getLog(AbstractResourceService.class);

    protected CoreSession session;

    public AbstractResourceService(CoreSession session) {
        this.session = session;
    }

    protected CoreSession getCoreSession() {
        return session;
    }

    protected List<TemplateSourceDocument> getTemplates() {
        List<TemplateSourceDocument> result = new ArrayList<TemplateSourceDocument>();
        CoreSession session = getCoreSession();
        StringBuffer sb = new StringBuffer(
                "select * from Document where ecm:mixinType = 'Template' AND ecm:isTrashed = 0");
        sb.append(" AND tmpl:templateType = 'XDocReportProcessor'");
        DocumentModelList docs = session.query(sb.toString());
        for (DocumentModel doc : docs) {
            TemplateSourceDocument tmpl = doc.getAdapter(TemplateSourceDocument.class);
            if (tmpl != null) {
                result.add(tmpl);
            }
        }
        return result;
    }

}
