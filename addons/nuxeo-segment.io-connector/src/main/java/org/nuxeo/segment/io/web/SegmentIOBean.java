/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.segment.io.web;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;

@Name("segmentIOActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class SegmentIOBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create=true)
    NuxeoPrincipal currentNuxeoPrincipal;

    protected static final String SEGMENTIO_FLAG = "segment.io.identify.flag";

    public String getWriteKey() {
        return Framework.getService(SegmentIO.class).getWriteKey();
    }

    public boolean needsIdentify() {
        if (currentNuxeoPrincipal==null) {
            return false;
        }

        if (currentNuxeoPrincipal.isAnonymous()) {
            return false;
        }

        Object flag = Contexts.getSessionContext().get(SEGMENTIO_FLAG);
        if (flag == null) {
            Contexts.getSessionContext().set(SEGMENTIO_FLAG, true);
            return true;
        }
        return false;
    }

}
