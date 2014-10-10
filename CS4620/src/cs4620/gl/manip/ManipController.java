package cs4620.gl.manip;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import blister.input.KeyboardEventDispatcher;
import blister.input.KeyboardKeyEventArgs;
import blister.input.MouseButton;
import blister.input.MouseButtonEventArgs;
import blister.input.MouseEventDispatcher;
import cs4620.common.Scene;
import cs4620.common.SceneObject;
import cs4620.common.UUIDGenerator;
import cs4620.common.event.SceneTransformationEvent;
import cs4620.gl.PickingProgram;
import cs4620.gl.RenderCamera;
import cs4620.gl.RenderEnvironment;
import cs4620.gl.RenderObject;
import cs4620.gl.Renderer;
import cs4620.ray1.Ray;
import cs4620.scene.form.ControlWindow;
import cs4620.scene.form.ScenePanel;
import egl.BlendState;
import egl.DepthState;
import egl.IDisposable;
import egl.RasterizerState;
import egl.math.Matrix4;
import egl.math.Vector2;
import egl.math.Vector3;
import egl.math.Vector4;
import ext.csharp.ACEventFunc;

public class ManipController implements IDisposable {
	public final ManipRenderer renderer = new ManipRenderer();
	public final HashMap<Manipulator, UUIDGenerator.ID> manipIDs = new HashMap<>();
	public final HashMap<Integer, Manipulator> manips = new HashMap<>();
	
	private final Scene scene;
	private final ControlWindow propWindow;
	private final ScenePanel scenePanel;
	private final RenderEnvironment rEnv;
	private ManipRenderer manipRenderer = new ManipRenderer();
	
	private final Manipulator[] currentManips = new Manipulator[3];
	private RenderObject currentObject = null;
	
	private Manipulator selectedManipulator = null;
	
	/**
	 * Is parent mode on?  That is, should manipulation happen in parent rather than object coordinates?
	 */
	private boolean parentSpace = false;
	
	/**
	 * Last seen mouse position in normalized coordinates
	 */
	private final Vector2 lastMousePos = new Vector2();
	
	public ACEventFunc<KeyboardKeyEventArgs> onKeyPress = new ACEventFunc<KeyboardKeyEventArgs>() {
		@Override
		public void receive(Object sender, KeyboardKeyEventArgs args) {
			if(selectedManipulator != null) return;
			switch (args.key) {
			case Keyboard.KEY_T:
				setCurrentManipType(Manipulator.Type.TRANSLATE);
				break;
			case Keyboard.KEY_R:
				setCurrentManipType(Manipulator.Type.ROTATE);
				break;
			case Keyboard.KEY_Y:
				setCurrentManipType(Manipulator.Type.SCALE);
				break;
			case Keyboard.KEY_P:
				parentSpace = !parentSpace;
				break;
			}
		}
	};
	public ACEventFunc<MouseButtonEventArgs> onMouseRelease = new ACEventFunc<MouseButtonEventArgs>() {
		@Override
		public void receive(Object sender, MouseButtonEventArgs args) {
			if(args.button == MouseButton.Right) {
				selectedManipulator = null;
			}
		}
	};
	
	public ManipController(RenderEnvironment re, Scene s, ControlWindow cw) {
		scene = s;
		propWindow = cw;
		Component o = cw.tabs.get("Object");
		scenePanel = o == null ? null : (ScenePanel)o;
		rEnv = re;
		
		// Give Manipulators Unique IDs
		manipIDs.put(Manipulator.ScaleX, scene.objects.getID("ScaleX"));
		manipIDs.put(Manipulator.ScaleY, scene.objects.getID("ScaleY"));
		manipIDs.put(Manipulator.ScaleZ, scene.objects.getID("ScaleZ"));
		manipIDs.put(Manipulator.RotateX, scene.objects.getID("RotateX"));
		manipIDs.put(Manipulator.RotateY, scene.objects.getID("RotateY"));
		manipIDs.put(Manipulator.RotateZ, scene.objects.getID("RotateZ"));
		manipIDs.put(Manipulator.TranslateX, scene.objects.getID("TranslateX"));
		manipIDs.put(Manipulator.TranslateY, scene.objects.getID("TranslateY"));
		manipIDs.put(Manipulator.TranslateZ, scene.objects.getID("TranslateZ"));
		for(Entry<Manipulator, UUIDGenerator.ID> e : manipIDs.entrySet()) {
			manips.put(e.getValue().id, e.getKey());
		}
		
		setCurrentManipType(Manipulator.Type.TRANSLATE);
	}
	@Override
	public void dispose() {
		manipRenderer.dispose();
		unhook();
	}
	
