/** * Copyright (c) 2008-2018 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.effect;import com.ardor3d.bounding.BoundingBox;import com.ardor3d.example.ExampleBase;import com.ardor3d.example.Purpose;import com.ardor3d.extension.shadow.map.ParallelSplitShadowMapPass;import com.ardor3d.extension.shadow.map.ShadowCasterManager;import com.ardor3d.framework.Canvas;import com.ardor3d.image.Texture;import com.ardor3d.image.Texture2D;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.input.logical.TwoInputStates;import com.ardor3d.light.DirectionalLight;import com.ardor3d.light.Light;import com.ardor3d.light.PointLight;import com.ardor3d.math.Matrix3;import com.ardor3d.math.Vector3;import com.ardor3d.renderer.Renderer;import com.ardor3d.renderer.pass.BasicPassManager;import com.ardor3d.renderer.pass.RenderPass;import com.ardor3d.renderer.queue.RenderBucketType;import com.ardor3d.renderer.state.BlendState;import com.ardor3d.renderer.state.BlendState.TestFunction;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.MaterialState;import com.ardor3d.renderer.state.MaterialState.ColorMaterial;import com.ardor3d.renderer.state.TextureState;import com.ardor3d.scenegraph.Node;import com.ardor3d.scenegraph.Spatial;import com.ardor3d.scenegraph.controller.SpatialController;import com.ardor3d.scenegraph.hint.CullHint;import com.ardor3d.scenegraph.hint.LightCombineMode;import com.ardor3d.scenegraph.hint.TextureCombineMode;import com.ardor3d.scenegraph.shape.Box;import com.ardor3d.scenegraph.shape.Quad;import com.ardor3d.scenegraph.shape.Torus;import com.ardor3d.scenegraph.visitor.UpdateModelBoundVisitor;import com.ardor3d.ui.text.BasicText;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;/** * Example showing the parallel split shadow mapping technique. Requires GLSL support. */@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.ParallelSplitShadowMapExample", //        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_ParallelSplitShadowMapExample.jpg", //        maxHeapMemory = 64)public class ParallelSplitShadowMapExample extends ExampleBase {    /** Pssm shadow map pass. */    private ParallelSplitShadowMapPass _pssmPass;    /** Pass manager. */    private BasicPassManager _passManager;    /** Quads used for debug showing shadowmaps. */    private Quad _orthoQuad[];    /** Flag for turning on/off light movement. */    private boolean _updateLight = false;    /** Temp vec for updating light pos. */    private final Vector3 lightPosition = new Vector3(10000, 5000, 10000);    /** Text fields used to present info about the example. */    private final BasicText _exampleInfo[] = new BasicText[12];    /** Flag to make sure quads are updated on reinitialization of shadow renderer */    private boolean _quadsDirty = true;    /** Console fps output */    private double counter = 0;    private int frames = 0;    /**     * The main method.     *     * @param args     *            the arguments     */    public static void main(final String[] args) {        start(ParallelSplitShadowMapExample.class);    }    /**     * Update the PassManager and light.     *     * @param timer     *            the application timer     */    @Override    protected void updateExample(final ReadOnlyTimer timer) {        _passManager.updatePasses(timer.getTimePerFrame());        if (_updateLight) {            final double time = timer.getTimeInSeconds() * 0.2;            lightPosition.set(Math.sin(time) * 10000.0, 5000.0, Math.cos(time) * 10000.0);        }        counter += timer.getTimePerFrame();        frames++;        if (counter > 1) {            final double fps = (frames / counter);            counter = 0;            frames = 0;            System.out.printf("%7.1f FPS\n", fps);        }    }    /**     * Initialize pssm if needed. Update light position. Render scene.     *     * @param renderer     *            the renderer     */    @Override    protected void renderExample(final Renderer renderer) {        if (!_pssmPass.isInitialised()) {            _pssmPass.init(renderer);        }        updateQuadTextures(renderer);        // Update the shadowpass "light" position. Iow it's camera.        final Light light = _lightState.get(0);        if (light instanceof PointLight) {            ((PointLight) light).setLocation(lightPosition);        } else if (light instanceof DirectionalLight) {            ((DirectionalLight) light).setDirection(lightPosition.normalize(null).negateLocal());        }        _passManager.renderPasses(renderer);    }    /**     * Initialize pssm pass and scene.     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Parallel Split Shadow Maps - Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(250, 200, -250));        _canvas.getCanvasRenderer()                .getCamera()                .setFrustumPerspective(                        45.0,                (float) _canvas.getCanvasRenderer().getCamera().getWidth()                                / (float) _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0, 10000);        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);        _controlHandle.setMoveSpeed(200);        // Setup some standard states for the scene.        final CullState cullFrontFace = new CullState();        cullFrontFace.setEnabled(true);        cullFrontFace.setCullFace(CullState.Face.Back);        _root.setRenderState(cullFrontFace);        final TextureState ts = new TextureState();        ts.setEnabled(true);        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));        _root.setRenderState(ts);        final MaterialState ms = new MaterialState();        ms.setColorMaterial(ColorMaterial.Diffuse);        _root.setRenderState(ms);        _passManager = new BasicPassManager();        // setup some quads for debug viewing.        final RenderPass renderPass = new RenderPass();        final int quadSize = _canvas.getCanvasRenderer().getCamera().getWidth() / 10;        _orthoQuad = new Quad[ParallelSplitShadowMapPass._MAX_SPLITS];        for (int i = 0; i < ParallelSplitShadowMapPass._MAX_SPLITS; i++) {            _orthoQuad[i] = new Quad("OrthoQuad", quadSize, quadSize);            _orthoQuad[i].setTranslation(new Vector3((quadSize / 2 + 5) + (quadSize + 5) * i, (quadSize / 2 + 5), 1));            _orthoQuad[i].getSceneHints().setRenderBucketType(RenderBucketType.Ortho);            _orthoQuad[i].getSceneHints().setLightCombineMode(LightCombineMode.Off);            _orthoQuad[i].getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);            _orthoQuad[i].getSceneHints().setCullHint(CullHint.Never);            renderPass.add(_orthoQuad[i]);        }        // Create scene objects.        setupTerrain();        final RenderPass rootPass = new RenderPass();        rootPass.add(_root);        _lightState.detachAll();        final DirectionalLight light = new DirectionalLight();        // final PointLight light = new PointLight();        light.setEnabled(true);        _lightState.attach(light);        // Create pssm pass        _pssmPass = new ParallelSplitShadowMapPass(light, 1024, 3);        _pssmPass.add(_root);        _pssmPass.setUseSceneTexturing(true);        _pssmPass.setUseObjectCullFace(true);        final Node occluders = setupOccluders();        ShadowCasterManager.INSTANCE.addSpatial(occluders);        // Populate passmanager with passes.        _passManager.add(rootPass);        _passManager.add(_pssmPass);        _passManager.add(renderPass);        // Setup textfields for presenting example info.        final Node textNodes = new Node("Text");        renderPass.add(textNodes);        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight();        for (int i = 0; i < _exampleInfo.length; i++) {            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - (i + 1) * 20, 0));            textNodes.attachChild(_exampleInfo[i]);        }        textNodes.updateGeometricState(0.0);        updateText();        // Register keyboard triggers for manipulating example        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setDrawShaderDebug(!_pssmPass.isDrawShaderDebug());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _updateLight = !_updateLight;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setUpdateMainCamera(!_pssmPass.isUpdateMainCamera());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setDrawDebug(!_pssmPass.isDrawDebug());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (_pssmPass.getNumOfSplits() > ParallelSplitShadowMapPass._MIN_SPLITS) {                    _pssmPass.setNumOfSplits(_pssmPass.getNumOfSplits() - 1);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (_pssmPass.getNumOfSplits() < ParallelSplitShadowMapPass._MAX_SPLITS) {                    _pssmPass.setNumOfSplits(_pssmPass.getNumOfSplits() + 1);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (_pssmPass.getShadowMapSize() > 1) {                    _pssmPass.setShadowMapSize(_pssmPass.getShadowMapSize() / 2);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SEVEN), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                if (_pssmPass.getShadowMapSize() < 2048) {                    _pssmPass.setShadowMapSize(_pssmPass.getShadowMapSize() * 2);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.EIGHT), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                final double maxShadowDistance = _pssmPass.getMaxShadowDistance();                if (maxShadowDistance > 200.0) {                    _pssmPass.setMaxShadowDistance(maxShadowDistance - 100.0);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                final double maxShadowDistance = _pssmPass.getMaxShadowDistance();                _pssmPass.setMaxShadowDistance(maxShadowDistance + 100.0);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setNumOfSplits(1);                _pssmPass.setShadowMapSize(1024);                updateText();                _quadsDirty = true;            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setNumOfSplits(3);                _pssmPass.setShadowMapSize(512);                updateText();                _quadsDirty = true;            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setUseSceneTexturing(!_pssmPass.isUseSceneTexturing());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setUseObjectCullFace(!_pssmPass.isUseObjectCullFace());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {            @Override            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {                _pssmPass.setEnabled(!_pssmPass.isEnabled());                updateText();                _quadsDirty = true;            }        }));        // Make sure all boundings are updated.        _root.acceptVisitor(new UpdateModelBoundVisitor(), false);    }    /**     * Setup debug quads to render pssm shadowmaps.     */    private void updateQuadTextures(final Renderer r) {        if (!_quadsDirty) {            return;        }        _quadsDirty = false;        _pssmPass.reinit(r);        for (int i = 0; i < _pssmPass.getNumOfSplits(); i++) {            final TextureState screen = new TextureState();            final Texture2D copy = new Texture2D();            copy.setTextureKey(_pssmPass.getShadowMapTexture(i).getTextureKey());            screen.setTexture(copy);            _orthoQuad[i].setRenderState(screen);            _orthoQuad[i].getSceneHints().setCullHint(CullHint.Never);            _orthoQuad[i].updateGeometricState(0.0);        }        for (int i = _pssmPass.getNumOfSplits(); i < ParallelSplitShadowMapPass._MAX_SPLITS; i++) {            _orthoQuad[i].getSceneHints().setCullHint(CullHint.Always);        }    }    /**     * Update text information.     */    private void updateText() {        _exampleInfo[0].setText("[0] Debug shader draw: " + _pssmPass.isDrawShaderDebug());        _exampleInfo[1].setText("[1] Update light: " + _updateLight);        _exampleInfo[2].setText("[2] Update main camera: " + _pssmPass.isUpdateMainCamera());        _exampleInfo[3].setText("[3] Debug draw: " + _pssmPass.isDrawDebug());        _exampleInfo[4].setText("[4/5] Number of splits: " + _pssmPass.getNumOfSplits());        _exampleInfo[5].setText("[6/7] Shadow map size: " + _pssmPass.getShadowMapSize());        _exampleInfo[6].setText("[8/9] Max shadow distance: " + _pssmPass.getMaxShadowDistance());        _exampleInfo[7].setText("[U] Setup 1 split of size 1024");        _exampleInfo[8].setText("[I] Setup 3 splits of size 512");        _exampleInfo[9].setText("[J] Use scene texturing: " + _pssmPass.isUseSceneTexturing());        _exampleInfo[10].setText("[K] Use object cull face: " + _pssmPass.isUseObjectCullFace());        _exampleInfo[11].setText("[SPACE] toggle PSSM pass: " + (_pssmPass.isEnabled() ? "enabled" : "disabled"));    }    /**     * Setup terrain.     */    private void setupTerrain() {        final Box box = new Box("box", new Vector3(), 10000, 10, 10000);        box.setModelBound(new BoundingBox());        box.addController(new SpatialController<Box>() {            double timer = 0;            @Override            public void update(final double time, final Box caller) {                timer += time;                caller.setTranslation(Math.sin(timer) * 20.0, 0, Math.cos(timer) * 20.0);            }        });        _root.attachChild(box);    }    /**     * Setup occluders.     */    private Node setupOccluders() {        final Node occluders = new Node("occs");        _root.attachChild(occluders);        for (int i = 0; i < 30; i++) {            final double w = Math.random() * 40 + 10;            final double y = Math.random() * 20 + 10;            final Box b = new Box("box", new Vector3(), w, y, w);            b.setModelBound(new BoundingBox());            final double x = Math.random() * 1000 - 500;            final double z = Math.random() * 1000 - 500;            b.setTranslation(new Vector3(x, y, z));            occluders.attachChild(b);        }        final Torus torusWithoutShadows = new Torus("torus", 32, 10, 15.0f, 20.0f);        torusWithoutShadows.setModelBound(new BoundingBox());        torusWithoutShadows.getSceneHints().setCastsShadows(false);        torusWithoutShadows.setTranslation(0, 50, -100);        occluders.attachChild(torusWithoutShadows);        final Torus torus = new Torus("torus", 64, 12, 10.0f, 15.0f);        torus.setModelBound(new BoundingBox());        occluders.attachChild(torus);        torus.addController(new SpatialController<Torus>() {            double timer = 0;            Matrix3 rotation = new Matrix3();            @Override            public void update(final double time, final Torus caller) {                timer += time;                caller.setTranslation(Math.sin(timer) * 40.0, Math.sin(timer) * 50.0 + 20.0, Math.cos(timer) * 40.0);                rotation.fromAngles(timer * 0.4, timer * 0.4, timer * 0.4);                caller.setRotation(rotation);            }        });        // Attach "billboard" with an alpha test.        occluders.attachChild(makeBillBoard());        return occluders;    }    private Spatial makeBillBoard() {        final Node billboard = new Node("bb");        billboard.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);        final Quad q1 = new Quad("font block", 150, 200);        q1.setTranslation(0, 80, 0);        q1.setModelBound(new BoundingBox());        final CullState cs = new CullState();        cs.setCullFace(CullState.Face.None);        q1.setRenderState(cs);        billboard.attachChild(q1);        final TextureState ts = new TextureState();        ts.setEnabled(true);        ts.setTexture(TextureManager.load("fonts/OkasaSansSerif-35-medium-regular_00.png",                Texture.MinificationFilter.Trilinear, true));        billboard.setRenderState(ts);        final BlendState bs = new BlendState();        bs.setEnabled(true);        bs.setBlendEnabled(false);        bs.setTestEnabled(true);        bs.setTestFunction(TestFunction.GreaterThan);        bs.setReference(0.7f);        billboard.setRenderState(bs);        return billboard;    }}