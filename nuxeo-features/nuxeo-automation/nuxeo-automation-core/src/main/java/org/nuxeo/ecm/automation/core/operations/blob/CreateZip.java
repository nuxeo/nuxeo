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
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.ZipOutputStream;

import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.FileCleanupHandler;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * TODO: detect mine?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateZip.ID, category = Constants.CAT_BLOB, label = "Zip", description = "Creates a zip file from the input file(s). If no file name is given, the first file name in the input will be used. Returns the zip file.")
public class CreateZip {

    public static final String ID = "Blob.CreateZip";

    @Context
    protected OperationContext ctx;

    @Param(name = "filename", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        if (fileName == null || (fileName = fileName.trim()).length() == 0) {
            fileName = blob.getFilename();
        }
        File file = File.createTempFile("nxops-createzip-", ".tmp");
        ctx.addCleanupHandler(new FileCleanupHandler(file));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            zip(blob, out);
        } finally {
            out.finish();
            out.close();
        }
        return new FileBlob(file, "application/zip", null, fileName, null);
    }

    @OperationMethod
    public Blob run(BlobList blobs) throws Exception {
        if (fileName == null || (fileName = fileName.trim()).length() == 0) {
            fileName = blobs.isEmpty() ? null : blobs.get(0).getFilename();
        }
        File file = File.createTempFile("nxops-createzip-", ".tmp");
        ctx.addCleanupHandler(new FileCleanupHandler(file));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            zip(blobs, out);
        } finally {
            out.finish();
            out.close();
        }
        return new FileBlob(file, "application/zip", null, fileName, null);
    }

    protected String getFileName(Blob blob) {
        String entry = blob.getFilename();
        if (entry == null) {
            entry = "Unknown_" + System.identityHashCode(blob);
        }
        return entry;
    }

    protected void zip(Blob blob, ZipOutputStream out) throws Exception {
        String entry = getFileName(blob);
        InputStream in = blob.getStream();
        try {
            ZipUtils._zip(entry, in, out);
        } finally {
            in.close();
        }
    }

    protected void zip(BlobList blobs, ZipOutputStream out) throws Exception {
        // use a set to avoid zipping entries with same names
        Collection<String> names = new HashSet<String>();
        int cnt = 1;
        for (Blob blob : blobs) {
            String entry = getFileName(blob);
            if (!names.add(entry)) {
                entry = "renamed_" + (cnt++) + "_" + entry;
            }
            InputStream in = blob.getStream();
            try {
                ZipUtils._zip(entry, in, out);
            } finally {
                in.close();
            }
        }
    }

}
