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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ReviewEventObserverLocal.java 19581 2007-05-28 23:06:15Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.web.listener.ejb.local;

import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.platform.workflow.web.api.ReviewEventObserver;

/**
 * Review event observer local.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ReviewEventObserverLocal extends ReviewEventObserver {

    @Remove
    @Destroy
    void destroy();

}