	private void setCurrentManipType(int type) {
		switch (type) {
		case Manipulator.Type.TRANSLATE:
			currentManips[Manipulator.Axis.X] = Manipulator.TranslateX;
			currentManips[Manipulator.Axis.Y] = Manipulator.TranslateY;
			currentManips[Manipulator.Axis.Z] = Manipulator.TranslateZ;
			break;
		case Manipulator.Type.ROTATE:
			currentManips[Manipulator.Axis.X] = Manipulator.RotateX;
			currentManips[Manipulator.Axis.Y] = Manipulator.RotateY;
			currentManips[Manipulator.Axis.Z] = Manipulator.RotateZ;
			break;
		case Manipulator.Type.SCALE:
			currentManips[Manipulator.Axis.X] = Manipulator.ScaleX;
			currentManips[Manipulator.Axis.Y] = Manipulator.ScaleY;
			currentManips[Manipulator.Axis.Z] = Manipulator.ScaleZ;
			break;
		}
	}
	
	public void hook() {
		KeyboardEventDispatcher.OnKeyPressed.add(onKeyPress);
		MouseEventDispatcher.OnMouseRelease.add(onMouseRelease);
	}
	public void unhook() {
		KeyboardEventDispatcher.OnKeyPressed.remove(onKeyPress);		
		MouseEventDispatcher.OnMouseRelease.remove(onMouseRelease);
	}
	
	/**
	 * Get the transformation that should be used to draw <manip> when it is being used to manipulate <object>.
	 * 
	 * This is just the object's or parent's frame-to-world transformation, but with a rotation appended on to 
	 * orient the manipulator along the correct axis.  One problem with the way this is currently done is that
	 * the manipulator can appear very small or large, or very squashed, so that it is hard to interact with.
	 * 
	 * @param manip The manipulator to be drawn (one axis of the complete widget)
	 * @param mViewProjection The camera (not needed for the current, simple implementation)
	 * @param object The selected object
	 * @return
	 */
	public Matrix4 getTransformation(Manipulator manip, RenderCamera camera, RenderObject object) {
		Matrix4 mManip = new Matrix4();
		
		switch (manip.axis) {
		case Manipulator.Axis.X:
			Matrix4.createRotationY((float)(Math.PI / 2.0), mManip);
			break;
		case Manipulator.Axis.Y:
			Matrix4.createRotationX((float)(-Math.PI / 2.0), mManip);
			break;
		case Manipulator.Axis.Z:
			mManip.setIdentity();
			break;
		}
		if (parentSpace) {
			if (object.parent != null)
				mManip.mulAfter(object.parent.mWorldTransform);
		} else
			mManip.mulAfter(object.mWorldTransform);

		return mManip;
	}
	
