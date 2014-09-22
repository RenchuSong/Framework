package cs4620.ray1.surface;

import cs4620.ray1.IntersectionRecord;
import cs4620.ray1.Ray;
import egl.math.Vector3d;

/**
 * Represents a sphere as a center and a radius.
 *
 * @author ags
 */
public class Sphere extends Surface {
  
  /** The center of the sphere. */
  protected final Vector3d center = new Vector3d();
  public void setCenter(Vector3d center) { this.center.set(center); }
  
  /** The radius of the sphere. */
  protected double radius = 1.0;
  public void setRadius(double radius) { this.radius = radius; }
  
  protected final double M_2PI = 2*Math.PI;
  
  public Sphere() { }
  
  /**
   * Tests this surface for intersection with ray. If an intersection is found
   * record is filled out with the information about the intersection and the
   * method returns true. It returns false otherwise and the information in
   * outRecord is not modified.
   *
   * @param outRecord the output IntersectionRecord
   * @param ray the ray to intersect
   * @return true if the surface intersects the ray
   */
  public boolean intersect(IntersectionRecord outRecord, Ray rayIn) {
    // TODO#A2: fill in this function.
	Vector3d newOrigin = new Vector3d(rayIn.origin);
	newOrigin.sub(center);
	double dd = rayIn.direction.dot(rayIn.direction);
	double dp = rayIn.direction.dot(newOrigin);
	double pp = newOrigin.dot(newOrigin);
	double domain = dp * dp - dd * (pp - this.radius * this.radius);
	if (domain < 0) return false;
	double t1 = (-dp + Math.sqrt(domain)) / dd;
	double t2 = (-dp - Math.sqrt(domain)) / dd;
	if (t1 < rayIn.start || t1 > rayIn.end) t1 = t2;
	if (t2 < rayIn.start || t2 > rayIn.end) t2 = t1;
	if (t1 > t2) t1 = t2;
	if (t1 < rayIn.start || t1 > rayIn.end) {
		return false;
	} else {
		// location
		Vector3d location = new Vector3d(rayIn.direction);
		location.mul(t1);
		location.add(rayIn.origin);
		outRecord.location.set(location);
		// normal
		location.sub(center);
		location.normalize();
		outRecord.normal.set(location);
		// texCoords
		double angle = Math.asin(Math.abs(location.x));
		if (location.x <= 0 && location.z >= 0) {
			angle = Math.PI - angle;
		} else if (location.x > 0) {
			if (location.z <= 0) {
				angle = this.M_2PI - angle;
			} else {
				angle = Math.PI + angle;
			}
		}
		double u = angle / this.M_2PI;
		angle = Math.acos(Math.abs(location.y));
		if (location.y > 0) {
			angle = Math.PI - angle;
		}
		double v = angle / Math.PI;
		outRecord.texCoords.set(u, v);
		// Surface
		outRecord.surface = this;
		// t
		outRecord.t = t1;
		
		// Ray shorten
		rayIn.end = t1;
		return true;
	}
  }
  
  /**
   * @see Object#toString()
   */
  public String toString() {
    return "sphere " + center + " " + radius + " " + shader + " end";
  }

}