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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.AbstractTreeCacheListener;
import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jgroups.View;

/**
 * This class delegates notifications received from TreeCache to the
 * list of NX cache listeners
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class TreeCacheAdaptor extends AbstractTreeCacheListener {

    private static final Log log = LogFactory.getLog(TreeCacheAdaptor.class);

    public TreeCacheAdaptor() {
        log.debug("<constructor>");
    }

    @Override
    public void cacheStarted(TreeCache arg0) {
        log.debug("<cacheStarted> ");
    }

    @Override
    public void cacheStopped(TreeCache arg0) {
        log.debug("<cacheStopped> ");
    }

    @Override
    public void nodeActivate(Fqn fqn, boolean arg1) {
        log.debug("<nodeActivate> fqn: " + fqn);
    }

    @Override
    public void nodeCreated(Fqn fqn) {
        log.debug("<nodeCreated> fqn: " + fqn);
    }

    @Override
    public void nodeEvict(Fqn fqn, boolean arg1) {
        log.debug("<nodeEvict> fqn: " + fqn);
    }

    @Override
    public void nodeEvicted(Fqn fqn) {
        log.debug("<nodeEvicted> fqn: " + fqn);
    }

    @Override
    public void nodeLoaded(Fqn fqn) {
        log.debug("<nodeLoaded> fqn: " + fqn);
    }

    @Override
    public void nodeModified(Fqn fqn) {
        log.debug("<nodeModified> fqn: " + fqn);
    }

    @Override
    public void nodeModify(Fqn fqn, boolean arg1, boolean arg2) {
        log.debug("<nodeModify> fqn: " + fqn);
    }

    @Override
    public void nodePassivate(Fqn fqn, boolean arg1) {
        log.debug("<nodePassivate> fqn: " + fqn);
    }

    @Override
    public void nodeRemove(Fqn fqn, boolean arg1, boolean arg2) {
        log.debug("<nodeRemove> fqn: " + fqn);
    }

    @Override
    public void nodeRemoved(Fqn fqn) {
        log.debug("<nodeRemoved> fqn: " + fqn);
    }

    @Override
    public void nodeVisited(Fqn fqn) {
        log.debug("<nodeVisited> not handled: ");
    }

    @Override
    public void viewChange(View view) {
        log.debug("<viewChange> not handled: ");
    }

}
