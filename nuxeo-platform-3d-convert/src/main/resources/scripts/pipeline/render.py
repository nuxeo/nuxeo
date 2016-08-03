if args.outdir == None or args.width == None or args.height == None:
    sys.exit()

outfile = args.outdir + "/render-" + str(lod) + "-" + str(coords[0]) + "-" + str(coords[1]) + ".png"
width = args.width
height = args.height
# get the meshes
meshes = [obj for obj in bpy.data.objects if obj.type == 'MESH']

print("Found %d meshes" % len(meshes))

# get min/max for X, Y and Z
m_min = [None, None, None]
m_max = [None, None, None]

for mesh in meshes:
    m_matrix = mesh.matrix_world
    for local_pt in mesh.bound_box:
        pt = m_matrix * Vector(local_pt)
        for i in range(3):
            num = pt[i]
            if not m_min[i]:
                m_min[i] = num
                m_max[i] = num
            else:
                if m_min[i] > num:
                    m_min[i] = num
                if m_max[i] < num:
                    m_max[i] = num

dx = abs(m_min[0] - m_max[0])
dy = abs(m_min[1] - m_max[1])
dz = abs(m_min[2] - m_max[2])

greatest = max(dx, dy, dz)

print("Greatest length is %f " % greatest)

print(coords)

cam_location = [greatest * 1.5] * 3
cam_look_at = [
    (dx / 2) + m_min[0],
    (dy / 2) + m_min[1],
    (dz / 2) + m_min[2]
]

# add and setup the camera
bpy.ops.object.camera_add(view_align=False, enter_editmode=False,
                          location=cam_location)
camera_ob = bpy.context.object
camera = camera_ob.data
camera.clip_end = greatest * 10
camera.ortho_scale = greatest * 1.5
camera.type = "ORTHO"

# add an empty for focusing the camera
bpy.ops.object.add(
    type='EMPTY',
    location=cam_look_at)
target = bpy.context.object
target.name = 'Target'

# add track to constraints
cns = camera_ob.constraints.new('TRACK_TO')
cns.name = 'TrackTarget'
cns.target = target
cns.track_axis = 'TRACK_NEGATIVE_Z'
cns.up_axis = 'UP_Y'
cns.owner_space = 'WORLD'
cns.target_space = 'WORLD'

# attempt to render
scene = bpy.data.scenes.values()[0]
scene.camera = camera_ob
# scene.render.file_format = 'PNG'
scene.render.resolution_x = int(width)
scene.render.resolution_y = int(height)
scene.render.resolution_percentage = 100
scene.render.alpha_mode = 'TRANSPARENT'
bpy.context.scene.render.image_settings.color_mode = 'RGBA'

# scene.world.ambient_color = [0.08, 0.01, 0.045]

print("""World settings:

- ambient color : %s
- horizon color : %s
- zenith color : %s
- exposure : %d
""" % (
    scene.world.ambient_color,
    scene.world.horizon_color,
    scene.world.zenith_color,
    scene.world.exposure
))

scene.world.light_settings.use_environment_light = True
scene.world.light_settings.environment_energy = .5

scene.world.light_settings.use_ambient_occlusion = True

scene.world.light_settings.sample_method = 'CONSTANT_JITTERED'
scene.world.light_settings.samples = 10
scene.world.light_settings.bias = .5

print("""Environment Lighting settings:

- color : %s
- energy : %f
- use ambient occlusion : %s
- ambient occlusion factor: %f
- ambient occlusion blend: %s
- Gather method: %s
- Sample method: %s
- Samples: %f
- Bias: %f
""" % (
    scene.world.light_settings.environment_color,
    scene.world.light_settings.environment_energy,
    scene.world.light_settings.use_ambient_occlusion,
    scene.world.light_settings.ao_factor,
    scene.world.light_settings.ao_blend_type,
    scene.world.light_settings.gather_method,
    scene.world.light_settings.sample_method,
    scene.world.light_settings.samples,
    scene.world.light_settings.bias))

print("Camera(%f, %f, %f)" % (camera_ob.location.x, camera_ob.location.y, camera_ob.location.z))

scene.render.filepath = outfile

bpy.ops.render.render(write_still=True)
