/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.xml;

import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.model.Field;
import org.nuxeo.connect.update.model.Form;
import org.nuxeo.connect.update.model.PackageDefinition;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XmlSerializer extends XmlWriter {

    public XmlSerializer() {
    }

    public XmlSerializer(String tab) {
        super(tab);
    }

    public String toXML(PackageDefinition def) {
        start("package");
        if (def.getType() != null) {
            attr("type", def.getType().getValue());
        }
        attr("name", def.getName());
        if (def.getVersion() != null) {
            attr("version", def.getVersion().toString());
        }
        startContent();

        element("title", def.getTitle());
        element("description", def.getDescription());
        element("vendor", def.getVendor());
        element("classifier", def.getClassifier());
        element("home-page", def.getHomePage());

        element("hotreload-support", Boolean.valueOf(def.supportsHotReload()).toString());
        element("supported", Boolean.valueOf(def.isSupported()).toString());
        element("require-terms-and-conditions-acceptance", Boolean.valueOf(def.requireTermsAndConditionsAcceptance()).toString());
        element("production-state", def.getProductionState().toString());
        element("nuxeo-validation", def.getValidationState().toString());

        if (def.getInstaller() != null) {
            start("installer");
            attr("class", def.getInstaller().getType());
            attr("restart",
                    String.valueOf(def.getInstaller().getRequireRestart()));
            end();
        }
        if (def.getUninstaller() != null) {
            start("uninstaller");
            attr("class", def.getUninstaller().getType());
            attr("restart",
                    String.valueOf(def.getUninstaller().getRequireRestart()));
            end();
        }
        element("validator", def.getValidator());

        if (def.getPlatforms() != null && def.getPlatforms().length > 0) {
            start("platforms");
            startContent();
            for (String platform : def.getPlatforms()) {
                element("platform", platform);
            }
            end("platforms");
        }

        if (def.getDependencies() != null && def.getDependencies().length > 0) {
            start("dependencies");
            startContent();
            for (PackageDependency dep : def.getDependencies()) {
                element("package", dep.toString());
            }
            end("dependencies");
        }

        end("package");
        return sb.toString();
    }

    public void buildXML(Form form) {
        start("form");
        startContent();
        element("title", form.getTitle());
        element("image", form.getImage());
        element("description", form.getDescription());
        if (form.getFields() != null && form.getFields().length > 0) {
            start("fields");
            startContent();
            for (Field field : form.getFields()) {
                start("field");
                attr("name", field.getName());
                attr("type", field.getType());
                if (field.isRequired()) {
                    attr("required", "true");
                }
                if (field.isReadOnly()) {
                    attr("readonly", "true");
                }
                if (field.isVertical()) {
                    attr("vertical", "true");
                }
                startContent();
                element("label", field.getLabel());
                element("value", field.getValue());
                end("field");
            }
            end("fields");
        }
        end("form");
    }

    public String toXML(FormDefinition form) {
        buildXML(form);
        return sb.toString();
    }

    public String toXML(FormDefinition... forms) {
        start("forms");
        startContent();
        for (FormDefinition form : forms) {
            buildXML(form);
        }
        end("forms");
        return sb.toString();
    }

    public String toXML(FormsDefinition forms) {
        start("forms");
        startContent();
        for (Form form : forms.getForms()) {
            buildXML(form);
        }
        end("forms");
        return sb.toString();
    }
}
