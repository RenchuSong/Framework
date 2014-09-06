package cs4620.mesh.gen;

import org.lwjgl.BufferUtils;

import cs4620.mesh.MeshData;

/**
 * Generates A Cylinder Mesh
 * @author Cristian
 *
 */
public class MeshGenCylinder extends MeshGenerator {
	@Override
	public void generate(MeshData outData, MeshGenOptions opt) {
		// TODO#A1: Add Normals And Texture Coordinates Into The Mesh

		// Calculate Vertex And Index Count
		outData.vertexCount = (opt.divisionsLongitude) * 4 + 2;
		int tris = (opt.divisionsLongitude * 2) + (2 * (opt.divisionsLongitude - 2));
		outData.indexCount = tris * 3;

		// Create Storage Spaces
		outData.positions = BufferUtils.createFloatBuffer(outData.vertexCount * 3);
		outData.normals = BufferUtils.createFloatBuffer(outData.vertexCount * 3);
		outData.uvs = BufferUtils.createFloatBuffer(outData.vertexCount * 2);
		outData.indices = BufferUtils.createIntBuffer(outData.indexCount);
		
		// Create The Vertices
		for(int i = 0;i < opt.divisionsLongitude;i++) {
			// Calculate XZ-Plane Position
			float p = (float)i / (float)opt.divisionsLongitude;
			double theta = p * Math.PI * 2.0;
			float z = (float)-Math.cos(theta);
			float x = (float)-Math.sin(theta);
			
			// Middle Tube Top
			outData.positions.put(x); outData.positions.put(1); outData.positions.put(z);
			outData.normals.put(x); outData.normals.put(0); outData.normals.put(z);
			outData.uvs.put(p);outData.uvs.put(0.5f);
			
			// Middle Tube Bottom
			outData.positions.put(x); outData.positions.put(-1); outData.positions.put(z);
			outData.normals.put(x); outData.normals.put(0); outData.normals.put(z);
			outData.uvs.put(p);outData.uvs.put(0);
			
			// Top Cap
			outData.positions.put(x); outData.positions.put(1); outData.positions.put(z);
			outData.normals.put(0); outData.normals.put(1); outData.normals.put(0);
			outData.uvs.put(0.75f + 0.25f * x);outData.uvs.put(0.75f + 0.25f * z);
			
			// Bottom Cap
			outData.positions.put(x); outData.positions.put(-1); outData.positions.put(z);
			outData.normals.put(0); outData.normals.put(-1); outData.normals.put(0);
			outData.uvs.put(0.25f + 0.25f * x);outData.uvs.put(0.75f + 0.25f * z);
		}
		// Extra Vertices For U = 1
		float z = (float)-Math.cos(0);
		float x = (float)-Math.sin(0);
		outData.positions.put(0); outData.positions.put(1); outData.positions.put(-1);
		outData.normals.put(x); outData.normals.put(0); outData.normals.put(z);
		outData.uvs.put(1);outData.uvs.put(0.5f);
		
		outData.positions.put(0); outData.positions.put(-1); outData.positions.put(-1);
		outData.normals.put(x); outData.normals.put(0); outData.normals.put(z);
		outData.uvs.put(1);outData.uvs.put(0);
		
		// Create The Indices For The Tube
		for(int i = 0;i < opt.divisionsLongitude;i++) {
			int si = i * 4;
			outData.indices.put(si);
			outData.indices.put(si + 1);
			outData.indices.put(si + 4);
			outData.indices.put(si + 4);
			outData.indices.put(si + 1);
			outData.indices.put(si + 5);
		}
		
		// Create The Indices For The Caps
		for(int i = 0;i < opt.divisionsLongitude - 2;i++) {
			int si = (i + 1) * 4 + 2;
			
			// Top Fan Piece
			outData.indices.put(2);
			outData.indices.put(si);
			outData.indices.put(si + 4);

			// Bottom Fan Piece
			outData.indices.put(3);
			outData.indices.put(si + 5);
			outData.indices.put(si + 1);
		}
		
	}

}
