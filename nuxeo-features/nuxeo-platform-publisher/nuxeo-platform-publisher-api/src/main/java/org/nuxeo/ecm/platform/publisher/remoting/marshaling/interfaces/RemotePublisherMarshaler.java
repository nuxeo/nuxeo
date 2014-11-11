/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.core.api.CoreSession;

import java.util.List;
import java.util.Map;

/**
 * Interface for the Marshaller.
 *
 * @author tiry
 */
public interface RemotePublisherMarshaler {

    String marshallParameters(List<Object> params)
            throws PublishingMarshalingException;

    List<Object> unMarshallParameters(String data)
            throws PublishingMarshalingException;

    String marshallResult(Object result) throws PublishingMarshalingException;

    Object unMarshallResult(String data) throws PublishingMarshalingException;

    void setAssociatedCoreSession(CoreSession session);

    void setParameters(Map<String, String> params);

}
