import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.views.ViewType
import org.nuxeo.theme.fragments.FragmentType
import org.nuxeo.theme.editor.FragmentInfo

fragments = []
applicationPath = Request.getParameter("org.nuxeo.theme.application.path")
templateEngine = ThemeManager.getTemplateEngine(applicationPath)

for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
    FragmentType fragmentType = (FragmentType) f
    FragmentInfo fragmentInfo = new FragmentInfo(fragmentType)
    for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
        String viewTemplateEngine = viewType.getTemplateEngine()
        if (!"*".equals(viewType.getViewName()) && templateEngine.equals(viewTemplateEngine)) {
            fragmentInfo.addView(viewType)
        }
    }
    if (fragmentInfo.size() > 0) {
         fragments.add(fragmentInfo)
    }
}

return fragments

