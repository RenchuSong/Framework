package cs4620.mesh.gen;

import org.lwjgl.BufferUtils;

import cs4620.mesh.MeshData;

/**
 * Generates A Sphere Mesh
 * @author Cristian
 *
 */
public class MeshGenSphere extends MeshGenerator {
	@Override
	public void generate(MeshData outData, MeshGenOptions opt) {
		// TODO#A1: Create A Sphere Mesh

		// Calculate Vertex And Index Count
		outData.vertexCount = (opt.divisionsLongitude + 1) * (opt.divisionsLatitude + 1);
		int tris = opt.divisionsLongitude * opt.divisionsLatitude * 2;
		outData.indexCount = tris * 3;
		
		// Create Storage Spaces
		outData.positions = BufferUtils.createFloatBuffer(outData.vertexCount * 3);
		outData.normals = BufferUtils.createFloatBuffer(outData.vertexCount * 3);
		outData.uvs = BufferUtils.createFloatBuffer(outData.vertexCount * 2);
		outData.indices = BufferUtils.createIntBuffer(outData.indexCount);
		
		// Create The Vertices
		for (int i = 0; i <= opt.divisionsLatitude; i++) {
			float p = (float) i / (float) opt.divisionsLatitude;
			for (int j = 0; j <= opt.divisionsLongitude; j++) {
				float q = (float) j / (float) opt.divisionsLongitude;
				double theta = q * Math.PI * 2;
				double gama = p * Math.PI;
				float x = (float)-(Math.sin(theta) * Math.sin(gama));
				float y = (float)-Math.cos(gama);
				float z = (float)-(Math.cos(theta) * Math.sin(gama));
				outData.positions.put(x); outData.positions.put(y); outData.positions.put(z);
				outData.normals.put(x); outData.normals.put(y); outData.normals.put(z);
				outData.uvs.put(q);outData.uvs.put(p);
			}
		}
		
		// Create The Indices
		for(int i = 0; i < opt.divisionsLatitude; i++) {
			int si = i * (opt.divisionsLongitude + 1);
			for (int j = 0; j < opt.divisionsLongitude; j++) {
				outData.indices.put(si + j);
				outData.indices.put(si + j + 1);
				outData.indices.put(si + j + opt.divisionsLongitude + 1);
				outData.indices.put(si + j + opt.divisionsLongitude + 1);
				outData.indices.put(si + j + 1);
				outData.indices.put(si + j + opt.divisionsLongitude + 2);	
			}
		}
		
	}
}
