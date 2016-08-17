outfile = args.outdir + '/render-' + str(base_lod) + '-' + str(coords[0]) + '-' + str(coords[1]) + '.png'
width = args.width
height = args.height
meshes = [obj for obj in bpy.data.objects if obj.type == 'MESH']

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
azimuth = radians(coords[0])
zenith = radians(coords[1])
cam_location = [
    mc_c[0] + radial * cos(azimuth) * sin(zenith),
    mc_c[1] + radial * sin(azimuth) * sin(zenith),
    mc_c[2] + radial * cos(zenith)
]

# camera should look at the center point of the cluster
cam_look_at = mc_c

# set the orthographic camera
bpy.ops.object.camera_add(view_align=False, enter_editmode=False, location=cam_location)
camera_ob = bpy.context.object
camera = camera_ob.data
# the clip end needs to be distance from the camera position to the minimum point of the mesh cluster
# added 1% to avoid losing points due to floating point errors
camera.clip_end = (Vector(mc_min) - Vector(cam_location)).length * 1.01
camera.type = 'ORTHO'

# set the camera target and constraints
bpy.ops.object.add(type='EMPTY', location=cam_look_at)
target = bpy.context.object
target.name = 'Target'
cns = camera_ob.constraints.new('TRACK_TO')
cns.name = 'TrackTarget'
cns.target = target
cns.track_axis = 'TRACK_NEGATIVE_Z'
cns.up_axis = 'UP_Y'
cns.owner_space = 'WORLD'
cns.target_space = 'WORLD'

# set render properties
scene = bpy.data.scenes.values()[0]
scene.camera = camera_ob
scene.render.resolution_x = int(width)
scene.render.resolution_y = int(height)
scene.render.resolution_percentage = 100
scene.render.alpha_mode = 'TRANSPARENT'
bpy.context.scene.render.image_settings.color_mode = 'RGBA'
print("""World settings:
- Ambient color: %s
- Horizon color: %s
- Zenith color: %s
- Exposure: %d""" % (
    scene.world.ambient_color,
    scene.world.horizon_color,
    scene.world.zenith_color,
    scene.world.exposure))

# set lighting
scene.world.light_settings.use_environment_light = True
scene.world.light_settings.environment_energy = .5
scene.world.light_settings.use_ambient_occlusion = True
scene.world.light_settings.sample_method = 'CONSTANT_JITTERED'
scene.world.light_settings.samples = 10
scene.world.light_settings.bias = .5
print("""Environment Lighting settings:
- Color: %s
- Energy: %f
- Use ambient occlusion: %s
- Ambient occlusion factor: %f
- Ambient occlusion blend: %s
- Gather method: %s
- Sample method: %s
- Samples: %f
- Bias: %f""" % (
    scene.world.light_settings.environment_color,
    scene.world.light_settings.environment_energy,
    scene.world.light_settings.use_ambient_occlusion,
    scene.world.light_settings.ao_factor,
    scene.world.light_settings.ao_blend_type,
    scene.world.light_settings.gather_method,
    scene.world.light_settings.sample_method,
    scene.world.light_settings.samples,
    scene.world.light_settings.bias))

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

# for point in mc_bb_points:
#     bpy.ops.mesh.primitive_uv_sphere_add(size=(max(mc_d) * 0.02), location=point)

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

# render and write file
scene.render.filepath = outfile
bpy.ops.render.render(write_still=True)

# clean up afterwards (delete the created camera and target)
# bpy.ops.object.select_pattern(pattern='Sphere*')
# bpy.ops.object.delete()
for obj in scene.objects:
    obj.select = obj.type == 'CAMERA' or obj.type == 'EMPTY'
bpy.ops.object.delete()