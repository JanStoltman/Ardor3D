/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.jogl.CapsUtil;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.renderer.AbstractPbufferTextureRenderer;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.jogl.DirectNioBuffersSet;

/**
 * <p>
 * This class is used by Ardor3D's JOGL implementation to render textures. Users should <b>not </b> create this class
 * directly.
 * </p>
 * N.B: This class can't work without a complete implementation of GLOffscreenAutoDrawable.PBuffer, which is currently
 * missing from JOGL
 * 
 * @see TextureRendererFactory
 */
public class JoglPbufferTextureRenderer extends AbstractPbufferTextureRenderer {
    private static final Logger logger = Logger.getLogger(JoglPbufferTextureRenderer.class.getName());

    private GLOffscreenAutoDrawable _offscreenDrawable;

    private GLContext _context;

    protected CapsUtil _capsUtil;

    // HACK: needed to get the parent context in here somehow...
    public static GLContext _parentContext;

    protected DirectNioBuffersSet _directNioBuffersSet;

    public JoglPbufferTextureRenderer(final DisplaySettings settings, final Renderer parentRenderer,
            final ContextCapabilities caps) {
        super(settings, parentRenderer, caps);
        _capsUtil = new CapsUtil();
        setMultipleTargets(false);
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid gl
     * texture id for this texture and inits the data type for the texture.
     */
    @Override
    public void setupTexture(final Texture tex) {
        if (tex.getType() != Type.TwoDimensional) {
            throw new IllegalArgumentException("Unsupported type: " + tex.getType());
        }
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);

        // check if we are already setup... if so, throw error.
        if (tex.getTextureKey() == null) {
            tex.setTextureKey(TextureKey.getRTTKey(tex.getMinificationFilter()));
        } else if (tex.getTextureIdForContext(context.getGlContextRep()) != 0) {
            throw new Ardor3dException("Texture is already setup and has id.");
        }

        // Create the texture
        final IntBuffer ibuf = _directNioBuffersSet.getSingleIntBuffer();
        gl.glGenTextures(1, ibuf);
        final int textureId = ibuf.get(0);
        tex.setTextureIdForContext(context.getGlContextRep(), textureId);

        JoglTextureStateUtil.doTextureBind(tex, 0, true);

        // Initialize our texture with some default data.
        final int internalFormat = JoglTextureUtil.getGLInternalFormat(tex.getTextureStoreFormat());
        final int dataFormat = JoglTextureUtil.getGLPixelFormatFromStoreFormat(tex.getTextureStoreFormat());
        final int pixelDataType = JoglTextureUtil.getGLPixelDataType(tex.getRenderedTexturePixelDataType());

        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, _width, _height, 0, dataFormat, pixelDataType, null);

