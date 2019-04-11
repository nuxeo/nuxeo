/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.importer.xml.parser.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.importer.xml.parser.AttributeConfigDescriptor;
import org.nuxeo.ecm.platform.importer.xml.parser.DocConfigDescriptor;
import org.nuxeo.ecm.platform.importer.xml.parser.ParserConfigRegistry;

/**
 * Hard coded config used for testing imported outside of service / extension point infrastructure
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class DummyRegistry implements ParserConfigRegistry {

    protected List<AttributeConfigDescriptor> attConfig = null;

    protected List<DocConfigDescriptor> docConfig = null;

    @Override
    public List<AttributeConfigDescriptor> getAttributConfigs() {
        if (attConfig == null) {
            attConfig = new ArrayList<>();

            attConfig.add(new AttributeConfigDescriptor("titre", "dc:title", "text()", null)); // use xpath
            attConfig.add(new AttributeConfigDescriptor("dossierActe", "dc:source",
                    "#{'Seance ' + currentDocument.name}", null)); // MVEL

            attConfig.add(new AttributeConfigDescriptor("document", "dc:title", "@nom", null));
            attConfig.add(new AttributeConfigDescriptor("document", "dc:source", "@type", null));

            attConfig.add(new AttributeConfigDescriptor("signature", "dc:format", "@formatSignature", null));

            Map<String, String> complex = new HashMap<>();
            complex.put("filename", "@nom");
            complex.put("mimetype", "mimetype/text()");
            complex.put("content", "@nom");

            attConfig.add(new AttributeConfigDescriptor("document", "file:content", complex, null));

        }

        return attConfig;
    }

    @Override
    public List<DocConfigDescriptor> getDocCreationConfigs() {
        if (docConfig == null) {
            docConfig = new ArrayList<>();
            docConfig.add(new DocConfigDescriptor("seance", "Workspace", null, "@idSeance")); // pure xpath

            String findParent = "#{" + "nodes = currentElement.selectNodes('@refSeance');" + "if (nodes.size()>0) {"
                    + "  String seanceRef = nodes.get(0).getText();"
                    + "  String parentRef = '//seance[@idSeance=\"' + seanceRef + '\"]';"
                    + "  return xml.selectNodes(parentRef).get(0);" + " } else {" + "  return root.getPathAsString();"
                    + " }" + "}";

            // xpath resolution inside String + complex MVEL Parent resolution
            docConfig.add(new DocConfigDescriptor("dossierActe", "Folder", findParent, "Acte-{{@idActe}}"));
            docConfig.add(new DocConfigDescriptor("document", "File", "..", "@nom"));
        }
        return docConfig;
    }
}
