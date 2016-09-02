original_polygon_count = 0
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        original_polygon_count += len(ob.data.polygons)

if calculated_lod is not None and max_polygons is not None:
    lod_ratio = min(calculated_lod / 100, int(max_polygons) / original_polygon_count)
elif max_polygons is not None:
    lod_ratio = int(max_polygons) / original_polygon_count
else:
    lod_ratio = calculated_lod

for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        ob.select = ob.type == 'MESH'
        mod = ob.modifiers.new(name='decimate', type='DECIMATE')
        mod.ratio = lod_ratio
        bpy.context.scene.objects.active = ob
        bpy.ops.object.modifier_apply(apply_as='DATA', modifier='decimate')