        // Setup filtering and wrap
        final TextureRecord texRecord = record.getTextureRecord(textureId, tex.getType());
        JoglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        JoglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup pbuffer tex" + textureId + ": " + _width + "," + _height);
    }

    @Override
    public void render(final Spatial spat, final Texture tex, final int clear) {
        render(null, spat, null, tex, clear);
    }

    @Override
    public void render(final List<? extends Spatial> spat, final Texture tex, final int clear) {
        render(spat, null, null, tex, clear);
    }

    @Override
    public void render(final Scene scene, final Texture tex, final int clear) {
        render(null, null, scene, tex, clear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Scene toDrawC,
            final Texture tex, final int clear) {
        try {
            if (_offscreenDrawable == null) {
                initPbuffer();
            }

            if (_useDirectRender && !tex.getTextureStoreFormat().isDepthFormat()) {
                // setup and render directly to a 2d texture.
                releaseTexture();
                activate();
                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                deactivate();
                switchCameraOut();
                JoglTextureStateUtil.doTextureBind(tex, 0, true);
                bindTexture();
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else if (toDrawB != null) {
                    doDraw(toDrawB);
                } else {
                    doDraw(toDrawC);
                }

                switchCameraOut();

                copyToTexture(tex, 0, 0, _width, _height, 0, 0);

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    private void bindTexture() {
        // FIXME The class PBuffer is going to be removed from JOGL very soon and GLOffscreenAutoDrawable.PBuffer is not
        // yet ready
    }

    private void releaseTexture() {
        // FIXME
    }

    @Override
    public void render(final Spatial spat, final List<Texture> texs, final int clear) {
        render(null, spat, null, texs, clear);
    }

    @Override
    public void render(final List<? extends Spatial> spat, final List<Texture> texs, final int clear) {
        render(spat, null, null, texs, clear);
    }

    @Override
    public void render(final Scene scene, final List<Texture> texs, final int clear) {
        render(null, null, scene, texs, clear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Scene toDrawC,
            final List<Texture> texs, final int clear) {
        try {
            if (_offscreenDrawable == null) {
                initPbuffer();
            }

            if (texs.size() == 1 && _useDirectRender && !texs.get(0).getTextureStoreFormat().isDepthFormat()) {
                // setup and render directly to a 2d texture.
                JoglTextureStateUtil.doTextureBind(texs.get(0), 0, true);
                activate();
                switchCameraIn(clear);
                releaseTexture();

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else if (toDrawB != null) {
                    doDraw(toDrawB);
                } else {
                    doDraw(toDrawC);
                }

                switchCameraOut();

                deactivate();
                bindTexture();
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                for (int i = 0; i < texs.size(); i++) {
                    copyToTexture(texs.get(i), 0, 0, _width, _height, 0, 0);
                }

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    @Override
    public void copyToTexture(final Texture tex, final int x, final int y, final int width, final int height,
            final int xoffset, final int yoffset) {
        final GL gl = GLContext.getCurrentGL();

        JoglTextureStateUtil.doTextureBind(tex, 0, true);

        gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, xoffset, yoffset, x, y, width, height);
    }

    @Override
    protected void clearBuffers(final int clear) {
        final GL gl = GLContext.getCurrentGL();

        gl.glDisable(GL.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers(clear);
    }

    private void initPbuffer() {

        try {
            if (_offscreenDrawable != null) {
                _context.destroy();
                _offscreenDrawable.destroy();
                giveBackContext();
                ContextManager.removeContext(_offscreenDrawable.getContext());
            }

            // Make our GLPbuffer...
            final GLProfile profile = _capsUtil.getProfile();
            final GLDrawableFactory fac = GLDrawableFactory.getFactory(profile);
            final GLCapabilities caps = new GLCapabilities(profile);
            caps.setHardwareAccelerated(true);
            caps.setDoubleBuffered(true);
            caps.setAlphaBits(_settings.getAlphaBits());
            caps.setDepthBits(_settings.getDepthBits());
            caps.setNumSamples(_settings.getSamples());
            caps.setSampleBuffers(_settings.getSamples() != 0);
            caps.setStencilBits(_settings.getStencilBits());
            caps.setDoubleBuffered(false);
            caps.setOnscreen(false);
            caps.setPBuffer(true);
            _offscreenDrawable = fac.createOffscreenAutoDrawable(null, caps, null, _width, _height);
            _offscreenDrawable.setSharedContext(_parentContext);
            _context = _offscreenDrawable.getContext();

            _context.makeCurrent();

            if (_directNioBuffersSet == null) {
                _directNioBuffersSet = new DirectNioBuffersSet();
            }

            final JoglContextCapabilities contextCaps = new JoglContextCapabilities(_offscreenDrawable.getGL(),
                    _directNioBuffersSet);
            ContextManager.addContext(_context,
                    new JoglRenderContext(_context, contextCaps, ContextManager.getCurrentContext(),
                            _directNioBuffersSet));

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "initPbuffer()", "Exception", e);

            if (_useDirectRender) {
                logger.warning("Your card claims to support Render to Texture but fails to enact it.  Updating your driver might solve this problem.");
                logger.warning("Attempting to fall back to Copy Texture.");
                _useDirectRender = false;
                initPbuffer();
                return;
            }

            logger.log(Level.WARNING, "Failed to create Pbuffer.", e);
            return;
        }

        try {
            activate();

            _width = _offscreenDrawable.getSurfaceWidth();
            _height = _offscreenDrawable.getSurfaceHeight();

            deactivate();
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Failed to initialize created Pbuffer.", e);
            return;
        }
    }

    private void activate() {
        if (_active == 0) {
            _oldContext = ContextManager.getCurrentContext();
            _context.makeCurrent();

            ContextManager.switchContext(_context);

            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);

            if (_bgColorDirty) {
                final GL gl = GLContext.getCurrentGL();

                gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                        _backgroundColor.getAlpha());
                _bgColorDirty = false;
            }
        }
        _active++;
    }

    private void deactivate() {
        if (_active == 1) {
            giveBackContext();
        }
        _active--;
    }

    private void giveBackContext() {
        _parentContext.makeCurrent();
        ContextManager.switchContext(_oldContext.getContextKey());
    }

    @Override
    public void cleanup() {
        ContextManager.removeContext(_offscreenDrawable.getContext());
        _offscreenDrawable.destroy();
    }

    @Override
    public void setMultipleTargets(final boolean force) {
        if (force) {
            logger.fine("Copy Texture Pbuffer used!");
            _useDirectRender = false;
            if (_offscreenDrawable != null) {
                giveBackContext();
                ContextManager.removeContext(_offscreenDrawable.getContext());
            }
        } else {
            // XXX: Is this WGL specific query right?
            if (GLContext.getCurrentGL().isExtensionAvailable("WGL_ARB_render_texture")) {
                logger.fine("Render to Texture Pbuffer supported!");
                _useDirectRender = true;
            } else {
                logger.fine("Copy Texture Pbuffer supported!");
            }
        }
    }
}
