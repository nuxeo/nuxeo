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

package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;
import org.nuxeo.runtime.api.Framework;

import java.util.List;
import java.util.Map;

/**
 * {@link PublicationInvokationHandler} implementation. Could be called by a
 * Restlet, a WebEngine module or a TestInvoker
 *
 * @author tiry
 */
public class TestInvokationHandler implements PublicationInvokationHandler {

    protected RemotePublisherMarshaler marshaler;

    public TestInvokationHandler() {
        this.marshaler = new DefaultMarshaler();
        this.marshaler.setAssociatedCoreSession(getCoreSession());
    }

    public TestInvokationHandler(RemotePublisherMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public void init(RemotePublisherMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    protected CoreSession getCoreSession() {
        // XXX !!!
        try {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            CoreSession coreSession = null;
            if (rm != null) {
                coreSession = rm.getDefaultRepository().open();
            }
            return coreSession;
        } catch (Exception e) {
            return null;
        }
    }

    public String invoke(String methodName, String data) throws ClientException {

        // XXX Err management !

        RemotePublicationTreeManager tm = Framework.getLocalService(RemotePublicationTreeManager.class);

        List<Object> params = marshaler.unMarshallParameters(data);

        try {
            if ("getChildrenDocuments".equals(methodName)) {
                return marshaler.marshallResult(tm.getChildrenDocuments((PublicationNode) params.get(0)));
            } else if ("getChildrenNodes".equals(methodName)) {
                return marshaler.marshallResult(tm.getChildrenNodes((PublicationNode) params.get(0)));
            } else if ("getExistingPublishedDocument".equals(methodName)) {
                return marshaler.marshallResult(tm.getExistingPublishedDocument(
                        (String) params.get(0),
                        (DocumentLocation) params.get(1)));
            } else if ("getNodeByPath".equals(methodName)) {
                return marshaler.marshallResult(tm.getNodeByPath(
                        (String) params.get(0), (String) params.get(1)));
            } else if ("getParent".equals(methodName)) {
                return marshaler.marshallResult(tm.getParent((PublicationNode) params.get(0)));
            } else if ("getPublishedDocumentInNode".equals(methodName)) {
                return marshaler.marshallResult(tm.getPublishedDocumentInNode((PublicationNode) params.get(0)));
            } else if ("initRemoteSession".equals(methodName)) {
                return marshaler.marshallResult(tm.initRemoteSession(
                        (String) params.get(0),
                        (Map<String, String>) params.get(1)));
            } else if ("release".equals(methodName)) {
                tm.release((String) params.get(0));
                return null;
            } else if ("publish".equals(methodName)) {
                if (params.size() == 2 || params.get(2) == null) {
                    return marshaler.marshallResult(tm.publish(
                            (DocumentModel) params.get(0),
                            (PublicationNode) params.get(1)));
                } else {
                    return marshaler.marshallResult(tm.publish(
                            (DocumentModel) params.get(0),
                            (PublicationNode) params.get(1),
                            (Map<String, String>) params.get(2)));
                }
            } else if ("unpublish".equals(methodName)) {
                if (params.get(0) instanceof DocumentModel) {
                    tm.unpublish((DocumentModel) params.get(0),
                            (PublicationNode) params.get(1));
                } else if (params.get(1) instanceof PublishedDocument) {
                    tm.unpublish((String) params.get(0),
                            (PublishedDocument) params.get(1));
                }
                return null;
            } else {
                throw new ClientException("Unable to handle unknown method "
                        + methodName);
            }

        } catch (Exception e) {
            throw new ClientException("Error during invocation of method "
                    + methodName, e);
        } finally {

        }
    }

}
