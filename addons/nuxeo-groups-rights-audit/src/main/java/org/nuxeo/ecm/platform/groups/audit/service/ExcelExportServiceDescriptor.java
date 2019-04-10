/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
            template = File.createTempFile("ExcelTemplate", ".xls");
            template.createNewFile();
            FileUtils.copyURLToFile(templateUrl, template);
        } catch (IOException e) {
        }
    }

    public ExcelExportFactory getFactory() throws InstantiationException, IllegalAccessException {
        if (factoryClass != null) {
            return (ExcelExportFactory) factoryClass.newInstance();
        }
        return null;
    }

}
