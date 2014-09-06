package cs4620.mesh;

import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import egl.math.Vector3;
import egl.math.Vector3i;

/**
 * Performs Normals Reconstruction Upon A Mesh Of Positions
 * @author Cristian
 *
 */
public class MeshConverter {
	/**
	 * Reconstruct a mesh's normals so that it appears to have sharp creases
	 * @param positions List of positions
	 * @param tris List of triangles (Each is a group of three indices into the positions list)
	 * @return A mesh with all faces separated and normals at vertices that lie normal to faces
	 */
	public static MeshData convertToFaceNormals(ArrayList<Vector3> positions, ArrayList<Vector3i> tris) {
		MeshData data = new MeshData();

		// Notice
		System.out.println("Face normals are not implemented");
		
		// No need to implement this function, not part of the Mesh assignment.
		
		return data;
	}
	/**
	 * Reconstruct a mesh's normals so that it appears to be smooth
	 * @param positions List of positions
	 * @param tris List of triangles (Each is a group of three indices into the positions list)
	 * @return A mesh with normals at vertices
	 */
	public static MeshData convertToVertexNormals(ArrayList<Vector3> positions, ArrayList<Vector3i> tris) {
		MeshData data = new MeshData();

		// TODO#A1: Allocate mesh data and create mesh positions, normals, and indices (Remember to set mesh Vertex/Index counts)
		// Note that the vertex data has been supplied as a list of egl.math.Vector3 objects.  Take a
		// look at that class, which contains methods that are very helpful for performing vector
		// math.
		ArrayList<Vector3> normals = new ArrayList<Vector3>();
		for (int i = 0; i < positions.size(); i++) {
			normals.add(new Vector3(0));
		}
		for (int i = 0; i < tris.size(); i++) {
			Vector3i surface = tris.get(i);
			Vector3 p1 = new Vector3(positions.get(surface.x)),
					p2 = new Vector3(positions.get(surface.y)),
					p3 = new Vector3(positions.get(surface.z));
			p2.sub(p1);
			p3.sub(p1);
			if (p2.len() < 1e-8 || p3.len() < 1e-8) continue;
			p2.cross(p3).normalize();
			normals.get(surface.x).add(p2);
			normals.get(surface.y).add(p2);
			normals.get(surface.z).add(p2);
		}
		for (int i = 0; i < positions.size(); i++) {
			normals.get(i).normalize();
		}

		// Calculate Vertex And Index Count
		data.vertexCount = positions.size();
		data.indexCount = tris.size() * 3;

		// Create Storage Spaces
		data.positions = BufferUtils.createFloatBuffer(data.vertexCount * 3);
		data.normals = BufferUtils.createFloatBuffer(data.vertexCount * 3);
		data.indices = BufferUtils.createIntBuffer(data.indexCount);
		
		// Create The Vertices
		for (int i = 0; i < positions.size(); i++) {
			data.positions.put(positions.get(i).x);
			data.positions.put(positions.get(i).y);
			data.positions.put(positions.get(i).z);
			data.normals.put(normals.get(i).x);
			data.normals.put(normals.get(i).y);
			data.normals.put(normals.get(i).z);
		}
		
		// Create The Indices
		for (int i = 0; i < tris.size(); i++) {
			data.indices.put(tris.get(i).x);
			data.indices.put(tris.get(i).y);
			data.indices.put(tris.get(i).z);
		}
		
		return data;
	}
}
