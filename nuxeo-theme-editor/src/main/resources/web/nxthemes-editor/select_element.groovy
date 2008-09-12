        
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.Element

id = Request.getParameter("id");

Element element = ThemeManager.getElementById(id);
if (element != null) {
    Context.setCookie("nxthemes.editor.selectedElement", id);
    Context.expireCookie("nxthemes.editor.currentStyleSelector");
    Context.expireCookie("nxthemes.editor.styleCategory");
    Context.expireCookie("nxthemes.editor.currentStyleLayer");
}

return id;
