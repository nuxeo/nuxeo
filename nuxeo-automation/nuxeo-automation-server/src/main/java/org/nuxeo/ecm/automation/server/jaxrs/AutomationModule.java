/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.automation.jaxrs.JsonFactoryProvider;
import org.nuxeo.ecm.automation.jaxrs.io.JsonAdapterWriter;
import org.nuxeo.ecm.automation.jaxrs.io.JsonExceptionWriter;
import org.nuxeo.ecm.automation.jaxrs.io.JsonLoginInfoWriter;
import org.nuxeo.ecm.automation.jaxrs.io.JsonRecordSetWriter;
import org.nuxeo.ecm.automation.jaxrs.io.JsonTreeWriter;
import org.nuxeo.ecm.automation.jaxrs.io.audit.LogEntryListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.audit.LogEntryWriter;
import org.nuxeo.ecm.automation.jaxrs.io.directory.DirectoryEntriesWriter;
import org.nuxeo.ecm.automation.jaxrs.io.directory.DirectoryEntryReader;
import org.nuxeo.ecm.automation.jaxrs.io.directory.DirectoryEntryWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.ACPWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.BlobsWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.BusinessAdapterReader;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JSONDocumentModelReader;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.JsonAutomationInfoWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.JsonOperationWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.JsonRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.operations.UrlEncodedFormRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.usermanager.NuxeoGroupListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.usermanager.NuxeoGroupReader;
import org.nuxeo.ecm.automation.jaxrs.io.usermanager.NuxeoGroupWriter;
import org.nuxeo.ecm.automation.jaxrs.io.usermanager.NuxeoPrincipalListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.usermanager.NuxeoPrincipalReader;
import org.nuxeo.ecm.automation.jaxrs.io.usermanager.NuxeoPrincipalWriter;
import org.nuxeo.ecm.webengine.app.WebEngineModule;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartRequestReader.class);
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    protected static Set<Object> setupSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new JsonRequestReader());
        result.add(new JsonExceptionWriter());
        result.add(new JsonAutomationInfoWriter());
        result.add(new JsonDocumentWriter());
        result.add(new JsonDocumentListWriter());
        result.add(new BlobsWriter());
        result.add(new JsonLoginInfoWriter());
        result.add(new JsonOperationWriter());
        result.add(new UrlEncodedFormRequestReader());
        result.add(new JsonTreeWriter());
        result.add(new JsonAdapterWriter());
        result.add(new JsonRecordSetWriter());
        result.add(new BusinessAdapterReader());
        result.add(new JSONDocumentModelReader());
        result.add(new NuxeoPrincipalWriter());
        result.add(new NuxeoPrincipalReader());
        result.add(new NuxeoGroupWriter());
        result.add(new NuxeoGroupReader());
        result.add(new NuxeoGroupListWriter());
        result.add(new NuxeoPrincipalListWriter());
        result.add(new JsonFactoryProvider());
        result.add(new DirectoryEntriesWriter());
        result.add(new DirectoryEntryWriter());
        result.add(new DirectoryEntryReader());
        result.add(new ACPWriter());
        result.add(new LogEntryWriter());
        result.add(new LogEntryListWriter());
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        return setupSingletons();
    }

}
