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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */

package org.nuxeo.ecm.platform.groups.audit.service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * XMap descriptor for contributed export service factory (and template).
 *
 * @since 5.7
 */
@XObject("ExcelExport")
public class ExcelExportServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@factoryClass")
    private Class<? extends ExcelExportFactory> factoryClass;

    private File template;

    public String getName() {
        return name;
    }

    public File getTemplate() {
        return template;
    }

    @XNode("@template")
    public void setTemplate(String templatePath) {
        URL templateUrl = ExcelExportServiceDescriptor.class.getResource("/" + templatePath);
        try {
            template = Framework.createTempFile("ExcelTemplate", ".xls");
            template.createNewFile();
            FileUtils.copyURLToFile(templateUrl, template);
        } catch (IOException e) {
        }
    }

    public ExcelExportFactory getFactory() {
        if (factoryClass != null) {
            try {
                return factoryClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
        }
        return null;
    }

}
