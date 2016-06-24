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

package org.nuxeo.ecm.webdav.jaxrs;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.java.dev.webdav.jaxrs.xml.conditions.CannotModifyProtectedProperty;
import net.java.dev.webdav.jaxrs.xml.conditions.LockTokenMatchesRequestUri;
import net.java.dev.webdav.jaxrs.xml.conditions.LockTokenSubmitted;
import net.java.dev.webdav.jaxrs.xml.conditions.NoConflictingLock;
import net.java.dev.webdav.jaxrs.xml.conditions.NoExternalEntities;
import net.java.dev.webdav.jaxrs.xml.conditions.PreservedLiveProperties;
import net.java.dev.webdav.jaxrs.xml.conditions.PropFindFiniteDepth;
import net.java.dev.webdav.jaxrs.xml.elements.ActiveLock;
import net.java.dev.webdav.jaxrs.xml.elements.AllProp;
import net.java.dev.webdav.jaxrs.xml.elements.Collection;
import net.java.dev.webdav.jaxrs.xml.elements.Depth;
import net.java.dev.webdav.jaxrs.xml.elements.Exclusive;
import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import net.java.dev.webdav.jaxrs.xml.elements.Include;
import net.java.dev.webdav.jaxrs.xml.elements.Location;
import net.java.dev.webdav.jaxrs.xml.elements.LockEntry;
import net.java.dev.webdav.jaxrs.xml.elements.LockInfo;
import net.java.dev.webdav.jaxrs.xml.elements.LockRoot;
import net.java.dev.webdav.jaxrs.xml.elements.LockScope;
import net.java.dev.webdav.jaxrs.xml.elements.LockToken;
import net.java.dev.webdav.jaxrs.xml.elements.LockType;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Owner;
import net.java.dev.webdav.jaxrs.xml.elements.Prop;
import net.java.dev.webdav.jaxrs.xml.elements.PropFind;
import net.java.dev.webdav.jaxrs.xml.elements.PropName;
import net.java.dev.webdav.jaxrs.xml.elements.PropStat;
import net.java.dev.webdav.jaxrs.xml.elements.PropertyUpdate;
import net.java.dev.webdav.jaxrs.xml.elements.Remove;
import net.java.dev.webdav.jaxrs.xml.elements.ResponseDescription;
import net.java.dev.webdav.jaxrs.xml.elements.Set;
import net.java.dev.webdav.jaxrs.xml.elements.Shared;
import net.java.dev.webdav.jaxrs.xml.elements.Status;
import net.java.dev.webdav.jaxrs.xml.elements.TimeOut;
import net.java.dev.webdav.jaxrs.xml.elements.Write;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.DisplayName;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLanguage;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLength;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentType;
import net.java.dev.webdav.jaxrs.xml.properties.GetETag;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import net.java.dev.webdav.jaxrs.xml.properties.ResourceType;
import net.java.dev.webdav.jaxrs.xml.properties.SupportedLock;

/**
 * Utility functions.
 */
public class Util {

    // volatile for double-checked locking
    private static volatile JAXBContext jaxbContext;

    private static final Object jaxbContextLock = new Object();

    private static JAXBContext initJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(new Class<?>[] { //
        ActiveLock.class, //
                AllProp.class, //
                CannotModifyProtectedProperty.class, //
                Collection.class, //
                CreationDate.class, //
                Depth.class, //
                DisplayName.class, //
                net.java.dev.webdav.jaxrs.xml.elements.Error.class, //
                Exclusive.class, //
                GetContentLanguage.class, //
                GetContentLength.class, //
                GetContentType.class, //
                GetETag.class, //
                GetLastModified.class, //
                HRef.class, //
                Include.class, //
                Location.class, //
                LockDiscovery.class, //
                LockEntry.class, //
                LockInfo.class, //
                LockRoot.class, //
                LockScope.class, //
                LockToken.class, //
                LockTokenMatchesRequestUri.class, //
                LockTokenSubmitted.class, //
                LockType.class, //
                MultiStatus.class, //
                NoConflictingLock.class, //
                NoExternalEntities.class, //
                Owner.class, //
                PreservedLiveProperties.class, //
                Prop.class, //
                PropertyUpdate.class, //
                PropFind.class, //
                PropFindFiniteDepth.class, //
                PropName.class, //
                PropStat.class, //
                Remove.class, //
                ResourceType.class, //
                Response.class, //
                ResponseDescription.class, //
                Set.class, //
                Shared.class, //
                Status.class, //
                SupportedLock.class, //
                TimeOut.class, //
                Write.class, //
                IsCollection.class, //
                IsFolder.class, //
                IsHidden.class, //
                Win32CreationTime.class, //
                Win32FileAttributes.class, //
                Win32LastAccessTime.class, //
                Win32LastModifiedTime.class, //
        });
    }

    public static JAXBContext getJaxbContext() throws JAXBException {
        if (jaxbContext == null) {
            synchronized (jaxbContextLock) {
                if (jaxbContext == null) {
                    jaxbContext = initJaxbContext();
                }
            }
        }
        return jaxbContext;
    }

    public static Unmarshaller getUnmarshaller() throws JAXBException {
        return getJaxbContext().createUnmarshaller();
    }

}
