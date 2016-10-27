/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *     mguillaume
 *     jcarsique
 *     Yannis JULIENNE
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
        element("require-terms-and-conditions-acceptance", Boolean.valueOf(def.requireTermsAndConditionsAcceptance())
                                                                  .toString());
        element("production-state", def.getProductionState().toString());
        element("nuxeo-validation", def.getValidationState().toString());

        if (def.getInstaller() != null) {
            start("installer");
            attr("class", def.getInstaller().getType());
            attr("restart", String.valueOf(def.getInstaller().getRequireRestart()));
            end();
        }
        if (def.getUninstaller() != null) {
            start("uninstaller");
            attr("class", def.getUninstaller().getType());
            attr("restart", String.valueOf(def.getUninstaller().getRequireRestart()));
            end();
        }
        element("validator", def.getValidator());

        try {
            def.getClass().getMethod("getVisibility");
            if (def.getVisibility() != null) {
                element("visibility", def.getVisibility().toString());
            }
        } catch (NoSuchMethodException e) {
            // Ignore visibility with old Connect Client versions
        }

        if (def.getTargetPlatforms() != null && def.getTargetPlatforms().length > 0) {
            start("platforms");
            startContent();
            for (String platform : def.getTargetPlatforms()) {
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

        if (def.getOptionalDependencies() != null && def.getOptionalDependencies().length > 0) {
            start("optional-dependencies");
            startContent();
            for (PackageDependency dep : def.getOptionalDependencies()) {
                element("package", dep.toString());
            }
            end("optional-dependencies");
        }

        try {
            def.getClass().getMethod("getConflicts");
            if (def.getConflicts() != null && def.getConflicts().length > 0) {
                start("conflicts");
                startContent();
                for (PackageDependency conflict : def.getConflicts()) {
                    element("package", conflict.toString());
                }
                end("conflicts");
            }
        } catch (NoSuchMethodException e) {
            // Ignore conflicts with old Connect Client versions
        }

        try {
            def.getClass().getMethod("getProvides");
            if (def.getProvides() != null && def.getProvides().length > 0) {
                start("provides");
                startContent();
                for (PackageDependency provide : def.getProvides()) {
                    element("package", provide.toString());
                }
                end("provides");
            }
        } catch (NoSuchMethodException e) {
            // Ignore provides with old Connect Client versions
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
