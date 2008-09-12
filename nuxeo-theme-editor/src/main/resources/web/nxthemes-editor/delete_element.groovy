
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element	
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.fragments.Fragment;

id = Request.getParameter("id");

Element element = ThemeManager.getElementById(id);
Element parent = (Element) element.getParent();

ThemeManager themeManager = Manager.getThemeManager()

if (element instanceof ThemeElement || element instanceof PageElement) {
    themeManager.destroyElement(element);
} else if (element instanceof CellElement) {
    if (element.hasSiblings()) {
        Element sibling = (Element) element.getNextNode();
        if (sibling == null) {
            sibling = (Element) element.getPreviousNode();
        }
        FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "layout");
        Format layout1 = ElementFormatter.getFormatByType(element, layoutType);
        if (layout1 != null) {
            width1 = layout1.getProperty("width");
            if (width1 != null) {
                Format layout2 = ElementFormatter.getFormatByType(sibling, layoutType);
                if (layout2 != null) {
                    width2 = layout2.getProperty("width");
                    newWidth = Utils.addWebLengths(width1, width2);
                    if (newWidth != null) {
                        layout2.setProperty("width", newWidth);
                    }
                }
            }
        }
        // remove cell
        themeManager.destroyElement(element);
    } else {
        // remove parent section
        themeManager.destroyElement(parent);
    }
} else if (element instanceof Fragment) {
    themeManager.destroyElement(element);
}

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null));
return id;

