prev_lod = current_lod
original_polygon_count = 0
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        original_polygon_count += len(ob.data.polygons)


def ratio_from_lods():
    return (perc_poly / current_lod) / 100


def ratio_from_max_polys():
    return int(max_poly) / original_polygon_count


def current_from_lods():
    return perc_poly / 100


def current_from_max_polys():
    return current_lod * lod_ratio


if perc_poly is not None and max_poly is not None:
    # both params are available
    lod_ratio_from_lods = ratio_from_lods()
    lod_ratio_from_max_polys = ratio_from_max_polys()
    if lod_ratio_from_lods < lod_ratio_from_max_polys:
        # perc_poly param is stricter
        lod_ratio = lod_ratio_from_lods
        current_lod = current_from_lods()
    else:
        # max_poly param is stricter
        lod_ratio = lod_ratio_from_max_polys
        current_lod = current_from_max_polys()
elif max_poly is not None:
    # only the max_poly param is available
    lod_ratio = ratio_from_max_polys()
    current_lod = current_from_max_polys()
else:
    # only the perc_poly param is available
    lod_ratio = ratio_from_lods()
    current_lod = current_from_lods()

next_lod = prev_lod * lod_ratio
print('[LOD] [%.2f to %.2f]' % (prev_lod, next_lod))
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        verts_non_manif = info_default['parts'][ob.name]['non_manifold_vertices']
        verts_total = info_default['parts'][ob.name]['total_vertices']
        perc_non_manif = verts_non_manif / verts_total
        ob.select = True
        mod = ob.modifiers.new(name='decimate', type='DECIMATE')
        lod_success = next_lod >= perc_non_manif
        if lod_success:
            # can reduce safely according to the heuristic
            mod.ratio = lod_ratio
            print('[LOD] applying %.2f ratio to %s mesh' % (next_lod, ob.name))
        else:
            # has to reduce to the value of the percentage of non manifold vertices
            mod.ratio = perc_non_manif / prev_lod
            print('[LOD] cannot apply %.2f ratio to %s mesh because the percentage of non manifold vertices is %.2f '
                  '(using %.2f instead)' % (next_lod, ob.name, perc_non_manif, perc_non_manif))
        bpy.context.scene.objects.active = ob
        bpy.ops.object.modifier_apply(apply_as='DATA', modifier='decimate')
