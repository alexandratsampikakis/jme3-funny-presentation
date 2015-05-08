package presentation;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
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
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import presentation.models.TeamMember;


public class Main extends SimpleApplication { //implements PhysicsCollisionListener {
    
    protected static final Logger logger = Logger.getLogger(Main.class.getName());
    
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
    private List<TeamMember> teamMembers;
    private float xCamCoord, yCamCoord, zCamCoord;
    private List<Node> hitMembers;
    private List<Geometry> shootablesGeom;
    private int playerCounter;
    
    // Not used
    ParticleEmitter fire, debris;
    Spatial wall;
    Geometry mark;
    private Geometry selectedCollisionObject;
    
    private boolean shouldRestartApp;
    protected static Main app;
    protected static AppSettings settings;
    
    
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    
    private boolean left = false,
            right = false,
            up = false,
            down = false;

    
    
    public static void main(String[] args) {
        app = new Main();
        
        settings = new AppSettings(true);
        app.setShowSettings(false);
        app.setSettings(settings);
        
        app.start();
    }
    
    

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        teamMembers = new ArrayList<TeamMember>();
        shootables = new ArrayList<Node>();
        shootablesGeom = new ArrayList<Geometry>();
        
        setupSky();
        
        setupTerrain();
        
        setupLight();
        
//        setupPlayer();
        initCrossHairs();
        initMark();
        
//        setupCamera();
        
        setupKeys();
        
