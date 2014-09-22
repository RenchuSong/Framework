package cs4620.ray1.surface;

import cs4620.ray1.IntersectionRecord;
import cs4620.ray1.Ray;
import egl.math.Vector2d;
import egl.math.Vector3d;
import egl.math.Vector3i;
import cs4620.ray1.shader.Shader;

/**
 * Represents a single triangle, part of a triangle mesh
 *
 * @author ags
 */
public class Triangle extends Surface {
  /** The normal vector of this triangle, if vertex normals are not specified */
  Vector3d norm;
  
  /** The mesh that contains this triangle */
  Mesh owner;
  
  /** 3 indices to the vertices of this triangle. */
  Vector3i index;
  
  double a, b, c, d, e, f;
  public Triangle(Mesh owner, Vector3i index, Shader shader) {
    this.owner = owner;
    this.index = new Vector3i(index);
    
    Vector3d v0 = owner.getPosition(index.x);
    Vector3d v1 = owner.getPosition(index.y);
    Vector3d v2 = owner.getPosition(index.z);
    
    if (!owner.hasNormals()) {
    	Vector3d e0 = new Vector3d(), e1 = new Vector3d();
    	e0.set(v1).sub(v0);
    	e1.set(v2).sub(v0);
    	norm = new Vector3d();
    	norm.set(e0).cross(e1);
    }
    a = v0.x-v1.x;
    b = v0.y-v1.y;
    c = v0.z-v1.z;
    
    d = v0.x-v2.x;
    e = v0.y-v2.y;
    f = v0.z-v2.z;
    
    this.setShader(shader);
  }

  /**
   * Tests this surface for intersection with ray. If an intersection is found
   * record is filled out with the information about the intersection and the
   * method returns true. It returns false otherwise and the information in
   * outRecord is not modified.
   *
   * @param outRecord the output IntersectionRecord
   * @param rayIn the ray to intersect
   * @return true if the surface intersects the ray
   */
  public boolean intersect(IntersectionRecord outRecord, Ray rayIn) {
    // TODO#A2: fill in this function.
	Vector3d v0 = owner.getPosition(index.x);

	double g = rayIn.direction.x;
	double h = rayIn.direction.y;
	double i = rayIn.direction.z;
	double j = v0.x - rayIn.origin.x;
	double k = v0.y - rayIn.origin.y;
	double l = v0.z - rayIn.origin.z;
	double M = a * (e * i - h * f) + b * (g * f - d * i) + c * (d * h - e * g);
	if (M == 0) {
		return false;
	}
	double t = -(f * (a * k - j * b) + e * (j * c - a * l) + d * (b * l - k * c)) / M;
	if (t < rayIn.start || t > rayIn.end) {
		return false;
	}
	double gama = (i * (a * k - j * b)+h * (j * c - a * l)+g * (b * l - k * c)) / M;
	if (gama < 0 || gama > 1) {
		return false;
	}
	double beta = (j * (e * i-h * f)+k * (g * f-d * i)+l * (d * h-e * g)) / M;
	if (beta < 0 || beta > 1 - gama) {
		return false;
	}
	// location
	Vector3d location = new Vector3d(rayIn.direction);
	location.mul(t);
	location.add(rayIn.origin);
	outRecord.location.set(location);
	// normal
	if (this.owner.hasNormals()) {
		Vector3d n1 = this.owner.getNormal(this.index.x);
		Vector3d n2 = this.owner.getNormal(this.index.y);
		Vector3d n3 = this.owner.getNormal(this.index.z);
		n2.sub(n1); n2.mul(beta);
		n3.sub(n1); n3.mul(gama);
		n1.add(n2);
		n1.add(n3);
		n1.normalize();
		outRecord.normal.set(n1);
	} else {
		outRecord.normal.set(this.norm);
	}
	// texCoords
	if (this.owner.hasUVs()) {
		Vector2d n1 = this.owner.getUV(this.index.x);
		Vector2d n2 = this.owner.getUV(this.index.y);
		Vector2d n3 = this.owner.getUV(this.index.z);
		n2.sub(n1); n2.mul(beta);
		n3.sub(n1); n3.mul(gama);
		n1.add(n2);
		n1.add(n3);
		outRecord.texCoords.set(n1);
	}
	// Surface
	outRecord.surface = this;
	// t
	outRecord.t = t;
	// Ray shorten
	rayIn.end = t;
	return true;
  }

  /**
   * @see Object#toString()
   */
  public String toString() {
    return "Triangle ";
  }
}