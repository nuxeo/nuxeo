/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.NullOutputStream;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.services.streaming.FileSource;

/**
 * A simple filesystem-based binary manager. It stores the binaries according to
 * their digest (hash), which means that no transactional behavior needs to be
 * implemented.
 * <p>
 * A garbage collection is needed to purge unused binaries.
 * <p>
 * The format of the <em>binaries</em> directory is:
 * <ul>
 * <li><em>data/</em> hierarchy with the actual binaries in subdirectories,</li>
 * <li><em>tmp/</em> temporary storage during creation,</li>
 * <li><em>config.xml</em> a file containing the configuration used.</li>
 * </ul>
 * 
 * This class includes optimizations that make it unsuitable for use with a
 * binary scrambler. Extend {@link LocalBinaryManager} instead to make use of a
 * scrambler.
 * 
 * @author Florent Guillaume
 */
public class DefaultBinaryManager extends LocalBinaryManager implements
        BinaryManagerStreamSupport {

    public DefaultBinaryManager() {
        super();
        if (!(getBinaryScrambler() instanceof NullBinaryScrambler)) {
            throw new IllegalStateException(
                    "DefaultBinaryManager cannot be used with a binary scrambler");
        }
    }

    @Override
    public Binary getBinary(FileSource source) throws IOException {
        String  digest = storeAndDigest(source);

        File file = getFileForDigest(digest, false);
        /*
         * Now we can build the Binary.
         */
        return getBinaryScrambler().getUnscrambledBinary(file, digest,
                repositoryName);
    }

    protected String storeAndDigest(FileSource source) throws IOException {
        File sourceFile = source.getFile();
        InputStream in = source.getStream();
        OutputStream out = new NullOutputStream();
        String digest;
        try {
            digest = storeAndDigest(in, out);
        } finally {
            in.close();
            out.close();
        }
        File digestFile = getFileForDigest(digest, true);
        if (!sourceFile.renameTo(digestFile)) {
            FileUtils.copy(sourceFile, digestFile);
            sourceFile.delete();
        }
        source.setFile(digestFile);
        return digest;
    }

}
