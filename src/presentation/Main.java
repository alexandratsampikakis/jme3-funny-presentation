package presentation;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.shape.Sphere;


public class Main extends SimpleApplication {
    
    private TerrainQuad terrain;
    Material matRock;
    Material matWire;
    boolean wireframe = false;
    boolean triPlanar = false;
    protected BitmapText hintText;
    PointLight pl;
    Geometry lightMdl;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    
    private Node playerNode;
    private Geometry playerGeometry;
    private RigidBodyControl playerControl;
    private BulletAppState bulletAppState;
    
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private boolean left = false,
            right = false,
            up = false,
            down = false;

    
    
    public static void main(String[] args) {
        
        Main app = new Main();
        
        app.start();
    }
    
    

    @Override
    public void simpleInitApp() {
        
        setupSky();
        
        setupTerrain();
        
        setupLight();
        
        setupPlayer();
        
        setupCamera();
    }

    
    
    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    
    
    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    
    private void setupSky() {
        
        rootNode.attachChild(SkyFactory.createSky(
                assetManager, "Textures/Sky/Bright/BrightSky.dds", false));
    }
    
    
    
    private void setupTerrain() {
        
        // TERRAIN TEXTURE material
        matRock = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
        matRock.setBoolean("useTriPlanarMapping", false);

        // ALPHA map (for splat textures)
//        matRock.setTexture("Alpha", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        // HEIGHTMAP image (for the terrain heightmap)
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains128.png");

        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex1", grass);
        matRock.setFloat("Tex1Scale", grassScale);

        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex2", dirt);
        matRock.setFloat("Tex2Scale", dirtScale);

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex3", rock);
        matRock.setFloat("Tex3Scale", rockScale);

        // WIREFRAME material
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);

        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 1f);
            heightmap.load();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        /*
         * Here we create the actual terrain. The tiles will be 33x33, and the total size of the
         * terrain will be 129x129. It uses the heightmap we created to generate the height values.
         */
        terrain = new TerrainQuad("terrain", 33, 129, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator( new DistanceLodCalculator(33, 2.7f) ); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matRock);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(8f, 0.5f, 8f);
        rootNode.attachChild(terrain);
    }
    
    
    private void setupLight() {
        
        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.5f, -1f, -0.5f)).normalize());
        rootNode.addLight(light);
    }
    
    
    private void setupPlayer() {
        
        float radius = 2;
        playerNode = new Node("Player");
        playerGeometry = new Geometry("PlayerGeometry", new Sphere(100, 100, radius));
        rootNode.attachChild(playerNode);
        playerNode.attachChild(playerGeometry);
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        playerGeometry.setMaterial(material);
        playerNode.setLocalTranslation(new Vector3f(0, 20, 0));
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        float stepHeight = 500f;
        playerControl = new RigidBodyControl(sphereShape, stepHeight);
        playerNode.addControl(playerControl);
        //playerControl.setRestitution(0.001f);
        playerControl.setFriction(12f);
        playerControl.setGravity(new Vector3f(1.0f,1.0f,1.0f)); //??
        playerNode.setShadowMode(ShadowMode.CastAndReceive);
//        bulletAppState.getPhysicsSpace().add(playerControl);
    
    }
    
    
    private void setupCamera() {
        
        flyCam.setEnabled(false);
        ChaseCamera camera = new ChaseCamera(cam, playerNode, inputManager);
        camera.setDragToRotate(false);
    }
   
    
    
}
