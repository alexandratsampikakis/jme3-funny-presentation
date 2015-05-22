package presentation;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
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
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.MotionPathListener;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import presentation.models.TeamMember;


public class Main extends SimpleApplication {
    
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
    
    private BulletAppState bulletAppState;
    
    float x, y, z, ry;
    private List<Node> shootables;
    private List<TeamMember> teamMembers;
    private List<Node> hitMembers;
    private List<Geometry> shootablesGeom;
    private int playerCounter;
    
    ParticleEmitter fire, debris;
    Geometry mark;
    private BitmapText ch;
    
    private boolean shouldRestartApp;
    protected static Main app;
    
    private MotionPath path;
    private List<MotionEvent> motionControlList;
    private BitmapText wayPointsText;
    
    private long tStart;
    private long tEnd;

    
    
    public static void main(String[] args) {
        app = new Main();
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
        motionControlList = new ArrayList<MotionEvent>();
        
        playerCounter = 0;
        
        setupSky();
        
        setupTerrain();
        
        setupWalls();
        
        setupLight();
        
        initCrossHairs();
        
        initMark();
        
        setupKeys();
        
        setupMotionPath();
        
        flyCam.setMoveSpeed(200);
        
        tStart = System.currentTimeMillis();
    }
    

    
    
    @Override
    public void simpleUpdate(float tpf) {    
        if (shouldRestartApp) {
            // TODO
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
         * Here we create the actual terrain. The tiles will be 65x65, and the total size of the
         * terrain will be 512x512. It uses the heightmap we created to generate the height values.
         */
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);
        terrain.setMaterial(matRock);
        terrain.setLocalTranslation(0, 0, 0);
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
        
        double min = -200.0;
        int max = 200;
        
        for (int i=0; i<mitvTeam.length; i++) {
            Node newTeamMember = new Node(mitvTeam[i]);
            
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
            
            getTeamMember(newTeamMember, imageUrl, new Vector3f(x, 30, z));
        }
    }
    
    
    
    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
    
    
    
    private void setupLight() {
        DirectionalLight light = new DirectionalLight();
        rootNode.addLight(light);
    }
    
    
    
    public void getTeamMember(Node newTeamMember, String imageUrl, Vector3f placement) {
        float radius = 5;
        float stepHeight = 600f;
        
        rootNode.attachChild(newTeamMember);
        
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setTexture("DiffuseMap", assetManager.loadTexture(imageUrl));
        
        Geometry memberGeometry = new Geometry(newTeamMember.getName(), new Sphere(100, 100, radius));
        memberGeometry.setMaterial(material);
        
        shootablesGeom.add(memberGeometry);
        
        newTeamMember.attachChild(memberGeometry);
        newTeamMember.setLocalTranslation(placement);
        
        SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
        
        RigidBodyControl memberControl = new RigidBodyControl(sphereShape, stepHeight);
        newTeamMember.addControl(memberControl);
        memberControl.setFriction(20f);
        memberControl.setGravity(new Vector3f(1.0f, 1.0f, 1.0f));
        newTeamMember.setShadowMode(ShadowMode.CastAndReceive);
        
        bulletAppState.getPhysicsSpace().add(memberControl);
    }
    
    
    
    protected void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");
        int x = settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2;
        float y = (settings.getHeight() / 2 + ch.getLineHeight() / 2);
        int z = 0;
        ch.setLocalTranslation(x, y, z);
        guiNode.attachChild(ch);
        cam.setLocation(new Vector3f(1, 20, 1));
    }
    
    
   
    private void setupKeys() {
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_SPACE), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Shoot");
        
        inputManager.addMapping("Return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(actionListener, "Return");
        
        inputManager.addMapping("Restart", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(actionListener, "Restart");
    }
    
    
    private ActionListener actionListener = new ActionListener() {
        
        public void onAction(String name, boolean keyPressed, float tpf) {
            
            if (name.equals("Shoot") && !keyPressed) {
                
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());

                if (hitMembers == null) {
                    hitMembers = new ArrayList<Node>();
                }

                for (Node member : shootables) {
                    member.collideWith(ray, results);
                    hitMembers.add(member);
                }

//                System.out.println("----- Collisions? " + results.size() + "-----");

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

                                backgroundMusic.setPositional(false);
                                backgroundMusic.setLooping(false);
                                backgroundMusic.setVolume(10);
                                rootNode.attachChild(backgroundMusic);
                                backgroundMusic.playInstance();
                            }
                        }
                    }

