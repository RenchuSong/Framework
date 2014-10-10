package cs4620.ray1.shader;

import cs4620.ray1.IntersectionRecord;
import cs4620.ray1.Light;
import cs4620.ray1.Ray;
import cs4620.ray1.Scene;
import egl.math.Color;
import egl.math.Colord;
import egl.math.Vector3d;

/**
 * A Lambertian material scatters light equally in all directions. BRDF value is
 * a constant
 *
 * @author ags
 */
public class Lambertian extends Shader {

	/** The color of the surface. */
	protected final Colord diffuseColor = new Colord(Color.White);
	public void setDiffuseColor(Colord inDiffuseColor) { diffuseColor.set(inDiffuseColor); }

	public Lambertian() { }
	
	/**
	 * @see Object#toString()
	 */
	public String toString() {
		return "lambertian: " + diffuseColor;
	}

	/**
	 * Evaluate the intensity for a given intersection using the Lambert shading model.
	 * 
	 * @param outIntensity The color returned towards the source of the incoming ray.
	 * @param scene The scene in which the surface exists.
	 * @param ray The ray which intersected the surface.
	 * @param record The intersection record of where the ray intersected the surface.
	 * @param depth The recursion depth.
	 */
	@Override
	public void shade(Colord outIntensity, Scene scene, Ray ray, IntersectionRecord record) {
		// TODO#A2: Fill in this function.
		// 1) Loop through each light in the scene.
		// 2) If the intersection point is shadowed, skip the calculation for the light.
		//	  See Shader.java for a useful shadowing function.
		// 3) Compute the incoming direction by subtracting
		//    the intersection point from the light's position.
		// 4) Compute the color of the point using the Lambert shading model. Add this value
		//    to the output.
		outIntensity.set(0);
		for (Light light: scene.getLights()) {
			if (!record.surface.getShader().isShadowed(scene, light, record, new Ray())) {
				Vector3d wi = new Vector3d(light.position);
				wi.sub(record.location);
				double r = wi.dot(wi);
				wi.normalize();
				// TODO: get color from texture
				Colord kd = record.surface.getShader().texture == null ? 
						new Colord(this.diffuseColor) :
						new Colord(record.surface.getShader().texture.getTexColor(record.texCoords));
				Vector3d intensity = new Vector3d(light.intensity);
				intensity.div(r);
				intensity.mul(Math.max(0, wi.dot(record.normal.normalize())));
				kd.mul(intensity);				
				outIntensity.add(kd);
			}
		}
	}

}