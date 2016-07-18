/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Laurent Doguin
 *
 */

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.io.IOException;

import org.artofsolving.jodconverter.OfficeDocumentConverter;

/**
 * OOoManagerService can either start or stop OpenOffice pool server and return an OfficeDocumentConverter.
 *
 * @deprecated Since 8.4. Use 'soffice' with {@link org.nuxeo.ecm.platform.convert.plugins.CommandLineConverter} instead
 */
@Deprecated
public interface OOoManagerService {

    OfficeDocumentConverter getDocumentConverter();

    void stopOOoManager();

    void startOOoManager() throws IOException;

    boolean isOOoManagerStarted();

}
