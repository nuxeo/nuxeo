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

import net.java.dev.webdav.core.jaxrs.xml.properties.*;
import net.java.dev.webdav.jaxrs.xml.conditions.*;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Utility functions.
 */
public class Util {

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
                ResourceType.class, Response.class, ResponseDescription.class, Set.class, Shared.class, Status.class,
                SupportedLock.class, TimeOut.class, Write.class, IsCollection.class, IsFolder.class, IsHidden.class,
                Win32CreationTime.class, Win32FileAttributes.class, Win32LastAccessTime.class, Win32LastModifiedTime.class});
    }

    public static Unmarshaller getUnmarshaller() throws JAXBException {
        return getJaxbContext().createUnmarshaller();
    }

    // For debugging.

    public static void printAsXml(Object o) throws JAXBException {
        Writer sw = new StringWriter();
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(o, sw);
        System.out.println(sw);
        System.out.flush();
    }

    public static String normalizePath(String path) {
        Path p = new Path(path);
        return p.toString();
    }

    public static String getParentPath(String path) {
        Path p = new Path(path);
        path = p.removeLastSegments(1).toString();

        // Ensures that path starts with a "/" and doesn't end with a "/".
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    public static String getNameFromPath(String path) {
        Path p = new Path(path);
        return p.lastSegment();
    }

    public static String getTokenFromHeaders(String headerName, HttpServletRequest request) {
        String header = request.getHeader(headerName);
        if (header == null) {
            return null;
        }
        String token = header.trim();
        int tokenStart = token.indexOf("<urn:uuid:");
        token = token.substring(tokenStart + "<urn:uuid:".length(), token.length());
        int tokenEnd = token.indexOf(">");
        token = token.substring(0, tokenEnd);
        return token;
    }

    public static String encode(byte[] bytes, String encoding) throws ClientException {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ClientException("Unsupported encoding " + encoding);
        }
    }

}
