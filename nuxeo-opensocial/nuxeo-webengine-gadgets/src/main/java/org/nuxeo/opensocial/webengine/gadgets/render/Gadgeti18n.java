/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.webengine.gadgets.render;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.nuxeo.opensocial.gadgets.service.InternalGadgetDescriptor;

/**
 * Helper class that manages i18n files for gadgets.
 *
 * 2 modes are supported :
 *
 *  Static mode :
 *
 *  gadget has to statically include the Local headers and associated files
 *
 *  Dynamic mode :
 *  gadget only includes a dynamic_messages.properties
 *  This file is used to know what labels are used by the gadget (and provide default valude)
 *  The Headers in Gadgets specs and associated XML files will be dynamically generated using
 *  the message bundles used by Nuxeo app.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class Gadgeti18n {

    public static final String DYN_TRANSLATION_FILE = "dynamic_messages.properties";

    protected static final String EMPTY_I18N_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><messagebundle></messagebundle>";

    protected Map<String, Boolean> isDynamicTranslation = new HashMap<String, Boolean>();

    protected String[] langs = {"en","fr","de","it","es","pt","pl","eu","ru","ar","cn","ja","vn"};

    protected List<Locale> supportedLang;

    public InputStream getTranslationFile(InternalGadgetDescriptor gadget, String fileName) throws IOException {
        if (usesDynamicTranslation(gadget)) {
            return loadDynamicFile(gadget, fileName);
        } else {
            return gadget.getResourceAsStream(fileName);
        }
    }

    public List<Locale> getSupportedLangs() {
        if (supportedLang==null) {
            supportedLang = new ArrayList<Locale>();
            for (String lang : langs) {
                supportedLang.add(new Locale(lang));
            }
        }
        return supportedLang;
    }

    protected boolean usesDynamicTranslation(InternalGadgetDescriptor gadget) {
        Boolean isDynamic = isDynamicTranslation.get(gadget.getName());
        if (isDynamic==null) {
            try {
                InputStream dynDescriptor = gadget.getResourceAsStream(DYN_TRANSLATION_FILE);
                generateDynamicTranslations(gadget, dynDescriptor);
                isDynamic=true;
            } catch (IOException e) {
                isDynamic = false;
            }
            isDynamicTranslation.put(gadget.getName(), isDynamic);
        }
        return isDynamic;
    }

    protected File getDynamicFile(InternalGadgetDescriptor gadget, String fileName) throws IOException {

        File targetDirectory = new File (System.getProperty("java.io.tmpdir", "/tmp") + "/gadget-cache-" + gadget.getName());
        if (!targetDirectory.exists()) {
            targetDirectory.mkdir();
        }
        return new File(targetDirectory.getAbsolutePath() + "/" + fileName);
    }

    protected InputStream loadDynamicFile(InternalGadgetDescriptor gadget, String fileName) throws IOException {

        File resource = getDynamicFile(gadget, fileName);

        if (resource==null || ! resource.exists()) {
            return new ByteArrayInputStream(EMPTY_I18N_FILE.getBytes("UTF-8"));
        }
        return new FileInputStream(resource);
    }


    protected void generateDynamicTranslations(InternalGadgetDescriptor gadget, InputStream dynDescriptor) throws IOException {

        PropertyResourceBundle descriptor = new PropertyResourceBundle(dynDescriptor);

        for (Locale locale : getSupportedLangs()) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);

            File messageFile = getDynamicFile(gadget, "messages_" + locale.toString() + ".xml");
            Enumeration<String> keys = descriptor.getKeys();

            PrintWriter printer = new PrintWriter(new FileOutputStream(messageFile));

            try {
                printer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                printer.println("<messagebundle>");

                while (keys.hasMoreElements()) {

                    String key = keys.nextElement();
                    String value = null;
                    try {
                       value = bundle.getString(key);
                    }
                    catch (MissingResourceException e) {
                        value = descriptor.getString(key);
                    }
                    if (value!=null) {
                        printer.print("<msg name=\"");
                        printer.print(key);
                        printer.print("\">");
                        printer.print(value);
                        printer.println("</msg>");
                    }
                }
                printer.println("</messagebundle>");
            }
            finally {
                printer.close();
            }

        }
    }
}
