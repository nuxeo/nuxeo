/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;

/**
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NXCoreAppExample extends NXCoreApplication {

    public NXCoreAppExample() {
        super("demo"); // the repository name as specified in the repository XML
    }

    public static void main(String[] args) {
        try {
            run(new NXCoreAppExample());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void deployAll() {
        super.deployAll();
        // deploy the repository definition
        deploy("CustomTypes.xml");
        deploy("ExampleRepository.xml");
    }

    protected void run() throws Exception {
        Session session = repository.getSession(null);
        Document root = session.getRootDocument();
        root.setString("title", "The Root"); // use prefixed name to avoid collisions. Ex: dc:title instead of title

        Document child = root.addChild("myDoc", "MyFile");

        // ------------ SETTING SIMPLE PROPERTIES -----------------
        // use prefixed name to avoid collisions. Ex: dc:title instead of title
        child.setString("title", "The Doc");
        // or use explicit access to property
        child.getProperty("description").setValue("the desc");

        // ------------ SETTING BLOB PROPERTIES -----------------
        // use FileBlob to set content read from files, or InpuStreamBlob to set content read from input streams
        child.setContent("content", new StringBlob("my blob", "text/plain"));

        // ------------ SETTING COMPLEX PROPERTY -----------------
        Map<String, Object> value = new HashMap<String, Object>();
        value.put("abonne", "value1");
        value.put("support_diffusion", "value2");
        Property ab = child.getProperty("abonnement"); // the property is returned even if it not exists
        ab.setValue(value);

        // ------------ SETTING COMPLEX LIST PROPERTY -----------------
        List<Object> list = new ArrayList<Object>();
        value = new HashMap<String, Object>();
        value.put("abonne", "value11");
        value.put("support_diffusion", "value21");
        list.add(value);
        value = new HashMap<String, Object>();
        value.put("abonne", "value12");
        value.put("support_diffusion", "value22");
        list.add(value);
        ab = child.getProperty("abonnements"); // the property is returned even if it not exists
        ab.setValue(list);


        // --------- SETTING SIMPLE LIST PROPERTY ----------
        child.getProperty("authors").setValue(new String[] {"me", "you"}); // it is also working using java.util.List

        // save root document
        root.save();

        root = session.getRootDocument();
        child = root.getChild("myDoc");

        // ------------ SIMPLE PROPERTY RETRIEVAL -----------------
        System.out.println(">> child title:" + child.getString("title"));
        // or using explicit access
        System.out.println(">> child desc:" + child.getProperty("description").getValue());

        // ------------ BLOB PROPERTY RETRIEVAL -----------------
        System.out.println(">> child content:" + child.getContent("content").getString());
        // or use explicit property access
        Blob content = (Blob) child.getProperty("content").getValue();
        System.out.println(">> child content:" + content.getString());

        // -------------- COMPLEX PROPERTY RETRIEVAL ------------
        ab = child.getProperty("abonnement");
        Map abVal = (Map) ab.getValue();
        System.out.println(">> child abonnement.abonne:" + abVal.get("abonne"));
        System.out.println(">> child abonnement.support_diffusion:"
                + abVal.get("support_diffusion"));

        // -------------- COMPLEX LIST PROPERTY RETRIEVAL ------------
        ab = child.getProperty("abonnements");
        list = (List<Object>) ab.getValue();
        abVal = (Map) list.get(0);
        System.out.println(">> 0: child abonnement.abonne:"
                + abVal.get("abonne"));
        System.out.println(">> 0: child abonnement.support_diffusion:"
                + abVal.get("support_diffusion"));
        abVal = (Map) list.get(1);
        System.out.println(">> 1: child abonnement.abonne:"
                + abVal.get("abonne"));
        System.out.println(">> 1: child abonnement.support_diffusion:"
                + abVal.get("support_diffusion"));

        // -------------- SIMPLE LIST PROPERTY RETRIEVAL ------------
        ab = child.getProperty("authors");
        String[] ar = (String[]) ab.getValue();
        System.out.println(">> authors: " + ar[0] + ", " + ar[1]);

        child.remove();

        root.save();

        // close() will also save the session (use session.dispose() to close
        // without saving)
        session.close();
    }

}