	/**
	 * Apply a transformation to <b>object</b> in response to an interaction with <b>manip</b> in which the user moved the mouse from
 	 * <b>lastMousePos</b> to <b>curMousePos</b> while viewing the scene through <b>camera</b>.  The manipulation happens differently depending
 	 * on the value of ManipController.parentMode; if it is true, the manipulator is aligned with the parent's coordinate system, 
 	 * or if it is false, with the object's local coordinate system.  
	 * @param manip The manipulator that is active (one axis of the complete widget)
	 * @param camera The camera (needed to map mouse motions into the scene)
	 * @param object The selected object (contains the transformation to be edited)
	 * @param lastMousePos The point where the mouse was last seen, in normalized [-1,1] x [-1,1] coordinates.
	 * @param curMousePos The point where the mouse is now, in normalized [-1,1] x [-1,1] coordinates.
	 */
	public void applyTransformation(Manipulator manip, RenderCamera camera, RenderObject object, Vector2 lastMousePos, Vector2 curMousePos) {

		// There are three kinds of manipulators; you can tell which kind you are dealing with by looking at manip.type.
		// Each type has three different axes; you can tell which you are dealing with by looking at manip.axis.

		// For rotation, you just need to apply a rotation in the correct space (either before or after the object's current
		// transformation, depending on the parent mode this.parentSpace).

		// For translation and scaling, the object should follow the mouse.  Following the assignment writeup, you will achieve
		// this by constructing the viewing rays and the axis in world space, and finding the t values *along the axis* where the
		// ray comes closest (not t values along the ray as in ray tracing).  To do this you need to transform the manipulator axis
		// from its frame (in which the coordinates are simple) to world space, and you need to get a viewing ray in world coordinates.

		// There are many ways to compute a viewing ray, but perhaps the simplest is to take a pair of points that are on the ray,
		// whose coordinates are simple in the canonical view space, and map them into world space using the appropriate matrix operations.
		
		// You may find it helpful to structure your code into a few helper functions; ours is about 150 lines.
		
		// TODO#A3
		// Construct two rays
		Ray r1 = genCameraRay(camera, lastMousePos);
		Ray r2 = genCameraRay(camera, curMousePos);
		
		// Construct transformation plane
		RenderObject tmp = object;
		if (this.parentSpace) {
			tmp = object.parent;
		}
		Vector3 origin = new Vector3(), v1 = new Vector3(), v2 = new Vector3();
		origin.set((float)tmp.mWorldTransform.m[12],(float)tmp.mWorldTransform.m[13],(float)tmp.mWorldTransform.m[14]);
		if (manip.axis == Manipulator.Axis.X) {
			v1.set((float)tmp.mWorldTransform.m[0],(float)tmp.mWorldTransform.m[1],(float)tmp.mWorldTransform.m[2]);
		//	v2.set((float)tmp.mWorldTransform.m[4],(float)tmp.mWorldTransform.m[5],(float)tmp.mWorldTransform.m[6]);
		} else if (manip.axis == Manipulator.Axis.Y) {
			v1.set((float)tmp.mWorldTransform.m[4],(float)tmp.mWorldTransform.m[5],(float)tmp.mWorldTransform.m[6]);
		//	v2.set((float)tmp.mWorldTransform.m[8],(float)tmp.mWorldTransform.m[9],(float)tmp.mWorldTransform.m[10]);
		} else if (manip.axis == Manipulator.Axis.Z) {
			v1.set((float)tmp.mWorldTransform.m[8],(float)tmp.mWorldTransform.m[9],(float)tmp.mWorldTransform.m[10]);
		//	v2.set((float)tmp.mWorldTransform.m[0],(float)tmp.mWorldTransform.m[1],(float)tmp.mWorldTransform.m[2]);
		}
		
		Vector3 u = new Vector3(camera.mWorldTransform.m[0], camera.mWorldTransform.m[1], camera.mWorldTransform.m[2]);
		Vector3 v = new Vector3(camera.mWorldTransform.m[4], camera.mWorldTransform.m[5], camera.mWorldTransform.m[6]);
		Vector3 r = new Vector3(v1);
		v2 = constructVector(u, v, r);
		
		// Get intersection points
		Vector3 p1 = rayPlaneIntersection(r1, origin, v1, v2);
		Vector3 p2 = rayPlaneIntersection(r2, origin, v1, v2);
		// Get move distance
		float d1 = moveDist(origin, v1, p1);
		float d2 = moveDist(origin, v1, p2);
		
		//System.out.println(dist);
		
		// Construct transformation matrix
		Matrix4 transformation = null;
		if (manip.type == Manipulator.Type.TRANSLATE) {
			if (manip.axis == Manipulator.Axis.X) {
				transformation = Matrix4.createTranslation(d2 - d1, 0, 0);
			} else if (manip.axis == Manipulator.Axis.Y) {
				transformation = Matrix4.createTranslation(0, d2 - d1, 0);
			} else if (manip.axis == Manipulator.Axis.Z) {
				transformation = Matrix4.createTranslation(0, 0, d2 - d1);
			}
		} else if (manip.type == Manipulator.Type.SCALE) {
			if (manip.axis == Manipulator.Axis.X) {
				transformation = Matrix4.createScale(d2 / d1, 1, 1);
			} else if (manip.axis == Manipulator.Axis.Y) {
				transformation = Matrix4.createScale(1, d2 / d1, 1);
			} else if (manip.axis == Manipulator.Axis.Z) {
				transformation = Matrix4.createScale(1, 1, d2 / d1);
			}
		} else if (manip.type == Manipulator.Type.ROTATE) {
			float angle = (float)((curMousePos.y - lastMousePos.y) * 2);
			if (manip.axis == Manipulator.Axis.X) {
				transformation = Matrix4.createRotationX(angle);
			} else if (manip.axis == Manipulator.Axis.Y) {
				transformation = Matrix4.createRotationY(angle);
			} else if (manip.axis == Manipulator.Axis.Z) {
				transformation = Matrix4.createRotationZ(angle);
			}
		}
		
		// Transform depend on space
		if (this.parentSpace) {
			object.sceneObject.transformation.mulAfter(transformation);
		} else {
			object.sceneObject.transformation.mulBefore(transformation);
		}
		
	}
	
	private Vector3 constructVector(Vector3 u, Vector3 v, Vector3 r) {
		float A = u.x * r.x + u.y * r.y + u.z * r.z;
		float B = v.x * r.x + v.y * r.y + v.z * r.z;
		if (A == 0) return new Vector3(u);
		Vector3 ans = new Vector3(v);
		Vector3 tmp = new Vector3(u);
		tmp.mul(-B / A);
		ans.add(tmp);
		return ans.normalize();
	}
	
