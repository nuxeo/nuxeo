width = dim[0]
height = dim[1]
outfile = args.outdir + '/render/render-' + str(id) + '-' + str(lod_id) + '-' + str(coords[0]) + '-' + str(coords[1]) +\
          '-' + str(width) + '-' + str(height) + '.png'
meshes = [obj for obj in bpy.data.objects if obj.type == 'MESH']

print('width: ' + str(width))
print('height: ' + str(height))

# min and max coordinate values of the mesh cluster (set of all mesh objects)
mc_min = [None, None, None]
mc_max = [None, None, None]
for mesh in meshes:
    for local_pt in mesh.bound_box:
        pt = mesh.matrix_world * Vector(local_pt)
        for i in range(3):
            num = pt[i]
            if not mc_min[i]:
                mc_min[i] = num
                mc_max[i] = num
            else:
                if mc_min[i] > num:
                    mc_min[i] = num
                if mc_max[i] < num:
                    mc_max[i] = num

# dimensions of the mesh cluster
mc_d = [abs(mc_min[0] - mc_max[0]), abs(mc_min[1] - mc_max[1]), abs(mc_min[2] - mc_max[2])]

# coordinates of the center of the mesh cluster
mc_c = [mc_min[0] + (mc_d[0] * 0.5), mc_min[1] + (mc_d[1] * 0.5), mc_min[2] + (mc_d[2] * 0.5)]

# position of the camera in cartesian coords from spherical coords
radial = (Vector(mc_max) - Vector(mc_min)).length
azimuth = radians(coords[0] % 360)
zenith = radians(coords[1] % 360)
cam_location = [
    mc_c[0] + radial * cos(azimuth) * sin(zenith),
    mc_c[1] + radial * sin(azimuth) * sin(zenith),
    mc_c[2] + radial * cos(zenith)
]

# set the orthographic camera
bpy.ops.object.camera_add(view_align=False, enter_editmode=False, location=cam_location)
camera_ob = bpy.context.object
camera = camera_ob.data
# the clip end is double the distance from the camera position to the center of the mesh cluster
camera.clip_end = radial * 2
camera.type = 'ORTHO'

# set render properties
scene = bpy.data.scenes.values()[0]
scene.camera = camera_ob
scene.render.resolution_x = int(width)
scene.render.resolution_y = int(height)
scene.render.resolution_percentage = 100

# set camera rotation to be aligned with the desired camera direction
cam_direction = Vector(mc_c) - Vector(cam_location)
rot_quat = cam_direction.to_track_quat('-Z', 'Y')
camera_ob.rotation_euler = rot_quat.to_euler()
# treat zenith edge cases in the poles
if zenith == radians(0) or zenith == radians(180):
    camera_ob.rotation_euler.z = azimuth + zenith + radians(90)
bpy.context.scene.update()

# set lighting
objects = bpy.data.objects
lamps = bpy.data.lamps

sun_top = objects.new(name='sun_top', object_data=lamps.new(name='sun_top', type='SUN'))
scene.objects.link(sun_top)
sun_top.rotation_euler = (0.0, 0.0, 0.0)
lamps['sun_top'].use_nodes = True
lamps['sun_top'].node_tree.nodes['Emission'].inputs[1].default_value = 3.0

sun_front = objects.new(name='sun_front', object_data=lamps.new(name='sun_front', type='SUN'))
scene.objects.link(sun_front)
sun_front.rotation_euler = (radians(90), 0.0, 0.0)
lamps['sun_front'].use_nodes = True
lamps['sun_front'].node_tree.nodes['Emission'].inputs[1].default_value = 1.0

sun_bottom = objects.new(name='sun_bottom', object_data=lamps.new(name='sun_bottom', type='SUN'))
scene.objects.link(sun_bottom)
sun_bottom.rotation_euler = (radians(180), 0.0, 0.0)
lamps['sun_bottom'].use_nodes = True
lamps['sun_bottom'].node_tree.nodes['Emission'].inputs[1].default_value = 1.0

