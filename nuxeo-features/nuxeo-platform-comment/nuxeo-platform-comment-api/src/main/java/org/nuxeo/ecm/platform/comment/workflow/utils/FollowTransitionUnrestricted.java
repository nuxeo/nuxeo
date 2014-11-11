/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.comment.workflow.utils;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public final class FollowTransitionUnrestricted extends
        UnrestrictedSessionRunner {

    public final DocumentRef docRef;

    public final String transition;

    public FollowTransitionUnrestricted(CoreSession session,
            DocumentRef docRef, String transition) {
        super(session);
        this.docRef = docRef;
        this.transition = transition;
    }

    @Override
    public void run() throws ClientException {
        session.followTransition(docRef, transition);
        session.save();
    }

}
