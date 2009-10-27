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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.nuxeo.build.maven.EmbeddedMavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;
import org.nuxeo.build.util.IOUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactTree extends JSplitPane {

    private static final long serialVersionUID = 1L;

    protected JTree tree;
    DefaultMutableTreeNode root;
    protected EmbeddedMavenClient maven;

    protected ItemProvider provider = ItemProvider.DEFAULT;

    public ArtifactTree() {
        super (JSplitPane.VERTICAL_SPLIT, true);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initializeMaven();

        JPanel pane = new JPanel(new BorderLayout(3,3));

        JToolBar tbar = new JToolBar();
        final JComboBox presets = new JComboBox(new String[] {"Default", "Minimal"});
        presets.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ItemProvider provider;
                if ("Default".equals(presets.getSelectedItem())) {
                    provider = new DefaultNuxeoProvider();
                } else {
                    provider = new CleanNuxeoProvider();
                }
               setProvider(provider);
            }
        });
        tbar.add(presets);

        final JTextPane info = new JTextPane();
        info.setContentType("text/html");
        info.setEditable(false);
        //info.setPreferredSize(new Dimension(800, 80));
        info.setBorder(new EmptyBorder(5,5,5,5));
        info.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    final JFileChooser fc = new JFileChooser();
                    int r = fc.showSaveDialog(ArtifactTree.this);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        try {
                            IOUtils.copy(e.getURL(), file);
                        } catch (Exception ee) {
                            JOptionPane.showMessageDialog(ArtifactTree.this, "Unable to copy url to file: "+file, "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            ee.printStackTrace();
                        }
                    }
                }
            }
        });

        final JTextField addressBar = new JTextField();
        addressBar.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String text = addressBar.getText().trim();
            if (text.length() > 0) {
                try {
                    Node node = maven.getGraph().addRootNode(text);
                    DefaultMutableTreeNode graphRoot = new DefaultMutableTreeNode(node);
                    int len = root.getChildCount();
                    for (int i=0; i<len; i++) {
                        DefaultMutableTreeNode tn = (DefaultMutableTreeNode)root.getChildAt(i);
                        Node n = (Node)tn.getUserObject();
                        if (node.equals(n)) {
                            // select this node?
                            return;
                        }
                    }
                    graphRoot.setAllowsChildren(provider.hasChildren(node));
                    root.add(graphRoot);
                    refresh();
                    //tree.setR
                } catch (Exception ee) {
                    JOptionPane.showMessageDialog(ArtifactTree.this, "Unable to resolve artifact: "+text, "Error",
                            JOptionPane.ERROR_MESSAGE);
                    ee.printStackTrace();
                }
            }
        }
        });
        tbar.add(addressBar);

        root = new DefaultMutableTreeNode();
        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        // to expand all nodes (without checking isChildren on the node)
        ((DefaultTreeModel)tree.getModel()).setAsksAllowsChildren(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        //tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setCellRenderer(new ArtifactCellRenderer(this));

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                if (tn != null) {
                    Object o = tn.getUserObject();
                    if (o instanceof Node) {
                        Node node = (Node)tn.getUserObject();
                        info.setText(provider.getInfo(node));
                    }
                }
            }
        });
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent event) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                if (tn != null) {
                    Object o = tn.getUserObject();
                    if (o instanceof Node) {
                        Node node = (Node)tn.getUserObject();
                        info.setText(provider.getInfo(node));
                    }
                }
            }
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
            public void treeWillExpand(TreeExpansionEvent event)
                    throws ExpandVetoException {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                if (tn != null && tn.getChildCount() == 0) {
                    Node node = (Node)tn.getUserObject();
                    System.out.println("Lazy load node: "+node);
                    node.expand(1, null);
                    if (!node.getEdgesOut().isEmpty()) {
                        for (Edge edge : node.getEdgesOut()) {
                            if (provider.accept(edge)) {
                                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
                                treeNode.setAllowsChildren(provider.hasChildren(node));
                                tn.add(new DefaultMutableTreeNode(edge.dst));
                            }
                        }
                        refresh(tn);
                    }
                }
            }
        });


        pane.add(tbar, BorderLayout.PAGE_START);
        pane.add(new JScrollPane(tree), BorderLayout.CENTER);

        add(pane);
        add(new JScrollPane(info));

        setProvider(new DefaultNuxeoProvider());
    }


    public void refresh(TreeNode tn) {
        ((DefaultTreeModel)tree.getModel()).reload(tn);
    }

    public void refresh() {
        ((DefaultTreeModel)tree.getModel()).reload();
    }

    public void setProvider(ItemProvider provider) {
        this.provider = provider;
        root.removeAllChildren();
        String[] roots = provider.getRoots();
        if (roots != null) {
            for (String root : roots) {
                try {
                    Node node = maven.getGraph().addRootNode(root);
                    DefaultMutableTreeNode graphRoot = new DefaultMutableTreeNode(node);
                    this.root.add(graphRoot);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ArtifactTree.this, "Failed to resolve artifact: "+root, "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            refresh();
        }
    }

    public ItemProvider getProvider() {
        return provider;
    }

    public void initializeMaven() {
        maven = MavenClientFactory.getEmbeddedMaven();
        try {
            maven.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ArtifactTree.this, "Failed to start maven: "+e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        Repository repo = new Repository();
        repo.setId("nuxeo_public");
        repo.setName("Nuxeo Public Repository");
        repo.setLayout("default");
        repo.setUrl("http://maven.nuxeo.org/public");
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setReleases(policy);
        policy = new RepositoryPolicy();
        policy.setEnabled(false);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setSnapshots(policy);
        maven.addRemoteRepository(repo);

        repo = new Repository();
        repo.setId("nuxeo_public_snapshot");
        repo.setName("Nuxeo Public Snapshot Repository");
        repo.setLayout("default");
        repo.setUrl("http://maven.nuxeo.org/public-snapshot");
        policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setReleases(policy);
        policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setSnapshots(policy);
        maven.addRemoteRepository(repo);

        repo = new Repository();
        repo.setId("jboss");
        repo.setName("JBoss Repository");
        repo.setLayout("default");
        repo.setUrl("http://repository.jboss.com/maven2");
        policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setReleases(policy);
        policy = new RepositoryPolicy();
        policy.setEnabled(false);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setSnapshots(policy);
        maven.addRemoteRepository(repo);

        repo = new Repository();
        repo.setId("ibiblio");
        repo.setName("IBiblio Mirror Repository");
        repo.setLayout("default");
        repo.setUrl("http://mirrors.ibiblio.org/pub/mirrors/maven2");
        policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setReleases(policy);
        policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("never");
        policy.setChecksumPolicy("fail");
        repo.setSnapshots(policy);
        maven.addRemoteRepository(repo);

    }


}