sun_back = objects.new(name='sun_back', object_data=lamps.new(name='sun_back', type='SUN'))
scene.objects.link(sun_back)
sun_back.rotation_euler = (radians(270), 0.0, 0.0)
lamps['sun_back'].use_nodes = True
lamps['sun_back'].node_tree.nodes['Emission'].inputs[1].default_value = 1.0

sun_left = objects.new(name='sun_left', object_data=lamps.new(name='sun_left', type='SUN'))
scene.objects.link(sun_left)
sun_left.rotation_euler = (radians(90), 0.0, radians(90))
lamps['sun_left'].use_nodes = True
lamps['sun_left'].node_tree.nodes['Emission'].inputs[1].default_value = 1.0

sun_right = objects.new(name='sun_right', object_data=lamps.new(name='sun_right', type='SUN'))
scene.objects.link(sun_right)
sun_right.rotation_euler = (radians(90), 0.0, radians(270))
lamps['sun_right'].use_nodes = True
lamps['sun_right'].node_tree.nodes['Emission'].inputs[1].default_value = 1.0

# set render nodes
scene.use_nodes = True
tree = scene.node_tree
if len(tree.nodes) == 2:
    node_render_layers = tree.nodes['Render Layers']
    node_composite = tree.nodes['Composite']
    node_alpha_over = tree.nodes.new(type='CompositorNodeAlphaOver')
    # background color and transparency
    node_alpha_over.inputs[1].default_value = (1.0, 1.0, 1.0, 1.0)
    tree.links.new(node_render_layers.outputs[0], node_alpha_over.inputs[2])
    tree.links.new(node_alpha_over.outputs[0], node_composite.inputs[0])

# mesh cluster bounding box points, that should be in the camera's field of view
mc_bb_points = [
    mc_min,
    [mc_max[0], mc_min[1], mc_min[2]],
    [mc_min[0], mc_max[1], mc_min[2]],
    [mc_min[0], mc_min[1], mc_max[2]],
    [mc_min[0], mc_max[1], mc_max[2]],
    [mc_max[0], mc_min[1], mc_max[2]],
    [mc_max[0], mc_max[1], mc_min[2]],
    mc_max
]

# from an orthographic scale of 1, project the bounding box points to 2d
# the axis with the biggest range defines the new scale value, so that all points fit in the view
camera.ortho_scale = 1
bpy.context.scene.update()
min_x = None
max_x = None
min_y = None
max_y = None
for point in mc_bb_points:
    projected_point = world_to_camera_view(scene, camera_ob, Vector(point))
    if min_x is None or projected_point.x < min_x:
        min_x = projected_point.x
    if max_x is None or projected_point.x > max_x:
        max_x = projected_point.x
    if min_y is None or projected_point.y < min_y:
        min_y = projected_point.y
    if max_y is None or projected_point.y > max_y:
        max_y = projected_point.y
camera.ortho_scale = max([max_x - min_x, max_y - min_y])
bpy.context.scene.update()

scene.cycles.film_transparent = True
for obj in scene.objects:
    if obj.type == 'MESH':
        scene.objects.active = obj
        obj.select = True
        textures_used = 0
        for mat_slot in obj.material_slots:
            mat_slot.material.use_transparency = True
            mat_slot.material.use_face_texture = True
            mat_slot.material.use_face_texture_alpha = True
            textures_used += len([tex for tex in mat_slot.material.texture_slots if tex is not None and tex.use_map_color_diffuse])
        if textures_used > 0:
            bpy.ops.xps_tools.convert_to_cycles_selected()
        obj.select = False

# render and write file
scene.render.engine = 'CYCLES'
scene.render.filepath = outfile
bpy.ops.render.render(write_still=True)

# clean up afterwards (delete the created camera and target)
for obj in scene.objects:
    obj.select = obj.type == 'CAMERA' or obj.type == 'LAMP' or obj.type == 'EMPTY'
bpy.ops.object.delete()