        flyCam.setMoveSpeed(200);
        
//        bulletAppState.getPhysicsSpace().addCollisionListener(this);
    }
    

    
    
    @Override
    public void simpleUpdate(float tpf) {
//        Vector3f camDir = cam.getDirection().clone();
//        Vector3f camLeft = cam.getLeft().clone();
//        
//        camDir.y = 0;  
//        camLeft.y = 0;
//        
//        walkDirection.set(0, 0, 0);

//        if (left) {
//            walkDirection.addLocal(camDir.negate());
//        }
//        
//        if (right) {
//            walkDirection.addLocal(camDir);
//        }
//        
//        if (up) {
//            walkDirection.addLocal(camLeft);
//        }
//        
//        if (down) {
//            walkDirection.addLocal(camLeft.negate());
//        }
//        
//        playerControl.setAngularVelocity(walkDirection.mult(10f));
        
        if (shouldRestartApp)
        {
            // TODO
        }
        
        for (int i=0; i<teamMembers.size(); i++) {
            if (teamMembers.get(i).hasTeamMemberBeenShot()) {
                shootablesGeom.get(i).removeFromParent();
                playerCounter++;
            }
        }
        
        if (playerCounter == 13) {
            mark.setLocalTranslation(new Vector3f(0, 20, 0)); // Let's interact - we mark the hit with a red dot.
            terrain.attachChild(mark);
            
            // Red mark in the middle
            // TODO add all team members around to this view
            // make all geoms jump/rotate
        }
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
        terrain.setLocalTranslation(x, y, z);
	terrain.setLocalScale(2f, 1f, 2f);
        
        terrainPhysicsNode = new RigidBodyControl(CollisionShapeFactory.createMeshShape(terrain), 0);
        terrain.addControl(terrainPhysicsNode);
        rootNode.attachChild(terrain);
        
        getPhysicsSpace().add(terrainPhysicsNode);
        
        setupTeamMembers();
    }
    
    
    private void setupTeamMembers() {
        String[] mitvTeam = new String[13];
        mitvTeam[0] = "erik";
        mitvTeam[1] = "bengt";
        mitvTeam[2] = "foteini";
        mitvTeam[3] = "johan";
        mitvTeam[4] = "albert";
        mitvTeam[5] = "filipe";
        mitvTeam[6] = "alexandra";
        mitvTeam[7] = "calle";
        mitvTeam[8] = "miguel";
        mitvTeam[9] = "thomas";
        mitvTeam[10] = "mattias";
        mitvTeam[11] = "sara";
        mitvTeam[12] = "gustav";
        
        int counter = 0;
        double min = -200.0;
        int max = 200;
        
        for (int i=0; i<mitvTeam.length; i++) {
            Node newTeamMember = new Node(mitvTeam[counter]);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Textures/");
            sb.append(newTeamMember.getName());
            sb.append(".png");
            
            String imageUrl = sb.toString();
            
            TeamMember member = new TeamMember(newTeamMember.getName(), imageUrl, false);
            teamMembers.add(member);
            
            shootables.add(newTeamMember);
            
            x = (float) (min * Math.random()) + max;
            z = (float) (min * Math.random()) + max;
            ry = (float) (min * Math.random()) + max;
            
            getTeamMember(newTeamMember, imageUrl);
            
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
    
    
    
    public void getTeamMember(Node newTeamMember, String imageUrl) {
        float radius = 5;
        float stepHeight = 600f;
        
        rootNode.attachChild(newTeamMember);
        
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setTexture("DiffuseMap", assetManager.loadTexture(imageUrl));
        
        Geometry memberGeometry = new Geometry(newTeamMember.getName(), new Sphere(100, 100, radius));
        memberGeometry.setMaterial(material);
        
        shootablesGeom.add(memberGeometry);
        
        newTeamMember.attachChild(memberGeometry);
        newTeamMember.setLocalTranslation(new Vector3f(x, 20, z));
        
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        
        RigidBodyControl memberControl = new RigidBodyControl(sphereShape, stepHeight);
        newTeamMember.addControl(memberControl);
        memberControl.setFriction(20f);
        memberControl.setGravity(new Vector3f(1.0f, 1.0f, 1.0f));
        newTeamMember.setShadowMode(ShadowMode.CastAndReceive);
        
        bulletAppState.getPhysicsSpace().add(memberControl);
    }
    
    
    private void setupPlayer() {
//        float radius = 2;
//        playerNode = new Node("Player");
//        playerGeometry = new Geometry("PlayerGeometry", new Sphere(100, 100, radius));
//        rootNode.attachChild(playerNode);
//        playerNode.attachChild(playerGeometry);
//        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/gustav.png"));
//        playerGeometry.setMaterial(material);
//        playerNode.setLocalTranslation(new Vector3f(0, 20, 0));
//        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
//        float stepHeight = 500f;
//        playerControl = new RigidBodyControl(sphereShape, stepHeight);
//        playerNode.addControl(playerControl);
//        playerControl.setFriction(8f);
//        playerControl.setGravity(new Vector3f(1.0f, 1.0f, 1.0f));
//        playerNode.setShadowMode(ShadowMode.CastAndReceive);
//        bulletAppState.getPhysicsSpace().add(playerControl);
        
        xCamCoord = 0;
        yCamCoord = 20;
        zCamCoord = 0;
    }
    
    
    protected void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        xCamCoord = settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2;
        yCamCoord = settings.getHeight() / 2 + ch.getLineHeight() / 2;
        zCamCoord = 0;
        ch.setLocalTranslation(xCamCoord, yCamCoord, zCamCoord);
        guiNode.attachChild(ch);
    }
    
    
    
    private void setupCamera() {
        flyCam.setEnabled(true);
        ChaseCamera camera = new ChaseCamera(cam, playerNode, inputManager);
        camera.setDragToRotate(false);
    }
    
    
   
    private void setupKeys() {
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_SPACE), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Shoot");
        
        inputManager.addMapping("Return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(actionListener, "Return");
        
        inputManager.addMapping("Restart", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(actionListener, "Restart");
        
//        inputManager.addMapping("CharLeft", new KeyTrigger(KeyInput.KEY_A));
//        inputManager.addMapping("CharRight", new KeyTrigger(KeyInput.KEY_D));
//        inputManager.addMapping("CharForward", new KeyTrigger(KeyInput.KEY_W));
//        inputManager.addMapping("CharBackward", new KeyTrigger(KeyInput.KEY_S));
//        inputManager.addMapping("Return", new KeyTrigger(KeyInput.KEY_SPACE));
//        
//        inputManager.addListener(actionListener, "CharLeft");
//        inputManager.addListener(actionListener, "CharRight");
//        inputManager.addListener(actionListener, "CharForward");
//        inputManager.addListener(actionListener, "CharBackward");
//        inputManager.addListener(actionListener, "Return");
    }
    
    
    private ActionListener actionListener = new ActionListener() {
        
        public void onAction(String name, boolean keyPressed, float tpf) {
            
            if (name.equals("Shoot") && !keyPressed) {
                
                CollisionResults results = new CollisionResults(); // 1. Reset results list.
                Ray ray = new Ray(cam.getLocation(), cam.getDirection()); // 2. Aim the ray from cam loc to cam direction.
                
                if (hitMembers == null) {
                    hitMembers = new ArrayList<Node>();
                }
                
                for (Node member : shootables) {
                    member.collideWith(ray, results);
                    hitMembers.add(member);
                }

                System.out.println("----- Collisions? " + results.size() + "-----"); // 4. Print the results
                
                for (int i = 0; i < results.size(); i++) {
                    
                    float dist = results.getCollision(i).getDistance();
                    Vector3f pt = results.getCollision(i).getContactPoint();
                    String hit = results.getCollision(i).getGeometry().getName();
                    
                    for(TeamMember member : teamMembers) {
                        
                        if (member.getName().equals(hit) && !member.hasTeamMemberBeenShot()) {
                            
                            member.setTeamMemberHasBeenShot();
                            
                            setFire(hitMembers.get(i));
                            fire.move(pt);
                            debris.move(pt);
                            
                            if (member.getName().equals("alexandra") ||
                                member.getName().equals("foteini") ||
                                member.getName().equals("erik") ||
                                member.getName().equals("gustav") ||
                                member.getName().equals("albert")) {
                                
                                StringBuilder sb = new StringBuilder();
                                sb.append("Sounds/");
                                sb.append(member.getName());
                                sb.append("sound.wav");

                                AudioNode backgroundMusic = new AudioNode(assetManager, sb.toString(), false);
//                                                              
                                backgroundMusic.setPositional(false);
                                backgroundMusic.setLooping(false);
                                backgroundMusic.setVolume(10);
                                rootNode.attachChild(backgroundMusic);
                                backgroundMusic.playInstance();
                            }
                        }
                    }
                    
                    System.out.println("* Collision #" + i);
                    System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");
                }
            }
            
            else if (name.equals("Return") && keyPressed) {
                shouldRestartApp = true;
            }
            
            else if (name.equals("Restart") && !keyPressed) {
                shouldRestartApp = true;
            }
        }
    };
    
    
//    private ActionListener actionListener = new ActionListener() {
//
//        public void onAction(String binding, boolean isPressed, float tpf) {
//            
//            if (binding.equals("CharLeft")) {
//                
//                if (isPressed) {
//                    left = true;
//                } else {
//                    left = false;
//                }
//                
//            } else if (binding.equals("CharRight")) {
//                
//                if (isPressed) {
//                    right = true;
//                } else {
//                    right = false;
//                }
//                
//            } else if (binding.equals("CharForward")) {
//                
//                if (isPressed) {
//                    up = true;
//                } else {
//                    up = false;
//                }
//                
//            } else if (binding.equals("CharBackward")) {
//                
//                if (isPressed) {
//                    down = true;
//                } else {
//                    down = false;
//                }
//                
//            } else if (binding.equals("Return")) {
//                if (isPressed) {
//                   context.restart(); // TODO do not work
//                }
//            }
//        }
//    };

    
    
//    public void collision(PhysicsCollisionEvent event) {
//        
//        logger.log(Level.INFO, "Collition detected on A : " + event.getNodeA().getName());
//
//        logger.log(Level.INFO, "Collition detected on: B " + event.getNodeB().getName());
//        
////        if (!event.getNodeB().getName().equals("terrain") &&
////            event.getNodeA().getName().equals("Player")) {
////            
////        logger.log(Level.INFO, "Collition detected on: A " + event.getNodeA().getName());
////        logger.log(Level.INFO, "Collition detected on: B " + event.getNodeB().getName());
////        
////            for (Node teamMember : shootables) {
////
////                if (event.getNodeB().getName().equals(teamMember.getName())) {
////
////                    logger.log(Level.INFO, "Collition detected on B: " + teamMember.getName());
////
//////                    teamMember.setLocalTranslation(0, 20, 0);
//////
//////                    for (TeamMember member : teamMembers) {
//////
//////                        if (member.getName().equals(teamMember.getName())) {
//////                            member.setTeamMemberHasBeenShot();
//////                        }
//////                    }
////
////                }
////            }
////        }
//    }
    
    
    public void setFire(Node member) {
        fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        fire.setMaterial(mat_red);
        fire.setImagesX(2);
        fire.setImagesY(2); // 2x2 texture animation
        fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f)); // red
        fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(1.5f);
        fire.setEndSize(0.1f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(3f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);
        rootNode.attachChild(fire);

        debris = new ParticleEmitter("Debris", ParticleMesh.Type.Triangle, 10);
        Material debris_mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        debris_mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/Debris.png"));
        debris.setMaterial(debris_mat);
        debris.setImagesX(3);
        debris.setImagesY(3); // 3x3 texture animation
        debris.setRotateSpeed(4);
        debris.setSelectRandomImage(true);
        debris.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 4, 0));
        debris.setStartColor(ColorRGBA.White);
        debris.setGravity(0, 6, 0);
        debris.getParticleInfluencer().setVelocityVariation(.60f);
        rootNode.attachChild(debris);
        debris.emitAllParticles();
    }
    
    protected void initMark() {
        Sphere sphere = new Sphere(100, 100, 2);
        mark = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mark_mat);
    }
    
}
