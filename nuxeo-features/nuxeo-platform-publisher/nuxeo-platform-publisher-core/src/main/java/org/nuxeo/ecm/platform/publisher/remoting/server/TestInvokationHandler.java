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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;
import org.nuxeo.runtime.api.Framework;

import java.util.List;
import java.util.Map;

/**
 * {@link PublicationInvokationHandler} implementation. Could be called by a Restlet, a WebEngine module or a
 * TestInvoker
 *
 * @author tiry
 */
public class TestInvokationHandler implements PublicationInvokationHandler {

    protected RemotePublisherMarshaler marshaler;

    public TestInvokationHandler(RemotePublisherMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public void init(RemotePublisherMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public String invoke(String methodName, String data) {

        // XXX Err management !

        RemotePublicationTreeManager tm = Framework.getLocalService(RemotePublicationTreeManager.class);

        List<Object> params = marshaler.unMarshallParameters(data);

        if ("getChildrenDocuments".equals(methodName)) {
            return marshaler.marshallResult(tm.getChildrenDocuments((PublicationNode) params.get(0)));
        } else if ("getChildrenNodes".equals(methodName)) {
            return marshaler.marshallResult(tm.getChildrenNodes((PublicationNode) params.get(0)));
        } else if ("getExistingPublishedDocument".equals(methodName)) {
            return marshaler.marshallResult(
                    tm.getExistingPublishedDocument((String) params.get(0), (DocumentLocation) params.get(1)));
        } else if ("getNodeByPath".equals(methodName)) {
            return marshaler.marshallResult(tm.getNodeByPath((String) params.get(0), (String) params.get(1)));
        } else if ("getParent".equals(methodName)) {
            return marshaler.marshallResult(tm.getParent((PublicationNode) params.get(0)));
        } else if ("getPublishedDocumentInNode".equals(methodName)) {
            return marshaler.marshallResult(tm.getPublishedDocumentInNode((PublicationNode) params.get(0)));
        } else if ("initRemoteSession".equals(methodName)) {
            return marshaler.marshallResult(
                    tm.initRemoteSession((String) params.get(0), (Map<String, String>) params.get(1)));
        } else if ("release".equals(methodName)) {
            tm.release((String) params.get(0));
            return null;
        } else if ("publish".equals(methodName)) {
            if (params.size() == 2 || params.get(2) == null) {
                return marshaler.marshallResult(
                        tm.publish((DocumentModel) params.get(0), (PublicationNode) params.get(1)));
            } else {
                return marshaler.marshallResult(tm.publish((DocumentModel) params.get(0),
                        (PublicationNode) params.get(1), (Map<String, String>) params.get(2)));
            }
        } else if ("unpublish".equals(methodName)) {
            if (params.get(0) instanceof DocumentModel) {
                tm.unpublish((DocumentModel) params.get(0), (PublicationNode) params.get(1));
            } else if (params.get(1) instanceof PublishedDocument) {
                tm.unpublish((String) params.get(0), (PublishedDocument) params.get(1));
            }
            return null;
        } else {
            throw new NuxeoException("Unable to handle unknown method " + methodName);
        }
    }

}