//                    System.out.println("* Collision #" + i);
//                    System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");

                    updateGameStatus();
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
    
    
    
    private void addSouthWall() {
        Box box = new Box(512, 50, 1);
        Spatial wall = new Geometry("Box", box);
        Material mat_brick = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        mat_brick.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        wall.setMaterial(mat_brick);
        wall.setLocalTranslation(0, 0, -512);
        
        RigidBodyControl wallControl = new RigidBodyControl(CollisionShapeFactory.createMeshShape(wall), 0);
        wall.addControl(wallControl);
        getPhysicsSpace().add(wallControl);
        rootNode.attachChild(wall);
    }
    
    
    
    private void addNorthWall() {
        Box box = new Box(512, 50, 1);
        Spatial wall = new Geometry("Box", box);
        Material mat_brick = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        mat_brick.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        wall.setMaterial(mat_brick);
        wall.setLocalTranslation(0, 0, 512);
        
        RigidBodyControl wallControl = new RigidBodyControl(CollisionShapeFactory.createMeshShape(wall), 0);
        wall.addControl(wallControl);
        getPhysicsSpace().add(wallControl);
        rootNode.attachChild(wall);
    }
    
    
    
    private void addWestWall() {
        Box box = new Box(1, 50, 512);
        Spatial wall = new Geometry("Box", box);
        Material mat_brick = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        mat_brick.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        wall.setMaterial(mat_brick);
        wall.setLocalTranslation(-512, 0, 0);
        
        RigidBodyControl wallControl = new RigidBodyControl(CollisionShapeFactory.createMeshShape(wall), 0);
        wall.addControl(wallControl);
        getPhysicsSpace().add(wallControl);
        rootNode.attachChild(wall);
    }
    
    
    
    private void addEastWall() {
        Box box = new Box(1, 50, 512);
        Spatial wall = new Geometry("Box", box);
        Material mat_brick = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        mat_brick.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        wall.setMaterial(mat_brick);
        wall.setLocalTranslation(512, 0, 0);
        
        RigidBodyControl wallControl = new RigidBodyControl(CollisionShapeFactory.createMeshShape(wall), 0);
        wall.addControl(wallControl);
        getPhysicsSpace().add(wallControl);
        rootNode.attachChild(wall);
    }
    
    
    
    private void setupWalls() {
        addEastWall();
        addNorthWall();
        addSouthWall();
        addWestWall();
    }
    
    
    
    private void updateGameStatus() {
//        System.out.println("playerCounter: " + playerCounter);
        
        for (int i=0; i<teamMembers.size(); i++) {
            if (teamMembers.get(i).hasTeamMemberBeenShot()) {
                rootNode.detachChildNamed(shootables.get(i).getName());
            }
        }

        if (IsGameFinished()) {
            tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            double elapsedSeconds = tDelta / 1000.0;

            mark.setLocalTranslation(new Vector3f(0, 40, 0));
            terrain.attachChild(mark);

            System.out.println("We are now done!!!");

            /* Print time to kill all */
            ch.setSize(70f);
            ch.setText("Time to kill all: " + elapsedSeconds + " seconds");
            
            /* Remove input listener for shooting */
            inputManager.removeListener(actionListener);

            putAllMembersBackInTerrain();
            makeAllMembersMove();
            
            wayPointsText.setText("                    YOU WIN! \n"
                    + "But as you see, the team is not collected and all over the place as always so,\n"
                    + "                                YOU LOSE!");
            wayPointsText.setLocalTranslation((cam.getWidth() - wayPointsText.getLineWidth()) / 2, cam.getHeight(), 0);
        }
    }
    
    
    private boolean IsGameFinished() {
        boolean IsGameFinished = true;
        
        for (int i=0; i<teamMembers.size(); i++) {
            if (!teamMembers.get(i).hasTeamMemberBeenShot()) {
                IsGameFinished = false;
                break;
            }
        }
        
        return IsGameFinished;
    }
    
    
    
    private void putAllMembersBackInTerrain() {
        for (int i=0; i<teamMembers.size(); i++) {
            if (teamMembers.get(i).hasTeamMemberBeenShot()) {
                Node member = shootables.get(i);
                rootNode.attachChild(member);
            }
        }
    }
    
    
    
    private void setupMotionPath() {
        
        for (int i=0; i<shootablesGeom.size(); i++) {
            
            path = new MotionPath();
            float yy = shootablesGeom.get(i).getLocalTranslation().y;
            
            path.addWayPoint(new Vector3f(100, yy, 0));
            path.addWayPoint(new Vector3f(100, yy, 0));
            path.addWayPoint(new Vector3f(-400, yy, 0));
            path.addWayPoint(new Vector3f(-400, yy, 0));
            path.addWayPoint(new Vector3f(-400, yy, 0));
            path.addWayPoint(new Vector3f(100, yy, 0));
            path.addWayPoint(new Vector3f(100, yy, 0));
            path.addWayPoint(new Vector3f(150, yy, 0));
            
            path.enableDebugShape(assetManager, rootNode);

            MotionEvent motionControl = new MotionEvent(shootablesGeom.get(i), path);
            motionControl.setDirectionType(MotionEvent.Direction.PathAndRotation);
            motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
            motionControl.setInitialDuration(10f);
            motionControl.setSpeed(1f);   
            
            motionControlList.add(motionControl);

            guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
            wayPointsText = new BitmapText(guiFont, false);
            wayPointsText.setSize(70f);
            guiNode.attachChild(wayPointsText);

            path.addListener(new MotionPathListener() {

                public void onWayPointReach(MotionEvent control, int wayPointIndex) {
                    
                    if (path.getNbWayPoints() == wayPointIndex + 1) {
//                        for (MotionEvent motionEvent : motionControlList) {
//                            motionEvent.stop();
//                        }
                    } else {
//                        wayPointsText.setText(control.getSpatial().getName() + " Reached way point " + wayPointIndex);
                    }
                }
            });
        }
    }
    
    
    
    private void makeAllMembersMove() {
        for (Geometry geom : shootablesGeom) {
            geom.removeControl(terrainPhysicsNode);
        }
        
        for (MotionEvent motionEvent : motionControlList) {
            motionEvent.play();
        }
    }
    
}