	private Ray genCameraRay(RenderCamera camera, Vector2 clickPos) {
		Ray ray = new Ray();
		ray.origin.set(camera.mWorldTransform.m[12], camera.mWorldTransform.m[13], camera.mWorldTransform.m[14]);
		Vector4 p = new Vector4(
				(float)(clickPos.x * camera.sceneCamera.imageSize.x / 2), 
				(float)(clickPos.y * camera.sceneCamera.imageSize.y / 2), 
				-(float)camera.sceneCamera.zPlanes.x, 1);
		p = camera.mWorldTransform.mul(p);
		//System.out.println(p);
		ray.direction.set(p.x - ray.origin.x, p.y - ray.origin.y, p.z - ray.origin.z);
		ray.direction.normalize();
		return ray;
	}
	private Vector3 rayPlaneIntersection(Ray ray, Vector3 origin, Vector3 v1, Vector3 v2) {
		Vector3 v = new Vector3();
		
		double a = v1.x, d = v2.x, g = -ray.direction.x, j = ray.origin.x - origin.x;
		double b = v1.y, e = v2.y, h = -ray.direction.y, k = ray.origin.y - origin.y;
		double c = v1.z, f = v2.z, i = -ray.direction.z, l = ray.origin.z - origin.z;
		
		double M = a* (e * i - h * f) + b * (g * f - d * i) + c * (d * h - e * g);
		
		if (M != 0) {
			double t = -(f * (a * k - j * b) + e * (j * c - a * l) + d * (b * l - k * c)) / M;
			v.set((float)(ray.direction.x * t), (float)(ray.direction.y * t), (float)(ray.direction.z * t));
			v.add((float)ray.origin.x, (float)ray.origin.y, (float)ray.origin.z);
		}
		return v;
	}
	
	private float moveDist(Vector3 origin, Vector3 dir, Vector3 p1) {
		Vector3 v = new Vector3(p1);
		v.sub(origin);
		return v.dot(dir) / dir.len();
	}
	
	public void checkMouse(int mx, int my, RenderCamera camera) {
		Vector2 curMousePos = new Vector2(mx, my).add(0.5f).mul(2).div(camera.viewportSize.x, camera.viewportSize.y).sub(1);
		if(curMousePos.x != lastMousePos.x || curMousePos.y != lastMousePos.y) {
			if(selectedManipulator != null && currentObject != null) {
				applyTransformation(selectedManipulator, camera, currentObject, lastMousePos, curMousePos);
				scene.sendEvent(new SceneTransformationEvent(currentObject.sceneObject));
			}
			lastMousePos.set(curMousePos);
		}
	}

	public void checkPicking(Renderer renderer, RenderCamera camera, int mx, int my) {
		if(camera == null) return;
		
		// Pick An Object
		renderer.beginPickingPass(camera);
		renderer.drawPassesPick();
		if(currentObject != null) {
			// Draw Object Manipulators
			GL11.glClearDepth(1.0);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			
			DepthState.DEFAULT.set();
			BlendState.OPAQUE.set();
			RasterizerState.CULL_NONE.set();
			
			drawPick(camera, currentObject, renderer.pickProgram);
		}
		int id = renderer.getPickID(Mouse.getX(), Mouse.getY());
		
		selectedManipulator = manips.get(id);
		if(selectedManipulator != null) {
			// Begin Manipulator Operations
			System.out.println("Selected Manip: " + selectedManipulator.type + " " + selectedManipulator.axis);
			return;
		}
		
		SceneObject o = scene.objects.get(id);
		if(o != null) {
			System.out.println("Picked An Object: " + o.getID().name);
			if(scenePanel != null) {
				scenePanel.select(o.getID().name);
				propWindow.tabToForefront("Object");
			}
			currentObject = rEnv.findObject(o);
		}
		else if(currentObject != null) {
			currentObject = null;
		}
	}
	
	public void draw(RenderCamera camera) {
		if(currentObject == null) return;
		
		DepthState.NONE.set();
		BlendState.ALPHA_BLEND.set();
		RasterizerState.CULL_CLOCKWISE.set();
		
		for(Manipulator manip : currentManips) {
			Matrix4 mTransform = getTransformation(manip, camera, currentObject);
			manipRenderer.render(mTransform, camera.mViewProjection, manip.type, manip.axis);
		}
		
		DepthState.DEFAULT.set();
		BlendState.OPAQUE.set();
		RasterizerState.CULL_CLOCKWISE.set();
		
		for(Manipulator manip : currentManips) {
			Matrix4 mTransform = getTransformation(manip, camera, currentObject);
			manipRenderer.render(mTransform, camera.mViewProjection, manip.type, manip.axis);
		}

}
	public void drawPick(RenderCamera camera, RenderObject ro, PickingProgram prog) {
		for(Manipulator manip : currentManips) {
			Matrix4 mTransform = getTransformation(manip, camera, ro);
			prog.setObject(mTransform, manipIDs.get(manip).id);
			manipRenderer.drawCall(manip.type, prog.getPositionAttributeLocation());
		}
	}
	
}
