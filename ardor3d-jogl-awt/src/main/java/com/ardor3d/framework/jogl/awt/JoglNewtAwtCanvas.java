/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl.awt;

import java.util.concurrent.CountDownLatch;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLRunnable;
import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.jogl.CapsUtil;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.JoglDrawerRunnable;
import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;

public class JoglNewtAwtCanvas extends NewtCanvasAWT implements Canvas, NewtWindowContainer {

    private static final long serialVersionUID = 1L;

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    public JoglNewtAwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer) {
        this(settings, canvasRenderer, new CapsUtil());
    }

    public JoglNewtAwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final CapsUtil capsUtil) {
        this(createNewtChildWindow(settings, capsUtil), settings, canvasRenderer, capsUtil);
    }
    
    public JoglNewtAwtCanvas(final GLWindow newtChildWindow, final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final CapsUtil capsUtil) {
    	super(newtChildWindow);
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        getNewtWindow().setUndecorated(true);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        setFocusable(true);
        setSize(_settings.getWidth(), _settings.getHeight());
        setIgnoreRepaint(true);
        getNewtWindow().setAutoSwapBufferMode(false);
    }
    
    private static GLWindow createNewtChildWindow(final DisplaySettings settings, final CapsUtil capsUtil) {
    	final GLWindow newtChildWindow = GLWindow.create(capsUtil.getCapsForSettings(settings));
    	// disables HiDPI, see https://github.com/gouessej/Ardor3D/issues/14
    	newtChildWindow.setSurfaceScale(new float[] { ScalableSurface.IDENTITY_PIXELSCALE,
                ScalableSurface.IDENTITY_PIXELSCALE });
    	return newtChildWindow;
    }

    @Override
    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Make the window visible to realize the OpenGL surface.
        setVisible(true);
        if (getNewtWindow().isRealized()) {
            // Request the focus here as it cannot work when the window is not visible
            requestFocus();
            /**
             * I do not understand why I cannot get the context earlier, I failed in getting it from addNotify() and
             * setVisible(true)
             * */
            _canvasRenderer.setContext(getNewtWindow().getContext());
            getNewtWindow().invoke(true, new GLRunnable() {
                @Override
                public boolean run(final GLAutoDrawable glAutoDrawable) {
                    _canvasRenderer.init(_settings, true);// true - do swap in renderer.
                    return true;
                }
            });
            _inited = true;
        }
    }

    @Override
    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (isShowing()) {
            getNewtWindow().invoke(true, _drawerGLRunnable);
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    @Override
    public GLWindow getNewtWindow() {
        return (GLWindow) getNEWTChild();
    }

    public void setVSyncEnabled(final boolean enabled) {
        getNewtWindow().invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable glAutoDrawable) {
                glAutoDrawable.getGL().setSwapInterval(enabled ? 1 : 0);
                return false;
            }
        });
    }
}
