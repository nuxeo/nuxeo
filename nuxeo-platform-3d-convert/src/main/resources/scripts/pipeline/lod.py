original_polygon_count = 0
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        original_polygon_count += len(ob.data.polygons)


scene = bpy.context.scene
for obj in scene.objects:
    if obj.type == 'MESH' and len([d for d in obj.modifiers if d.name == 'decimate']) == 0:
        mod = obj.modifiers.new(name='decimate', type='DECIMATE')

if perc_poly is not None and max_poly is not None:
    # both params are available
    lod_ratio = min([perc_poly / 100, max_poly / original_polygon_count])
elif max_poly is not None:
    # only the max_poly param is available
    lod_ratio = max_poly / original_polygon_count
else:
    # only the perc_poly param is available
    lod_ratio = perc_poly / 100

for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        verts_non_manif = info_default['parts'][ob.name]['non_manifold_vertices']
        verts_total = info_default['parts'][ob.name]['vertices']
        perc_non_manif = verts_non_manif / verts_total
        ob.select = True
        mod = ob.modifiers['decimate']
        lod_success = lod_ratio >= perc_non_manif
        if lod_success:
            # can reduce safely according to the heuristic
            mod.ratio = lod_ratio
            print('[LOD] applying %.2f ratio to %s mesh' % (lod_ratio, ob.name))
        else:
            # has to reduce to the value of the percentage of non manifold vertices
            mod.ratio = perc_non_manif
            print('[LOD] cannot apply %.2f ratio to %s mesh because the percentage of non manifold vertices is %.2f '
                  '(using %.2f instead)' % (lod_ratio, ob.name, perc_non_manif, perc_non_manif))
