
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.presets.PresetType
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.types.Type

groups = [];
category = Context.getCookie("nxthemes.editor.styleCategory");
groups.add("");
if (category == null) {
    return groups;
}

groupNames = []
for (Type type : Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
    preset = (PresetType) type;

    group = preset.getGroup();
    if (!preset.getCategory().equals(category)) {
        continue;
    }
    if (!groupNames.contains(group)) {
        groups.add(group);
    }
    groupNames.add(group);
}

return groups;
