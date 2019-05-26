/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.seamremoting;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

@Name("SeamRemotingJSBuilderService")
@Scope(ScopeType.STATELESS)
public class SeamRemotingJSBuilderComponent extends DefaultComponent implements SeamRemotingJSBuilderService {

    public static final String EP_REMOTABLE_SEAMBEANS = "remotableSeamBeans";

    protected static final List<String> beanNames = new ArrayList<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_REMOTABLE_SEAMBEANS)) {
            RemotableSeamBeansDescriptor descriptor = (RemotableSeamBeansDescriptor) contribution;
            beanNames.addAll(descriptor.getBeanNames());
        }
    }

    @Override
    public List<String> getRemotableBeanNames() {
        return beanNames;
    }

    @Override
    @Factory(value = "SeamRemotingBeanNames", scope = ScopeType.APPLICATION)
    public String getSeamRemotingJavaScriptURLParameters() {

        StringBuilder sb = new StringBuilder();

        int idx = 0;
        for (String beanName : beanNames) {
            sb.append(beanName);
            idx++;
            if (idx < beanNames.size()) {
                sb.append('&');
            }
        }
        return sb.toString();
    }

}
