/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.nuxeo.ecm.platform.ui.web.multipart;

import java.io.IOException;
import java.io.InputStream;

public class ByteSequenceMatcher {

	public interface BytesHandler {

		void handle(byte[] bytes, int length) throws IOException;

	}

	private static final int ZERO_READS_NUMBER = 20;

	private final byte[] buffer;

	private int readLength = 0;

	private int zeroReadCounter = ZERO_READS_NUMBER;

	private boolean bufferEOF = false;

	private boolean isEOF = false;

	private boolean isMatched = false;

	private final InputStream inputStream;

	private BytesHandler bytesHandler;

	public ByteSequenceMatcher(InputStream inputStream, int bufferSize) {
		this.inputStream = inputStream;
        buffer = new byte[bufferSize];
	}

	public BytesHandler getBytesHandler() {
		return bytesHandler;
	}

	public void setBytesHandler(BytesHandler bytesHandler) {
		this.bytesHandler = bytesHandler;
	}

	protected void prefillBuffer(int usedLength) throws IOException {
		if (usedLength > readLength) {
			throw new IllegalArgumentException();
		}

		if (usedLength != readLength && usedLength != 0) {
			System.arraycopy(buffer, usedLength, buffer, 0, readLength - usedLength);
		}

		readLength -= usedLength;

		int remaining;

		while (!bufferEOF && (remaining = (buffer.length - readLength)) > 0) {
			int read = inputStream.read(buffer, readLength, remaining);

			if (read > 0) {
				readLength += read;
			} else if (read == 0) {
				--zeroReadCounter;

				if (zeroReadCounter == 0) {
					throw new IllegalStateException("Maximum number of zero reads reached");
				}
			} else if (read < 0) {
				bufferEOF = true;
			}
		}
	}

	private boolean match(int startOffset, byte[]...sequences) {
		int index = startOffset;

		for (byte[] bs : sequences) {
			for (byte b : bs) {

				if (index >= readLength) {
					return false;
				}

				if (buffer[index] != b) {
					return false;
				}

				index++;
			}
		}

		return true;
	}

	public void findSequence(int limit, byte[]... sequences) throws IOException {
		isMatched = false;

		int userLimit = limit;
		if (userLimit <= 0) {
			userLimit = Integer.MAX_VALUE;
		}

		prefillBuffer(0);

		int sequencesLength = 0;
		for (byte[] bs : sequences) {
			sequencesLength += bs.length;
		}

		int i = 0;

		while (!isMatched && i <= readLength - sequencesLength) {
			if (match(i, sequences)) {
				isMatched = true;
				bytesHandler.handle(buffer, i);
				prefillBuffer(i + sequencesLength);
			} else {
				int sequenceLimit = readLength - sequencesLength + 1;
				int realLimit;

				if (sequenceLimit < userLimit) {
					realLimit = sequenceLimit;
				} else {
					realLimit = userLimit;
				}

				if (realLimit > 0 && i == realLimit - 1) {
					//report limit
					bytesHandler.handle(buffer, realLimit);
					prefillBuffer(realLimit);

					i = 0;
				} else {
					i++;
				}
			}
		}

		if (!isMatched) {
			if (readLength > 0) {
				bytesHandler.handle(buffer, readLength);
				prefillBuffer(readLength);
			}
		}

		if (readLength == 0) {
            isEOF = true;
		}
	}

	public boolean isEOF() {
		return isEOF;
	}

	public boolean isMatched() {
		return isMatched;
	}

	public boolean isMatchedAndNotEOF() {
		return isMatched && !isEOF;
	}
}
