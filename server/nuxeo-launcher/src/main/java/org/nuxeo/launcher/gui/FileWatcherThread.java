/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.launcher.gui;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * @since 2023.0
 */
public class FileWatcherThread extends Thread {

    protected final Path file;

    protected final Runnable runnable;

    public FileWatcherThread(Path file, Runnable runnable) {
        this.file = file;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path parent = getFirstExistingParent();
            parent.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
            while (true) {
                WatchKey key = watcher.take();
                if (key == null) {
                    continue;
                }
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    // context returns the filename
                    var updatedEntry = dir.resolve((Path) event.context());
                    // handle the case where a parent didn't exist
                    if (kind == ENTRY_CREATE && file.startsWith(updatedEntry) && Files.isDirectory(updatedEntry)) {
                        updatedEntry.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                    } else if ((kind == ENTRY_CREATE || kind == ENTRY_MODIFY) && updatedEntry.equals(file)) {
                        runnable.run();
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            interrupt();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error while watching the configuration file", e);
        } catch (Exception e) {
            throw new RuntimeException("test", e);
        }
    }

    protected Path getFirstExistingParent() {
        var parent = file.getParent();
        while (!Files.exists(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }
}
