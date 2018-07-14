
/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.terrain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.shadow.map.ParallelSplitShadowMapPass;
import com.ardor3d.extension.shadow.map.ParallelSplitShadowMapPass.Filter;
import com.ardor3d.extension.shadow.map.ShadowCasterManager;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.heightmap.ImageHeightMap;
import com.ardor3d.extension.terrain.providers.array.ArrayTerrainDataProvider;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FogState.DensityFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * Example showing the Geometry Clipmap Terrain system with 'MegaTextures' where the terrain data is provided from a
 * float array populated from a heightmap generated from an Image. Requires GLSL support.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.terrain.ImageMapTerrainExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_ImageMapTerrainExample.jpg", //
        maxHeapMemory = 128)
public class MountainShadowTerrainExample extends ExampleBase {

    private final float farPlane = 8000.0f;

    /** Quads used for debug showing shadowmaps. */
    private Quad _orthoQuad[];

    private Terrain terrain;
    private final Node terrainNode = new Node("terrain");

    private boolean groundCamera = false;
    private Camera terrainCamera;

    /** Text fields used to present info about the example. */
    private final UILabel _exampleInfo[] = new UILabel[2];

    /** Pssm shadow map pass. */
    private ParallelSplitShadowMapPass _pssmPass;

    private DirectionalLight light;

    private double lightTime;
    private boolean moveLight = false;

    private UIHud hud;

    public static void main(final String[] args) {
        ExampleBase._minDepthBits = 24;
        ExampleBase.start(MountainShadowTerrainExample.class);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        // Lazy init since it needs the renderer...
        if (!_pssmPass.isInitialised()) {
            _pssmPass.init(renderer);
            _pssmPass.setPssmShader(terrain.getGeometryClipmapShader());
            for (int i = 0; i < _pssmPass.getNumOfSplits(); i++) {
                terrain.getClipTextureState().setTexture(_pssmPass.getShadowMapTexture(i), i + 1);
            }
            for (int i = 0; i < ParallelSplitShadowMapPass._MAX_SPLITS; i++) {
                terrain.getGeometryClipmapShader().setUniform("shadowMap" + i, i + 1);
            }
        }

        terrain.getGeometryClipmapShader().setUniform("lightDir", light.getDirection());

        for (int i = 0; i < _pssmPass.getNumOfSplits(); i++) {
            TextureState screen = (TextureState) _orthoQuad[i].getLocalRenderState(StateType.Texture);
            Texture copy;
            if (screen == null) {
                screen = new TextureState();
                _orthoQuad[i].setRenderState(screen);
                copy = new Texture2D();
                screen.setTexture(copy);
                _orthoQuad[i].updateGeometricState(0.0);
            } else {
                copy = screen.getTexture();
            }
            copy.setTextureKey(_pssmPass.getShadowMapTexture(i).getTextureKey());
        }

        // XXX: Use a rougher LOD for shadows - tweak?
        terrain.setMinVisibleLevel(4);

        // Update shadowmaps - this will update our terrain camera to light pos
        _pssmPass.updateShadowMaps(renderer);

        // XXX: reset LOD for drawing from view camera
        terrain.setMinVisibleLevel(0);

        // Render scene and terrain with shadows
        terrainNode.onDraw(renderer);
        _root.onDraw(renderer);

        // Render overlay shadows for all objects except the terrain
        renderer.renderBuckets();
        _pssmPass.renderShadowedScene(renderer);
        renderer.renderBuckets();

        // draw ui
        renderer.draw(hud);
    }

