package presentation;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
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
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.effect.ParticleEmitter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.List;


public class Main extends SimpleApplication {
    
    private TerrainQuad terrain;
    RigidBodyControl terrainPhysicsNode;
    Material matRock;
    Material matWire;
    boolean wireframe = false;
    boolean triPlanar = false;
    protected BitmapText hintText;
    PointLight pl;
    Geometry lightMdl;
    
    private Node playerNode;
    private Geometry playerGeometry;
    private RigidBodyControl playerControl;
    private BulletAppState bulletAppState;
    
    float x, y, z, ry;
    private List<Node> shootables;
    private float xCamCoord, yCamCoord, zCamCoord;
    
    // Not used
    ParticleEmitter fire, debris;
    Spatial wall;
    Geometry mark;
    
    
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
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        setupSky();
        
        setupTerrain();
        
        setupLight();
        
        setupPlayer();
        
        setupCamera();
        
        setupKeys();
    }

    
    
    @Override
    public void simpleUpdate(float tpf) {
        Vector3f camDir = cam.getDirection().clone();
        Vector3f camLeft = cam.getLeft().clone();
        
        camDir.y = 0;  
        camLeft.y = 0;
        
        walkDirection.set(0, 0, 0);

        if (left) {
            walkDirection.addLocal(camDir.negate());
        }
        
        if (right) {
            walkDirection.addLocal(camDir);
        }
        
        if (up) {
            walkDirection.addLocal(camLeft);
        }
        
        if (down) {
            walkDirection.addLocal(camLeft.negate());
        }
        
        playerControl.setAngularVelocity(walkDirection.mult(10f));
    }

    
    
    @Override
    public void simpleRender(RenderManager rm) {
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
        matRock.setTexture("Alpha", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex1", grass);
        matRock.setFloat("Tex1Scale", 64f);

        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex2", dirt);
        matRock.setFloat("Tex2Scale", 32f);

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex3", rock);
        matRock.setFloat("Tex3Scale", 128f);

        // WIREFRAME material
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);

        // HEIGHTMAP image (for the terrain heightmap)
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        
        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.1f);
            heightmap.load();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        /*
         * Here we create the actual terrain. The tiles will be 33x33, and the total size of the
         * terrain will be 129x129. It uses the heightmap we created to generate the height values.
         */
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);
        terrain.setMaterial(matRock);
//        terrain.setLocalTranslation(0, -100, 0);
//        terrain.setLocalScale(8f, 0.5f, 8f);
        terrain.setLocalTranslation(x, y, z);
	terrain.setLocalScale(2f, 1f, 2f);
        
        terrainPhysicsNode = new RigidBodyControl(CollisionShapeFactory.createMeshShape(terrain), 0);
        terrain.addControl(terrainPhysicsNode);
        rootNode.attachChild(terrain);
        
        getPhysicsSpace().add(terrainPhysicsNode);
        
        setupTeamMembers();
    }
    
    
    private void setupTeamMembers() {
        String[] mitvTeam = new String[10];
        mitvTeam[0] = "Erik";
        mitvTeam[1] = "Bengt";
        mitvTeam[2] = "Foteini";
        mitvTeam[3] = "Johan";
        mitvTeam[4] = "Albert";
        mitvTeam[5] = "Filipe";
        mitvTeam[6] = "Alex";
        mitvTeam[7] = "Calle";
        mitvTeam[8] = "Miguel";
        mitvTeam[9] = "Thomas";
        
        shootables = new ArrayList<Node>();
        
        int counter = 0;
        double min = -500.0;
        int max = 200;
        
        for (int i=0; i<mitvTeam.length; i++) {
            Node newTeamMember = new Node(mitvTeam[counter]);
            shootables.add(newTeamMember);
            
            x = (float) (min * Math.random()) + max;
            z = (float) (min * Math.random()) + max;
            ry = (float) (min * Math.random()) + max;
            
            getTeamMember(newTeamMember);
            
            counter++;
        }
    }
    
    
    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
    
    
    
    private void setupLight() {
        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.5f, -1f, -0.5f)).normalize());
        rootNode.addLight(light);
    }
    
    
    public Spatial drawBox() {
        Box box = new Box(Vector3f.ZERO, 3.5f, 3.5f, 1.0f);
        wall = new Geometry("Box", box);
        Material mat_brick = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat_brick.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        wall.setMaterial(mat_brick);
        Vector2f xz = new Vector2f(x, z);
        y = terrain.getHeight(xz);
        wall.setLocalTranslation(x, y, z);
        wall.rotate(x, ry, z);
        terrain.attachChild(wall);
        return wall;
    }
    
    
    public void getTeamMember(Node newTeamMember) {
        float radius = 2;
        float stepHeight = 500f;
        
        terrain.attachChild(newTeamMember);
        
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/gustav.png"));
        
        Geometry memberGeometry = new Geometry(newTeamMember.getName(), new Sphere(100, 100, radius));
        memberGeometry.setMaterial(material);
        
        newTeamMember.attachChild(memberGeometry);
        newTeamMember.setLocalTranslation(new Vector3f(x, 20, z));
        
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        
        RigidBodyControl memberControl = new RigidBodyControl(sphereShape, stepHeight);
        newTeamMember.addControl(memberControl);
        memberControl.setFriction(10f);
        memberControl.setGravity(new Vector3f(1.0f, 1.0f, 1.0f));
        newTeamMember.setShadowMode(ShadowMode.CastAndReceive);
        
        bulletAppState.getPhysicsSpace().add(memberControl);
    }
    
    
    private void setupPlayer() {
        
        float radius = 2;
        playerNode = new Node("Player");
        playerGeometry = new Geometry("PlayerGeometry", new Sphere(100, 100, radius));
        rootNode.attachChild(playerNode);
        playerNode.attachChild(playerGeometry);
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/gustav.png"));
        playerGeometry.setMaterial(material);
        playerNode.setLocalTranslation(new Vector3f(0, 20, 0));
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        float stepHeight = 500f;
        playerControl = new RigidBodyControl(sphereShape, stepHeight);
        playerNode.addControl(playerControl);
        playerControl.setFriction(10f);
        playerControl.setGravity(new Vector3f(1.0f, 1.0f, 1.0f));
        playerNode.setShadowMode(ShadowMode.CastAndReceive);
        bulletAppState.getPhysicsSpace().add(playerControl);
        
        xCamCoord = 0;
        yCamCoord = 20;
        zCamCoord = 0;
    }
    
    
    
    private void setupCamera() {
        flyCam.setEnabled(true);
        ChaseCamera camera = new ChaseCamera(cam, playerNode, inputManager);
        camera.setDragToRotate(false);
    }
    
    
   
    private void setupKeys() {
        inputManager.addMapping("CharLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("CharRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("CharForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("CharBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(actionListener, "CharLeft");
        inputManager.addListener(actionListener, "CharRight");
        inputManager.addListener(actionListener, "CharForward");
        inputManager.addListener(actionListener, "CharBackward");
    }
    
    
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String binding, boolean isPressed, float tpf) {
            
            if (binding.equals("CharLeft")) {
                
                if (isPressed) {
                    left = true;
                } else {
                    left = false;
                }
                
            } else if (binding.equals("CharRight")) {
                
                if (isPressed) {
                    right = true;
                } else {
                    right = false;
                }
                
            } else if (binding.equals("CharForward")) {
                
                if (isPressed) {
                    up = true;
                } else {
                    up = false;
                }
                
            } else if (binding.equals("CharBackward")) {
                
                if (isPressed) {
                    down = true;
                } else {
                    down = false;
                }
            }
        }
    };
    
}
