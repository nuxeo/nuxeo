package org.nuxeo.ecm.webapp.treesorter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * 
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin BAICAN</a>
 * 
 */
@XObject("treeSorterImplementation")
public class TreeSorterDescriptor {

    private static final Log log = LogFactory.getLog(TreeSorterDescriptor.class);

    /**
     * The name of the class implementig the compareTo method.
     */
    @XNode("@className")
    private String className;

    private TreeSorter implementor;

    public TreeSorterDescriptor() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Gets the default implementor or the contributed one if available.
     * 
     * @return implementor
     */
    public TreeSorter getImplementor() {
        if (implementor != null) {
            return implementor;
        }
        if (className != null) {
            try {
                Class implemObj = Class.forName(className);
                implementor = (TreeSorter) implemObj.newInstance();
            } catch (ClassNotFoundException e) {
                log.warn("Cannot load implementor class, falling to default: "
                        + className, e);
            } catch (InstantiationException e) {
                log.warn(
                        "Cannot instantiate implementor class, falling to default: "
                                + className, e);
            } catch (IllegalAccessException e) {
                log.error(
                        "Cannot create implementor class, falling to default: "
                                + className, e);
            }
        }
        if (implementor == null) {
            log.info("Tree Sorter Service uses default implementation");
            implementor = new TreeSorterServiceImpl();
        } else {
            log.info("Tree Sorter Service uses implementation " + className);
        }

        return implementor;
    }

}
