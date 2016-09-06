original_polygon_count = 0
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        original_polygon_count += len(ob.data.polygons)


def ratio_from_lods():
    return (lod / current_lod) / 100


def ratio_from_max_polys():
    return int(max_polygons) / original_polygon_count


def current_from_lods():
    return lod / 100


def current_from_max_polys():
    return current_lod * lod_ratio


if lod is not None and max_polygons is not None:
    # both params are available
    lod_ratio_from_lods = ratio_from_lods()
    lod_ratio_from_max_polys = ratio_from_max_polys()
    if lod_ratio_from_lods < lod_ratio_from_max_polys:
        # lod param is stricter
        lod_ratio = lod_ratio_from_lods
        current_lod = current_from_lods()
    else:
        # max_polygons param is stricter
        lod_ratio = lod_ratio_from_max_polys
        current_lod = current_from_max_polys()
elif max_polygons is not None:
    # only the max_polygons param is available
    lod_ratio = ratio_from_max_polys()
    current_lod = current_from_max_polys()
else:
    # only the lod param is available
    lod_ratio = ratio_from_lods()
    current_lod = current_from_lods()

for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        ob.select = ob.type == 'MESH'
        mod = ob.modifiers.new(name='decimate', type='DECIMATE')
        mod.ratio = lod_ratio
        bpy.context.scene.objects.active = ob
        bpy.ops.object.modifier_apply(apply_as='DATA', modifier='decimate')
