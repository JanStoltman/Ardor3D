/** * Copyright (c) 2008-2014 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.interact;import java.util.concurrent.Callable;import com.ardor3d.example.ExampleBase;import com.ardor3d.example.Purpose;import com.ardor3d.extension.interact.InteractManager;import com.ardor3d.extension.interact.widget.CompoundInteractWidget;import com.ardor3d.extension.interact.widget.InteractMatrix;import com.ardor3d.extension.interact.widget.MovePlanarWidget.MovePlane;import com.ardor3d.extension.terrain.client.Terrain;import com.ardor3d.extension.terrain.client.TerrainBuilder;import com.ardor3d.extension.terrain.client.TerrainDataProvider;import com.ardor3d.extension.terrain.heightmap.MidPointHeightMapGenerator;import com.ardor3d.extension.terrain.providers.array.ArrayTerrainDataProvider;import com.ardor3d.framework.Canvas;import com.ardor3d.framework.CanvasRenderer;import com.ardor3d.image.Texture.MinificationFilter;import com.ardor3d.image.Texture2D;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.input.logical.TwoInputStates;import com.ardor3d.light.DirectionalLight;import com.ardor3d.math.ColorRGBA;import com.ardor3d.math.Vector3;import com.ardor3d.renderer.RenderContext;import com.ardor3d.renderer.Renderer;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.FogState;import com.ardor3d.renderer.state.FogState.DensityFunction;import com.ardor3d.scenegraph.shape.PQTorus;import com.ardor3d.util.GameTaskQueue;import com.ardor3d.util.GameTaskQueueManager;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;/** * Example showing interact widgets with the Geometry Clipmap Terrain system. Requires GLSL support. */@Purpose(htmlDescriptionKey = "com.ardor3d.example.interact.TerrainInteractExample", //        thumbnailPath = "com/ardor3d/example/media/thumbnails/interact_TerrainInteractExample.jpg", //        maxHeapMemory = 128)public class TerrainInteractExample extends ExampleBase {    private final float farPlane = 3000.0f;    private Terrain terrain;    private InteractManager manager;    public static void main(final String[] args) {        ExampleBase.start(TerrainInteractExample.class);    }    @Override    protected void updateExample(final ReadOnlyTimer timer) {        manager.update(timer);    }    @Override    protected void updateLogicalLayer(final ReadOnlyTimer timer) {        manager.getLogicalLayer().checkTriggers(timer.getTimePerFrame());    }    @Override    protected void renderExample(final Renderer renderer) {        super.renderExample(renderer);        manager.render(renderer);    }    /**     * Initialize pssm pass and scene.     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Terrain Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(400, 220, 715));        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(430, 200, 730), Vector3.UNIT_Y);        _canvas.getCanvasRenderer()                .getCamera()                .setFrustumPerspective(                        70.0,                (float) _canvas.getCanvasRenderer().getCamera().getWidth()                                / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);        final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();        final RenderContext renderContext = canvasRenderer.getRenderContext();        final Renderer renderer = canvasRenderer.getRenderer();        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {            @Override            public Void call() throws Exception {                renderer.setBackgroundColor(ColorRGBA.GRAY);                return null;            }        });        _controlHandle.setMoveSpeed(500);        setupDefaultStates();        try {            final int SIZE = 2048;            final MidPointHeightMapGenerator raw = new MidPointHeightMapGenerator(SIZE, 0.6f);            raw.setHeightRange(0.2f);            final float[] heightMap = raw.getHeightData();            final TerrainDataProvider terrainDataProvider = new ArrayTerrainDataProvider(heightMap, SIZE, new Vector3(                    1, 500, 1));            terrain = new TerrainBuilder(terrainDataProvider, _canvas.getCanvasRenderer().getCamera())                    .setShowDebugPanels(false).build();            _root.attachChild(terrain);        } catch (final Exception ex1) {            System.out.println("Problem setting up terrain...");            ex1.printStackTrace();        }        addControls();        // Add something to move around        final PQTorus obj = new PQTorus("obj", 4, 3, 1.5, .5, 128, 8);        obj.setScale(10);        obj.updateModelBound();        _root.attachChild(obj);        _root.updateGeometricState(0);        try {            Thread.sleep(500);        } catch (final InterruptedException e) {        }        obj.setTranslation(630, terrain.getHeightAt(630, 830) + 20, 830);        manager.setSpatialTarget(obj);    }    private void setupDefaultStates() {        _lightState.detachAll();        final DirectionalLight dLight = new DirectionalLight();        dLight.setEnabled(true);        dLight.setAmbient(new ColorRGBA(0.4f, 0.4f, 0.5f, 1));        dLight.setDiffuse(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));        dLight.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.2f, 1));        dLight.setDirection(new Vector3(-1, -1, -1).normalizeLocal());        _lightState.attach(dLight);        _lightState.setEnabled(true);        final CullState cs = new CullState();        cs.setEnabled(true);        cs.setCullFace(CullState.Face.Back);        _root.setRenderState(cs);        final FogState fs = new FogState();        fs.setStart(farPlane / 2.0f);        fs.setEnd(farPlane);        fs.setColor(ColorRGBA.GRAY);        fs.setDensityFunction(DensityFunction.Linear);        _root.setRenderState(fs);    }    private void addControls() {        InteractExample.setupCursors();        // create our manager        manager = new InteractManager();        manager.setupInput(_canvas, _physicalLayer, _logicalLayer);        // final add our widget        final CompoundInteractWidget widget = new CompoundInteractWidget()                .withMoveXAxis(new ColorRGBA(1, 0, 0, .65f), 1.2, .15, .5, .2)                .withMoveZAxis(new ColorRGBA(0, 0, 1, .65f), 1.2, .15, .5, .2) //                .withRotateYAxis() //                .withPlanarHandle(MovePlane.XZ, new ColorRGBA(1, 0, 1, .65f)) //                .withRingTexture((Texture2D) TextureManager.load("images/tick.png", MinificationFilter.Trilinear, true));        // widget.getHandle().setRenderState(_lightState);        manager.addWidget(widget);        manager.setActiveWidget(widget);        // add toggle for matrix mode on widget        manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                widget.setInteractMatrix(widget.getInteractMatrix() == InteractMatrix.World ? InteractMatrix.Local                        : InteractMatrix.World);                widget.targetDataUpdated(manager);            }        }));        // add a filter        manager.addFilter(new TerrainHeightFilter(terrain, 20));    }}