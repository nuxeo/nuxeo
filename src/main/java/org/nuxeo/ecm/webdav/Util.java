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

package org.nuxeo.ecm.webdav;

import net.java.dev.webdav.jaxrs.xml.conditions.*;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringWriter;

/**
 * Utility functions.
 */
public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    private static CoreSession session;
    private static Unmarshaller unmarshaller;

    // Utility class.
    private Util() {
    }

    public static CoreSession getSession(HttpServletRequest request) throws Exception {
        return getSession();

        // FIXME
        //UserSession us = UserSession.getCurrentSession(request);
        //return us.getCoreSession();
    }

    private static CoreSession getSession() throws Exception {
        if (session == null) {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            Repository repo = rm.getDefaultRepository();
            session = repo.open();
        }
        return session;
    }

    // utility methods related to JAXB marshalling

    public static JAXBContext getJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(new Class<?>[] {
                ActiveLock.class, AllProp.class, CannotModifyProtectedProperty.class, Collection.class,
                CreationDate.class, Depth.class, DisplayName.class, net.java.dev.webdav.jaxrs.xml.elements.Error.class, Exclusive.class,
                GetContentLanguage.class, GetContentLength.class, GetContentType.class, GetETag.class,
                GetLastModified.class, HRef.class, Include.class, Location.class, LockDiscovery.class, LockEntry.class,
                LockInfo.class, LockRoot.class, LockScope.class, LockToken.class, LockTokenMatchesRequestUri.class,
                LockTokenSubmitted.class, LockType.class, MultiStatus.class, NoConflictingLock.class,
                NoExternalEntities.class, Owner.class, PreservedLiveProperties.class, Prop.class, PropertyUpdate.class,
                PropFind.class, PropFindFiniteDepth.class, PropName.class, PropStat.class, Remove.class,
                ResourceType.class, javax.ws.rs.core.Response.class, ResponseDescription.class, Set.class, Shared.class, Status.class,
                SupportedLock.class, TimeOut.class, Write.class});
    }

    public static Unmarshaller getUnmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            unmarshaller = getJaxbContext().createUnmarshaller();
        }
        return unmarshaller;
    }

    // For debugging.

    public static void printAsXml(Object o) throws JAXBException {
/*
        StringWriter sw = new StringWriter();
        Marshaller marshaller = Util.getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(o, sw);
        System.out.println(sw);
*/
    }

}
