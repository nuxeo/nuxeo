/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Unrestricted process end.
 *
 * @since 5.6
 */
public class EndProcessUnrestricted extends UnrestrictedSessionRunner {

    private final Set<String> recipients;

    private final List<TaskInstance> tis;

    public EndProcessUnrestricted(CoreSession session, List<TaskInstance> tis) {
        super(session);
        this.recipients = new HashSet<String>();
        this.tis = tis;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() throws ClientException {
        // end process and tasks
        for (TaskInstance ti : tis) {
            String actor = ti.getActorId();
            recipients.add(actor);
            Set<PooledActor> pooledActors = ti.getPooledActors();
            for (PooledActor pa : pooledActors) {
                recipients.add(pa.getActorId());
            }
        }
    }

    public Set<String> getRecipients() {
        return recipients;
    }
}
