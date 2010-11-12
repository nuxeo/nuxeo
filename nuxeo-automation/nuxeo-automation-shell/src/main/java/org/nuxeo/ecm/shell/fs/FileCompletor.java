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
package org.nuxeo.ecm.shell.fs;

import java.io.File;
import java.util.Collections;
import java.util.List;

import jline.Completor;
import jline.FileNameCompletor;

import org.nuxeo.ecm.shell.Shell;

/**
 * This is a modified {@link FileNameCompletor} to take into account the current
 * working directory
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class FileCompletor implements Completor {
    @SuppressWarnings("rawtypes")
    public int complete(final String buf, final int cursor,
            final List candidates) {
        String buffer = (buf == null) ? "" : buf;

        String translated = buffer;

        // special character: ~ maps to the user's home directory
        if (translated.startsWith("~" + File.separator)) {
            translated = System.getProperty("user.home")
                    + translated.substring(1);
        } else if (translated.startsWith("~")) {
            translated = new File(System.getProperty("user.home")).getParentFile().getAbsolutePath();
        } else if (!(translated.startsWith(File.separator))) {
            File wd = Shell.get().getContextObject(FileSystem.class).pwd();
            translated = wd.getAbsolutePath() + File.separator + translated;
        }

        File f = new File(translated);

        final File dir;

        if (translated.endsWith(File.separator)) {
            dir = f;
        } else {
            dir = f.getParentFile();
        }

        final File[] entries = (dir == null) ? new File[0] : dir.listFiles();

        try {
            return matchFiles(buffer, translated, entries, candidates);
        } finally {
            // we want to output a sorted list of files
            sortFileNames(candidates);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void sortFileNames(final List fileNames) {
        Collections.sort(fileNames);
    }

    /**
     * Match the specified <i>buffer</i> to the array of <i>entries</i> and
     * enter the matches into the list of <i>candidates</i>. This method can be
     * overridden in a subclass that wants to do more sophisticated file name
     * completion.
     * 
     * @param buffer the untranslated buffer
     * @param translated the buffer with common characters replaced
     * @param entries the list of files to match
     * @param candidates the list of candidates to populate
     * 
     * @return the offset of the match
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int matchFiles(String buffer, String translated, File[] entries,
            List candidates) {
        if (entries == null) {
            return -1;
        }

        int matches = 0;

        // first pass: just count the matches
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getAbsolutePath().startsWith(translated)) {
                matches++;
            }
        }

        // green - executable
        // blue - directory
        // red - compressed
        // cyan - symlink
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getAbsolutePath().startsWith(translated)) {
                String name = entries[i].getName()
                        + (((matches == 1) && entries[i].isDirectory()) ? File.separator
                                : " ");

                /*
                 * if (entries [i].isDirectory ()) { name = new ANSIBuffer
                 * ().blue (name).toString (); }
                 */
                candidates.add(name);
            }
        }

        final int index = buffer.lastIndexOf(File.separator);

        return index + File.separator.length();
    }
}
