info = {'parts': {}}

non_manifold_vertices = 0
non_manifold_edges = 0
non_manifold_polygons = 0
total_vertices = 0
total_edges = 0
total_polygons = 0
bb_min = [None, None, None]
bb_max = [None, None, None]
global_lod_success = True


# find a way to achieve the same results using bmesh (faster)
def get_mesh_info(m):
    min = [None, None, None]
    max = [None, None, None]
    for v in m.data.vertices:
        v_world = m.matrix_world * v.co
        min[0] = v_world.x if (min[0] is None or v_world.x < min[0]) else min[0]
        min[1] = v_world.y if (min[1] is None or v_world.y < min[1]) else min[1]
        min[2] = v_world.z if (min[2] is None or v_world.z < min[2]) else min[2]
        max[0] = v_world.x if (max[0] is None or v_world.x > max[0]) else max[0]
        max[1] = v_world.y if (max[1] is None or v_world.y > max[1]) else max[1]
        max[2] = v_world.z if (max[2] is None or v_world.z > max[2]) else max[2]
    dim = Vector([max[0] - min[0], max[1] - min[1], max[2] - min[2]])
    cen = Vector([min[0] + dim.x * 0.5, min[1] + dim.y * 0.5, min[2] + dim.z * 0.5])
    min = Vector([min[0], min[1], min[2]])
    max = Vector([max[0], max[1], max[2]])
    return {'dim': dim, 'cen': cen, 'min': min, 'max': max}

scene = bpy.context.scene
for obj in scene.objects:
    if obj.type == 'MESH':
        clone = obj.copy()
        scene.objects.link(clone)
        scene.objects.active = clone
        bpy.ops.object.make_single_user(type='ALL', object=True, obdata=True)
        bpy.ops.object.modifier_apply(apply_as='DATA', modifier='decimate')
        bpy.ops.object.mode_set(mode='EDIT')
        bpy.ops.mesh.select_all(action='DESELECT')
        bpy.ops.mesh.select_non_manifold()
        bm = bmesh.from_edit_mesh(clone.data)
        selected_vertices = [v for v in bm.verts if v.select]
        selected_edges = [e for e in bm.edges if e.select]
        selected_polygons = [p for p in bm.faces if p.select]
        mesh_info = get_mesh_info(clone)
        info['parts'][obj.name] = {
            'non_manifold_vertices': len(selected_vertices),
            'non_manifold_edges': len(selected_edges),
            'non_manifold_polygons': len(selected_polygons),
            'vertices': len(bm.verts),
            'edges': len(bm.edges),
            'polygons': len(bm.faces),
            'position_x': mesh_info['cen'].x,
            'position_y': mesh_info['cen'].y,
            'position_z': mesh_info['cen'].z,
            'dimension_x': mesh_info['dim'].x,
            'dimension_y': mesh_info['dim'].y,
            'dimension_z': mesh_info['dim'].z,
            'geometry_lod_success': lod_success
        }
        non_manifold_vertices += len(selected_vertices)
        non_manifold_edges += len(selected_edges)
        non_manifold_polygons += len(selected_polygons)
        total_vertices += len(bm.verts)
        total_edges += len(bm.edges)
        total_polygons += len(bm.faces)
        bb_min[0] = mesh_info['min'].x if (bb_min[0] is None or mesh_info['min'].x < bb_min[0]) else bb_min[0]
        bb_min[1] = mesh_info['min'].y if (bb_min[1] is None or mesh_info['min'].y < bb_min[1]) else bb_min[1]
        bb_min[2] = mesh_info['min'].z if (bb_min[2] is None or mesh_info['min'].z < bb_min[2]) else bb_min[2]
        bb_max[0] = mesh_info['max'].x if (bb_max[0] is None or mesh_info['max'].x > bb_max[0]) else bb_max[0]
        bb_max[1] = mesh_info['max'].y if (bb_max[1] is None or mesh_info['max'].y > bb_max[1]) else bb_max[1]
        bb_max[2] = mesh_info['max'].z if (bb_max[2] is None or mesh_info['max'].z > bb_max[2]) else bb_max[2]
        global_lod_success = global_lod_success and lod_success
        bpy.ops.object.mode_set(mode='OBJECT')
        bpy.ops.object.select_all(action='DESELECT')
        clone.select = True
        bpy.ops.object.delete()

info['global'] = {
    'non_manifold_vertices': non_manifold_vertices,
    'non_manifold_edges': non_manifold_edges,
    'non_manifold_polygons': non_manifold_polygons,
    'vertices': total_vertices,
    'edges': total_edges,
    'polygons': total_polygons,
    'position_x': bb_min[0] + ((bb_max[0] - bb_min[0]) * 0.5),
    'position_y': bb_min[1] + ((bb_max[1] - bb_min[1]) * 0.5),
    'position_z': bb_min[2] + ((bb_max[2] - bb_min[2]) * 0.5),
    'dimension_x': bb_max[0] - bb_min[0],
    'dimension_y': bb_max[1] - bb_min[1],
    'dimension_z': bb_max[2] - bb_min[2],
    'geometry_lod_success': global_lod_success
}

if lod_id == 'default':
    info_default = info

info_directory = args.outdir + '/info/'
info_file = str(lod_id) + '.info'

if not os.path.exists(info_directory):
    os.makedirs(info_directory)

with open(info_directory + info_file, 'w+') as outfile:
    json.dump(info, outfile, indent=4, sort_keys=True, check_circular=False)
