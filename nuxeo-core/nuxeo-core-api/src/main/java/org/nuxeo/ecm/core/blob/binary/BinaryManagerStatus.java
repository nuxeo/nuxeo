/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob.binary;

/**
 * Status of a BinaryManager, including files that may have just been deleted by GC
 */
public class BinaryManagerStatus {

    public long gcDuration;

    public long numBinaries;

    public long sizeBinaries;

    public long numBinariesGC;

    public long sizeBinariesGC;

    /**
     * The GC duration, in milliseconds
     */
    public long getGCDuration() {
        return gcDuration;
    }

    /**
     * The number of binaries.
     */
    public long getNumBinaries() {
        return numBinaries;
    }

    /**
     * The cumulated size of the binaries.
     */
    public long getSizeBinaries() {
        return sizeBinaries;
    }

    /**
     * The number of garbage collected binaries.
     */
    public long getNumBinariesGC() {
        return numBinariesGC;
    }

    /**
     * The cumulated size of the garbage collected binaries.
     */
    public long getSizeBinariesGC() {
        return sizeBinariesGC;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BinaryManagerStatus [gcDuration=").append(gcDuration) //
        .append(", numBinaries=").append(numBinaries) //
        .append(", sizeBinaries=").append(sizeBinaries) //
        .append(", numBinariesGC=").append(numBinariesGC) //
        .append(", sizeBinariesGC=").append(sizeBinariesGC).append("]");
        return builder.toString();
    }

}
