for ob in bpy.context.scene.objects:
	if ob.type == 'MESH':
		ob.select = ob.type =='MESH'
		mod = ob.modifiers.new(name='decimate', type='DECIMATE')
		mod.ratio = float(lod)/100.0
		bpy.context.scene.objects.active = ob
		bpy.ops.object.modifier_apply(apply_as='DATA',modifier='decimate')