    private double counter = 0;
    private int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = frames / counter;
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }

        final Camera camera = _canvas.getCanvasRenderer().getCamera();

        // Make sure camera is above terrain
        final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ());
        if (height > -Float.MAX_VALUE && (groundCamera || camera.getLocation().getY() < height + 3)) {
            camera.setLocation(new Vector3(camera.getLocation().getX(), height + 3, camera.getLocation().getZ()));
            terrainCamera.set(camera);
        } else {
            terrainCamera.set(_canvas.getCanvasRenderer().getCamera());
        }

        // move terrain to view pos
        terrainNode.updateGeometricState(timer.getTimePerFrame());
        hud.updateGeometricState(timer.getTimePerFrame());

        if (moveLight) {
            lightTime += timer.getTimePerFrame();
            light.setDirection(new Vector3(Math.sin(lightTime), -.8, Math.cos(lightTime)).normalizeLocal());
        }
    }

    /**
     * Initialize pssm pass and scene.
     */
    @Override
    protected void initExample() {
        // Setup main camera.
        _canvas.setTitle("Terrain Example");
        final Camera canvasCamera = _canvas.getCanvasRenderer().getCamera();
        canvasCamera.setLocation(new Vector3(2176, 790, 688));
        canvasCamera.lookAt(new Vector3(canvasCamera.getLocation()).addLocal(-0.87105768019686, -0.4349655341112313,
                0.22817427967541867), Vector3.UNIT_Y);
        canvasCamera.setFrustumPerspective(45.0, (float) _canvas.getCanvasRenderer().getCamera().getWidth()
                / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);
        final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
        final RenderContext renderContext = canvasRenderer.getRenderContext();
        final Renderer renderer = canvasRenderer.getRenderer();
        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                renderer.setBackgroundColor(ColorRGBA.BLUE);
                return null;
            }
        });
        _controlHandle.setMoveSpeed(400);

        setupDefaultStates();
        addRover();
        addUI();

        // Initialize PSSM shadows
        _pssmPass = new ParallelSplitShadowMapPass(light, 2048, 4);
        _pssmPass.setFiltering(Filter.None);
        _pssmPass.setRenderShadowedScene(false);
        _pssmPass.setKeepMainShader(true);
        // _pssmPass.setMinimumLightDistance(500); // XXX: Tune this
        _pssmPass.setUseSceneTexturing(false);
        _pssmPass.setUseObjectCullFace(false);
        _pssmPass.getShadowOffsetState().setFactor(1.1f);
        _pssmPass.getShadowOffsetState().setUnits(4.0f);
        // _pssmPass.setDrawDebug(true);

        // TODO: backside lock test
        final Quad floor = new Quad("floor", 2048, 2048);
        floor.updateModelBound();
        floor.setRotation(new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
        floor.setTranslation(1024, 0, 1024);
        terrainNode.attachChild(floor);

        _pssmPass.addBoundsReceiver(terrainNode);

        // Add objects that will get shadowed through overlay render
        _pssmPass.add(_root);

        // Add our occluders that will produce shadows
        ShadowCasterManager.INSTANCE.addSpatial(terrainNode);
        ShadowCasterManager.INSTANCE.addSpatial(_root);

        final int quadSize = _canvas.getCanvasRenderer().getCamera().getWidth() / 10;
        _orthoQuad = new Quad[ParallelSplitShadowMapPass._MAX_SPLITS];
        for (int i = 0; i < ParallelSplitShadowMapPass._MAX_SPLITS; i++) {
            _orthoQuad[i] = new Quad("OrthoQuad", quadSize, quadSize);
            _orthoQuad[i].setTranslation(new Vector3(quadSize / 2 + 5 + (quadSize + 5) * i, quadSize / 2 + 5, 1));
            _orthoQuad[i].setScale(1, -1, 1);
            _orthoQuad[i].getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
            _orthoQuad[i].getSceneHints().setLightCombineMode(LightCombineMode.Off);
            _orthoQuad[i].getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
            _orthoQuad[i].getSceneHints().setCullHint(CullHint.Never);
            hud.attachChild(_orthoQuad[i]);
        }

        try {
            // Keep a separate camera to be able to freeze terrain update
            final Camera camera = _canvas.getCanvasRenderer().getCamera();
            terrainCamera = new Camera(camera);

            // IMAGE LOADING AND CONVERSION TO HEIGHTMAP DONE HERE
            final BufferedImage heightmap = ImageIO.read(ResourceLocatorTool.getClassPathResource(
                    MountainShadowTerrainExample.class, "com/ardor3d/example/media/images/heightmap.jpg"));
            final Image ardorImage = AWTImageLoader.makeArdor3dImage(heightmap, false);
            final float[] heightMap = ImageHeightMap.generateHeightMap(ardorImage, 0.05f, .33f);
            // END OF IMAGE CONVERSION

            final int SIZE = ardorImage.getWidth();

            final ArrayTerrainDataProvider terrainDataProvider = new ArrayTerrainDataProvider(heightMap, SIZE,
                    new Vector3(5, 2048, 5), true);
            terrainDataProvider.setHeightMax(0.34f);

            final TerrainBuilder builder = new TerrainBuilder(terrainDataProvider, terrainCamera)
                    .setShowDebugPanels(true);

            terrain = builder.build();
            terrain.setPixelShader(ResourceLocatorTool.getClassPathResource(ShadowedTerrainExample.class,
                    "com/ardor3d/extension/terrain/shadowedGeometryClipmapShader_normalMap.frag"));
            terrain.reloadShader();
            terrain.getGeometryClipmapShader().setUniform("normalMap", 5);
            terrainNode.attachChild(terrain);

            terrain.setCullingEnabled(false);
        } catch (final Exception ex1) {
            System.out.println("Problem setting up terrain...");
            ex1.printStackTrace();
        }

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = new UILabel("Text");
            _exampleInfo[i].setForegroundColor(ColorRGBA.WHITE, true);
            _exampleInfo[i].addFontStyle(StyleConstants.KEY_SIZE, 16);
            _exampleInfo[i].addFontStyle(StyleConstants.KEY_BOLD, Boolean.TRUE);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
            hud.add(_exampleInfo[i]);
        }

        updateText();

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(5);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(50);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(400);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(1000);
                updateText();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                groundCamera = !groundCamera;
                updateText();
            }
        }));
    }

    private void addRover() {
        try {
            final ColladaStorage storage = new ColladaImporter().load("collada/sketchup/NASA Mars Rover.dae");
            final Node rover = storage.getScene();
            rover.setTranslation(440, 102, 160.1);
            rover.setScale(3);
            rover.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
            _root.attachChild(rover);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

    }

    private void setupDefaultStates() {
        terrainNode.setRenderState(_lightState);
        terrainNode.setRenderState(_wireframeState);
        terrainNode.setRenderState(new ZBufferState());

        _lightState.detachAll();
        light = new DirectionalLight();
        light.setEnabled(true);
        light.setAmbient(new ColorRGBA(0.4f, 0.4f, 0.5f, 1));
        light.setDiffuse(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));
        light.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.2f, 1));
        light.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        _lightState.attach(light);
        _lightState.setEnabled(true);

        final FogState fs = new FogState();
        fs.setStart(farPlane / 2.0f);
        fs.setEnd(farPlane);
        fs.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        fs.setDensityFunction(DensityFunction.Linear);
        terrainNode.setRenderState(fs);
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[1/2/3/4] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + " km/h");
        _exampleInfo[1].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    @Override
    protected void renderDebug(final Renderer renderer) {
        super.renderDebug(renderer);
        if (_showBounds) {
            Debugger.drawBounds(terrainNode, renderer, true);
        }
    }

    private void addUI() {
        // setup hud
        hud = new UIHud(_canvas);
        hud.setupInput(_physicalLayer, _logicalLayer);
        hud.setMouseManager(_mouseManager);

        final UIFrame frame = new UIFrame("Controls", EnumSet.noneOf(FrameButtons.class));
        frame.setResizeable(false);

        final UILabel distLabel = new UILabel("Max Shadow Distance: 1500");
        final UISlider distSlider = new UISlider(Orientation.Horizontal, 0, 2000, 1500);
        distSlider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                _pssmPass.setMaxShadowDistance(distSlider.getValue());
                distLabel.setText("Max Shadow Distance: " + distSlider.getValue());
            }
        });

        final UIButton updateCamera = new UIButton("Update Shadow Camera");
        updateCamera.setSelectable(true);
        updateCamera.setSelected(true);
        updateCamera.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                _pssmPass.setUpdateMainCamera(updateCamera.isSelected());
                updateText();
            }
        });

        final UIButton rotateLight = new UIButton("Rotate Light");
        rotateLight.setSelectable(true);
        rotateLight.setSelected(false);
        rotateLight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                moveLight = rotateLight.isSelected();
                updateText();
            }
        });

        final UIPanel panel = new UIPanel(new RowLayout(false, true, false));
        panel.setPadding(new Insets(10, 20, 10, 20));
        panel.add(distLabel);
        panel.add(distSlider);
        panel.add(updateCamera);
        panel.add(rotateLight);

        frame.setContentPanel(panel);
        frame.pack();
        frame.setLocalXY(hud.getWidth() - frame.getLocalComponentWidth(),
                hud.getHeight() - frame.getLocalComponentHeight());
        hud.add(frame);
    }
}
