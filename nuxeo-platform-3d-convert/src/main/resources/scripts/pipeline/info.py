info = {}

for obj in bpy.context.scene.objects:
    if obj.type == 'MESH':
        bpy.context.scene.objects.active = obj
        bpy.ops.object.mode_set(mode='EDIT')
        bpy.ops.mesh.select_all(action='DESELECT')
        bpy.ops.mesh.select_non_manifold()
        bm = bmesh.from_edit_mesh(obj.data)
        selected_vertices = [v for v in bm.verts if v.select]
        selected_edges = [e for e in bm.edges if e.select]
        selected_polygons = [p for p in bm.faces if p.select]
        info[obj.name] = {
            'non_manifold_vertices': len(selected_vertices),
            'non_manifold_edges': len(selected_edges),
            'non_manifold_polygons': len(selected_polygons),
            'total_vertices': len(bm.verts),
            'total_edges': len(bm.edges),
            'total_polygons': len(bm.faces),
            'position_x': obj.location.x,
            'position_y': obj.location.y,
            'position_z': obj.location.z,
            'dimension_x': obj.dimensions.x,
            'dimension_y': obj.dimensions.y,
            'dimension_z': obj.dimensions.z,
            'lod_succes': lod_success
        }
        bpy.ops.mesh.select_all(action='DESELECT')
        bpy.ops.object.mode_set(mode='OBJECT')

if lod_id == 'default':
    info_default = info

info_directory = args.outdir + '/info/'
info_file = str(lod_id) + '.info'

if not os.path.exists(info_directory):
    os.makedirs(info_directory)

with open(info_directory + info_file, 'w+') as outfile:
    json.dump(info, outfile, indent=4, sort_keys=True, check_circular=False)
