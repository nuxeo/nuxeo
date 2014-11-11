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

    protected static final List<String> beanNames = new ArrayList<String>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_REMOTABLE_SEAMBEANS)) {
            RemotableSeamBeansDescriptor descriptor = (RemotableSeamBeansDescriptor) contribution;
            beanNames.addAll(descriptor.getBeanNames());
        }
    }

    public List<String> getRemotableBeanNames() {
        return beanNames;
    }

    @Factory(value="SeamRemotingBeanNames", scope=ScopeType.APPLICATION)
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
