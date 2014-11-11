/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.swing.tree;

import java.awt.Container;

import javax.swing.JDialog;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactTreeDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static ArtifactTreeDialog instance;
    public static ArtifactTreeDialog getInstance() {
        if (instance == null) {
            instance = new ArtifactTreeDialog();
        }
        return instance;
    }

    public ArtifactTreeDialog() {
        super();
        setTitle("Artifact Explorer");
        //setAlwaysOnTop(true);
        setContentPane(createContentPane());
        pack();
        setSize(800, 600);
    }

    protected Container createContentPane() {
        return new ArtifactTree();
    }

}
