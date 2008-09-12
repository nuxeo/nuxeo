
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.views.ViewType
import org.nuxeo.theme.fragments.FragmentType

class FragmentInfo {

    FragmentType fragmentType;

    FragmentInfo(FragmentType fragmentType) {
        this.fragmentType = fragmentType;
    }

    FragmentType getFragmentType() {
        return fragmentType;
    }

    List<ViewType> viewTypes = [];

    void addView(final ViewType viewType) {
        viewTypes.add(viewType);
    }

    List<ViewType> getViews() {
        return viewTypes;
    }

    int size() {
        return viewTypes.size();
    }
    
}

fragments = []
templateEngine = ThemeManager.getTemplateEngine("/st")

for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
    FragmentType fragmentType = (FragmentType) f;
    FragmentInfo fragmentInfo = new FragmentInfo(fragmentType);
    for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
        String viewTemplateEngine = viewType.getTemplateEngine();
        if (!"*".equals(viewType.getViewName()) && templateEngine.equals(viewTemplateEngine)) {
            fragmentInfo.addView(viewType);
        }
    }
    if (fragmentInfo.size() > 0) {
         fragments.add(fragmentInfo);
    }
}

return fragments;

