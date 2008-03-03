/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.repository.jcr;

import java.io.File;

import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryFactory;

/**
 * JackRabbit Repository factory implementation.
 * <p>
 * The JackRabbit repository homes are set to
 * ${jboss.server.data.dir}/Jackrabbit/&lt;repository-name&gt;"
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JCRRepositoryFactory implements RepositoryFactory {

    public Repository createRepository(RepositoryDescriptor descriptor) throws Exception {
        if (descriptor.getForceReloadTypes()) {
            // apply hack to remove custom types file from jackrabbit
            // this is forcing the ecm type remapping
            String home = descriptor.getHomeDirectory();
            File file = new File(home, "repository/nodetypes/custom_nodetypes.xml");
            if (file.exists()) {
                if (!file.renameTo(new File(file.getParentFile(), "custom_nodetypes.xml.bak"))) {
                    file.delete();
                }
            }
            file = new File(home, "repository/namespaces/ns_reg.properties");
            if (file.exists()) {
                if (!file.renameTo(new File(file.getParentFile(), "ns_reg.properties.bak"))) {
                    file.delete();
                }
            }
        }
        return JCRRepository.create(descriptor);
    }

}
