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
 *     jcarsique
 *     Yannis JULIENNE
 */
package org.nuxeo.connect.update.task.standalone.commands;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileMatcher;
import org.nuxeo.common.utils.FileRef;
import org.nuxeo.common.utils.FileVersion;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 * Copy a file to the given target directory or file. If the target is a directory the file name is preserved. If the
 * target file exists it will be replaced if overwrite is true otherwise the command validation fails. If the source
 * file is a directory, then the files it contents will be recursively copied.
 * <p>
 * If md5 is set then the copy command will be validated only if the target file has the same md5 as the one specified
 * in the command.
 * <p>
 * The Copy command has as inverse either Delete either another Copy command. If the file was copied without overwriting
 * then Delete is the inverse (with a md5 set to the one of the copied file). If the file was overwritten then the
 * inverse of Copy command is another copy command with the md5 to the one of the copied file and the overwrite flag to
 * true. The file to copy will be the backup of the overwritten file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Copy extends AbstractCommand {

    protected static final Log log = LogFactory.getLog(Copy.class);

    public static final String ID = "copy";

    protected static final String LAUNCHER_JAR = "nuxeo-launcher.jar";

    protected static final String LAUNCHER_CHANGED_PROPERTY = "launcher.changed";

    /**
     * The source file. It can be a file or a directory.
     */
    protected File file;

    /**
     * The target file. It can be a directory since 5.5
     */
    protected File tofile;

    protected boolean overwrite;

    protected String md5;

    protected boolean removeOnExit;

    /**
     * @since 5.5
     */
    protected boolean append;

    /**
     * @since 5.5
     */
    private boolean overwriteIfNewerVersion;

    /**
     * @since 5.5
     */
    private boolean upgradeOnly;

    protected Copy(String id) {
        super(id);
    }

    public Copy() {
        this(ID);
    }

    public Copy(File file, File tofile, String md5, boolean overwrite) {
        this(ID, file, tofile, md5, overwrite, false);
    }

    public Copy(File file, File tofile, String md5, boolean overwrite, boolean removeOnExit) {
        this(ID, file, tofile, md5, overwrite, removeOnExit);
    }

    protected Copy(String id, File file, File tofile, String md5, boolean overwrite, boolean removeOnExit) {
        this(id);
        this.file = file;
        this.tofile = tofile;
        this.md5 = md5;
        this.overwrite = overwrite;
        this.removeOnExit = removeOnExit;
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        if (!file.exists()) {
            log.warn("Can't copy " + file + " . File missing.");
            return null;
        }
        return doCopy(task, prefs, file, tofile, overwrite);
    }

    /**
     * @param doOverwrite
     * @since 5.5
     */
    protected Command doCopy(Task task, Map<String, String> prefs, File fileToCopy, File dst, boolean doOverwrite)
            throws PackageException {
        String dstmd5;
        File bak = null;
        CompositeCommand rollbackCommand = new CompositeCommand();
        if (fileToCopy.isDirectory()) {
            if (fileToCopy != file) {
                dst = new File(dst, fileToCopy.getName());
            }
            dst.mkdirs();
            for (File childFile : fileToCopy.listFiles()) {
                rollbackCommand.addCommand(doCopy(task, prefs, childFile, dst, doOverwrite));
            }
            return rollbackCommand;
        }
        if (dst.isDirectory()) {
            dst = new File(dst, fileToCopy.getName());
        }
        try {
            FileMatcher filenameMatcher = FileMatcher.getMatcher("{n:.*-}[0-9]+.*\\.jar");
            boolean isVersionnedJarFile = filenameMatcher.match(fileToCopy.getName());
            if (isVersionnedJarFile) {
                log.warn(String.format(
                        "Use of the <copy /> command on JAR files is not recommended, prefer using <update /> command to ensure a safe rollback. (%s)",
                        fileToCopy.getName()));
            }
            if (isVersionnedJarFile && (overwriteIfNewerVersion || upgradeOnly)) {
                // Compare source and destination versions set in filename
                FileVersion fileToCopyVersion, dstVersion = null;
                String filenameWithoutVersion = filenameMatcher.getValue();
                FileMatcher versionMatcher = FileMatcher.getMatcher(filenameWithoutVersion + "{v:[0-9]+.*}\\.jar");
                // Get new file version
                if (versionMatcher.match(fileToCopy.getName())) {
                    fileToCopyVersion = new FileVersion(versionMatcher.getValue());
                    // Get original file name and version
                    File dir = dst.getParentFile();
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File f : list) {
                            if (versionMatcher.match(f.getName())) {
                                dst = f;
                                dstVersion = new FileVersion(versionMatcher.getValue());
                                break;
                            }
                        }
                    }
                    if (dstVersion == null) {
                        if (upgradeOnly) {
                            return null;
                        }
                    } else if (fileToCopyVersion.greaterThan(dstVersion)) {
                        // backup dst and generate rollback command
                        File oldDst = dst;
                        dst = new File(dst.getParentFile(), fileToCopy.getName());
                        File backup = IOUtils.backup(task.getPackage(), oldDst);
                        rollbackCommand.addCommand(new Copy(backup, oldDst, null, false));
                        // Delete old dst as its name differs from new version
                        oldDst.delete();
                    } else if (fileToCopyVersion.isSnapshot() && fileToCopyVersion.equals(dstVersion)) {
                        doOverwrite = true;
                    } else if (!doOverwrite) {
                        log.info("Ignore " + fileToCopy + " because not newer than " + dstVersion
                                + " and 'overwrite' is set to false.");
                        return null;
                    }
                }
            }
            if (dst.exists()) { // backup the destination file if exist.
                if (!doOverwrite && !append) { // force a rollback
                    throw new PackageException(
                            "Copy command has overwrite flag on false but destination file exists: " + dst);
                }
                if (task instanceof UninstallTask) {
                    // no backup for uninstall task
                } else if (append) {
                    bak = IOUtils.backup(task.getPackage(), fileToCopy);
                } else {
                    bak = IOUtils.backup(task.getPackage(), dst);
                }
            } else { // target file doesn't exists - it will be created
                dst.getParentFile().mkdirs();
            }

            // copy the file - use getContentToCopy to allow parameterization
            // for subclasses
            String content = getContentToCopy(fileToCopy, prefs);
            if (content != null) {
                if (append && dst.exists()) {
                    try (RandomAccessFile rfile = new RandomAccessFile(dst, "r")) {
                        rfile.seek(dst.length());
                        if (!"".equals(rfile.readLine())) {
                            content = System.getProperty("line.separator") + content;
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
                FileUtils.writeStringToFile(dst, content, UTF_8, append);
            } else {
                File tmp = new File(dst.getPath() + ".tmp");
                org.nuxeo.common.utils.FileUtils.copy(fileToCopy, tmp);
                if (!tmp.renameTo(dst)) {
                    tmp.delete();
                    org.nuxeo.common.utils.FileUtils.copy(fileToCopy, dst);
                }
            }
            // check whether the copied or restored file was the launcher
            if (dst.getName().equals(LAUNCHER_JAR) || fileToCopy.getName().equals(LAUNCHER_JAR)) {
                Environment env = Environment.getDefault();
                env.setProperty(LAUNCHER_CHANGED_PROPERTY, "true");
            }
            // get the md5 of the copied file.
            dstmd5 = IOUtils.createMd5(dst);
        } catch (IOException e) {
            throw new PackageException("Failed to copy " + fileToCopy, e);
        }
        if (bak == null) { // no file was replaced
            rollbackCommand.addCommand(new Delete(dst, dstmd5, removeOnExit));
        } else if (append) {
            rollbackCommand.addCommand(new UnAppend(bak, dst));
        } else {
            rollbackCommand.addCommand(new Copy(bak, dst, dstmd5, true));
        }
        return rollbackCommand;
    }

    /**
     * Override in subclass to parameterize content.
     *
     * @since 5.5
     * @param prefs
     * @return Content to put in destination file. See {@link #append} parameter to determine if returned content is
     *         replacing or appending to destination file.
     * @throws PackageException
     */
    protected String getContentToCopy(File fileToCopy, Map<String, String> prefs) throws PackageException {
        // For compliance
        String deprecatedContent = getContentToCopy(prefs);
        if (deprecatedContent != null) {
            return deprecatedContent;
        }
        if (append) {
            try {
                return FileUtils.readFileToString(fileToCopy, UTF_8);
            } catch (IOException e) {
                throw new PackageException("Couldn't read " + fileToCopy.getName(), e);
            }
        } else {
            return null;
        }
    }

    /**
     * @deprecated Since 5.5, use {@link #getContentToCopy(File, Map)}. This method is missing the fileToCopy reference.
     *             Using {@link #file} is leading to errors.
     * @throws PackageException
     */
    @Deprecated
    protected String getContentToCopy(Map<String, String> prefs) throws PackageException {
        return null;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status) throws PackageException {
        if (file == null || tofile == null) {
            status.addError("Cannot execute command in installer."
                    + " Invalid copy syntax: file, dir, tofile or todir was not specified.");
            return;
        }
        if (tofile.isFile() && !overwrite && !append) {
            if (removeOnExit) {
                // a plugin is still there due to a previous action that needs a
                // restart
                status.addError("A restart is needed to perform this operation: cleaning " + tofile.getName());
            } else {
                status.addError("Cannot overwrite existing file: " + tofile.getName());
            }
        }
        if (md5 != null) {
            try {
                if (tofile.isFile() && !md5.equals(IOUtils.createMd5(tofile))) {
                    status.addError("MD5 check failed. File: " + tofile + " has changed since its backup");
                }
            } catch (IOException e) {
                throw new PackageException(e);
            }
        }
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        boolean sourceIsDir = false;
        File dir = null;
        String v = element.getAttribute("dir");
        if (v.length() > 0) {
            dir = new File(v);
        }
        v = element.getAttribute("file");
        if (v.length() > 0) {
            if (dir != null) {
                file = new File(dir, v);
            } else {
                file = new File(v);
            }
            guardVars.put("file", file);
        } else {
            sourceIsDir = true;
            file = dir;
            guardVars.put("dir", dir);
        }

        v = element.getAttribute("todir");
        if (v.length() > 0) {
            if (sourceIsDir) {
                tofile = new File(v);
                guardVars.put("todir", tofile);
            } else {
                tofile = new File(v, file.getName());
                guardVars.put("tofile", tofile);
            }
        } else {
            v = element.getAttribute("tofile");
            if (v.length() > 0) {
                FileRef ref = FileRef.newFileRef(v);
                tofile = ref.getFile();
                guardVars.put("tofile", tofile);
                ref.fillPatternVariables(guardVars);
            }
        }

        v = element.getAttribute("md5");
        if (v.length() > 0) {
            md5 = v;
        }
        v = element.getAttribute("overwrite");
        if (v.length() > 0) {
            overwrite = Boolean.parseBoolean(v);
        }
        v = element.getAttribute("removeOnExit");
        if (v.length() > 0) {
            removeOnExit = Boolean.parseBoolean(v);
        }
        v = element.getAttribute("overwriteIfNewerVersion");
        if (v.length() > 0) {
            overwriteIfNewerVersion = Boolean.parseBoolean(v);
        }
        v = element.getAttribute("upgradeOnly");
        if (v.length() > 0) {
            upgradeOnly = Boolean.parseBoolean(v);
        }
        v = element.getAttribute("append");
        if (v.length() > 0) {
            append = Boolean.parseBoolean(v);
        }
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        if (tofile != null) {
            writer.attr("tofile", tofile.getAbsolutePath());
        }
        writer.attr("overwrite", String.valueOf(overwrite));
        if (md5 != null) {
            writer.attr("md5", md5);
        }
        if (removeOnExit) {
            writer.attr("removeOnExit", "true");
        }
        if (overwriteIfNewerVersion) {
            writer.attr("overwriteIfNewerVersion", "true");
        }
        if (upgradeOnly) {
            writer.attr("upgradeOnly", "true");
        }
        if (append) {
            writer.attr("append", "true");
        }
        writer.end();
    }

}
