/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLRunnable;

/**
 * Ardor3D NEWT lightweight window, NEWT "top level" component for the OpenGL rendering of Ardor3D with JOGL that
 * supports the NEWT input system directly and its abstraction in Ardor3D (com.ardor3d.input.jogl). This is the fastest
 * and the most cross-platform, reliable, memory-efficient and complete "surface" of the JogAmp backend
 */
public class JoglNewtWindow implements NativeCanvas, NewtWindowContainer {

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;
    private boolean _isClosing = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    private final GLWindow _newtWindow;

    /** list of monitor devices used in fullscreen mode, ignored in windowed mode */
    private List<MonitorDevice> _monitorDevices;

    public JoglNewtWindow(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings) {
        this(canvasRenderer, settings, true, false, false, false);
    }

    public JoglNewtWindow(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings,
            final boolean onscreen, final boolean bitmapRequested, final boolean pbufferRequested,
            final boolean fboRequested) {
        this(canvasRenderer, settings, onscreen, bitmapRequested, pbufferRequested, fboRequested, new CapsUtil());
    }

    public JoglNewtWindow(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings,
            final boolean onscreen, final boolean bitmapRequested, final boolean pbufferRequested,
            final boolean fboRequested, final CapsUtil capsUtil) {
        // FIXME rather pass the monitor(s) to the constructor, create a screen to get the primary monitor
        _newtWindow = GLWindow.create(capsUtil.getCapsForSettingsWithHints(settings, onscreen, bitmapRequested,
                pbufferRequested, fboRequested));
        _monitorDevices = new ArrayList<MonitorDevice>();
        // uses the primary monitor by default
        _newtWindow.getScreen().createNative();
        final MonitorDevice primaryMonitor = _newtWindow.getScreen().getPrimaryMonitor();
        _monitorDevices.add(primaryMonitor);
        // disables HiDPI, see https://github.com/gouessej/Ardor3D/issues/14
        _newtWindow.setSurfaceScale(new float[] { ScalableSurface.IDENTITY_PIXELSCALE,
                ScalableSurface.IDENTITY_PIXELSCALE });
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        if (settings.isFullScreen() && settings.getWidth() == 0 || settings.getHeight() == 0) {
            // FIXME use all available monitor devices to compute the size
            final DimensionImmutable currentResolution = primaryMonitor.queryCurrentMode().getSurfaceSize()
                    .getResolution();
            _settings = new DisplaySettings(currentResolution.getWidth(), currentResolution.getHeight(),
                    settings.getColorDepth(), settings.getFrequency(), settings.getAlphaBits(),
                    settings.getDepthBits(), settings.getStencilBits(), settings.getSamples(), true,
                    settings.isStereo(), settings.getShareContext(), settings.getRotation());
        } else {
            _settings = settings;
        }
        _canvasRenderer = canvasRenderer;
        _canvasRenderer._doSwap = true;// true - do swap in renderer.
        setAutoSwapBufferMode(false);// false - doesn't swap automatically in JOGL itself
    }

    /**
     * Applies all settings not related to OpenGL (screen resolution, screen size, etc...)
     * */
    protected void applySettings() {
        _newtWindow.setUndecorated(_settings.isFullScreen());
        if (_settings.isFullScreen()) {
            _newtWindow.setFullscreen(_monitorDevices);
            for (final MonitorDevice monitorDevice : _monitorDevices) {
                List<MonitorMode> monitorModes = monitorDevice.getSupportedModes();
                final MonitorMode currentMode = monitorDevice.getCurrentMode();
                if (monitorDevice == _monitorDevices.get(0)) {
                    final Dimension dimension;
                    // the resolution is taken into account only if it is valid
                    if (_settings.getWidth() > 0 && _settings.getHeight() > 0) {
                        dimension = new Dimension(_settings.getWidth(), _settings.getHeight());
                    } else {
                        final DimensionImmutable currentResolution = currentMode.getSurfaceSize().getResolution();
                        dimension = new Dimension(currentResolution.getWidth(), currentResolution.getHeight());
                    }
                    monitorModes = MonitorModeUtil.filterByResolution(monitorModes, dimension);
                } else {
                    // FIXME the display settings should store the size of the other monitors
                }
                // if the frequency may be valid (greater than zero), it tries to use it
                final List<MonitorMode> byRateMonitorModes;
                if (_settings.getFrequency() > 0) {
                    byRateMonitorModes = MonitorModeUtil.filterByRate(monitorModes, _settings.getFrequency());
                } else {
                    // if the frequency is set to zero, it tries to preserve the refresh rate
                    if (_settings.getFrequency() == 0) {
                        byRateMonitorModes = MonitorModeUtil.filterByRate(monitorModes, currentMode.getRefreshRate());
                    } else {
                        // otherwise it picks the highest available rate
                        byRateMonitorModes = MonitorModeUtil.getHighestAvailableRate(monitorModes);
                    }
                }
                if (!byRateMonitorModes.isEmpty()) {
                    monitorModes = byRateMonitorModes;
                }
                final List<MonitorMode> byBppMonitorModes;
                switch (_settings.getColorDepth()) {
                    case 16:
                    case 24:
                    case 32: {
                        byBppMonitorModes = MonitorModeUtil.filterByBpp(monitorModes, _settings.getColorDepth());
                        break;
                    }
                    case 0: {
                        byBppMonitorModes = MonitorModeUtil.filterByBpp(monitorModes, currentMode.getSurfaceSize()
                                .getBitsPerPixel());
                        break;
                    }
                    case -1: {
                        byBppMonitorModes = MonitorModeUtil.getHighestAvailableBpp(monitorModes);
                        break;
                    }
                    default: {
                        byBppMonitorModes = monitorModes;
                    }
                }
                if (!byBppMonitorModes.isEmpty()) {
                    monitorModes = byBppMonitorModes;
                }
                if (_settings.getRotation() == 0 || _settings.getRotation() == 90 || _settings.getRotation() == 180
                        || _settings.getRotation() == 270) {
                    final List<MonitorMode> rotatedMonitorModes = MonitorModeUtil.filterByRotation(monitorModes,
                            _settings.getRotation());
                    if (!rotatedMonitorModes.isEmpty()) {
                        monitorModes = rotatedMonitorModes;
                    }
                }
                monitorDevice.setCurrentMode(monitorModes.get(0));
            }
        } else {
            _newtWindow.setFullscreen(false);
        }
    }

    public void addKeyListener(final KeyListener keyListener) {
        _newtWindow.addKeyListener(keyListener);
    }

    public void addMouseListener(final MouseListener mouseListener) {
        _newtWindow.addMouseListener(mouseListener);
    }

    public void addWindowListener(final WindowListener windowListener) {
        _newtWindow.addWindowListener(windowListener);
    }

    public GLContext getContext() {
        return _newtWindow.getContext();
    }

    /**
     * Returns the width of the client area including insets (window decorations) in window units.
     *
     * @return width of the client area including insets (window decorations) in window units
     */
    public int getWidth() {
        return _newtWindow.getWidth() + (_newtWindow.getInsets() == null ? 0 : _newtWindow.getInsets().getTotalWidth());
    }

    /**
     * Returns the width of the client area including insets (window decorations) in pixel units.
     *
     * @return width of the client area including insets (window decorations) in pixel units
     */
    public int getWidthInPixelUnits() {
        return _newtWindow.convertToPixelUnits(new int[] { getWidth(), 0 })[0];
    }

    /**
     * Returns the width of the client area excluding insets (window decorations) in window units.
     *
     * @return width of the client area excluding insets (window decorations) in window units
     */
    public int getSurfaceWidthInWindowUnits() {
        return _newtWindow.getWidth();
    }

    /**
     * Returns the width of the client area excluding insets (window decorations) in pixel units.
     *
     * @return width of the client area excluding insets (window decorations) in pixel units
     */
    public int getSurfaceWidth() {
        return _newtWindow.getSurfaceWidth();
    }

    /**
     * Returns the height of the client area including insets (window decorations) in window units.
     *
     * @return height of the client area including insets (window decorations) in window units
     */
    public int getHeight() {
        return _newtWindow.getHeight()
                + (_newtWindow.getInsets() == null ? 0 : _newtWindow.getInsets().getTotalHeight());
    }

    /**
     * Returns the height of the client area including insets (window decorations) in pixel units.
     *
     * @return height of the client area including insets (window decorations) in pixel units
     */
    public int getHeightInPixelUnits() {
        return _newtWindow.convertToPixelUnits(new int[] { 0, getHeight() })[1];
    }

    /**
     * Returns the height of the client area excluding insets (window decorations) in window units.
     *
     * @return height of the client area excluding insets (window decorations) in window units
     */
    public int getSurfaceHeightInWindowUnits() {
        return _newtWindow.getHeight();
    }

    /**
     * Returns the height of the client area excluding insets (window decorations) in pixel units.
     *
     * @return height of the client area excluding insets (window decorations) in pixel units
     */
    public int getSurfaceHeight() {
        return _newtWindow.getSurfaceHeight();
    }

    public int getX() {
        return _newtWindow.getX();
    }

    public int getY() {
        return _newtWindow.getY();
    }

    public boolean isVisible() {
        return _newtWindow.isVisible();
    }

    public void setSize(final int width, final int height) {
        _newtWindow.setTopLevelSize(width, height);
    }

    public void setVisible(final boolean visible) {
        _newtWindow.setVisible(visible);
    }

    /**
     * Enables or disables automatic buffer swapping for this JoglNewtWindow. By default this property is set to false
     *
     * @param autoSwapBufferModeEnabled
     */
    public void setAutoSwapBufferMode(final boolean autoSwapBufferModeEnabled) {
        _newtWindow.setAutoSwapBufferMode(autoSwapBufferModeEnabled);
    }

    @Override
    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Set the size very early to prevent the default one from being used (typically when exiting full screen mode)
        if (_settings.getWidth() == 0 || _settings.getHeight() == 0) {
            final DimensionImmutable currentResolution = _monitorDevices.get(0).queryCurrentMode().getSurfaceSize()
                    .getResolution();
            setSize(currentResolution.getWidth(), currentResolution.getHeight());
        } else {
            setSize(_settings.getWidth(), _settings.getHeight());
        }
        // Make the window visible to realize the OpenGL surface.
        setVisible(true);
        if (_newtWindow.isRealized()) {
            _newtWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowDestroyNotify(final WindowEvent e) {
                    _isClosing = true;
                }

                // public void windowResized(final WindowEvent e) {
                // _newtWindow.invoke(true, new GLRunnable() {
                //
                // @Override
                // public boolean run(GLAutoDrawable glAutoDrawable) {
                // _canvasRenderer._camera.resize(_newtWindow.getWidth(), _newtWindow.getHeight());
                // _canvasRenderer._camera.setFrustumPerspective(_canvasRenderer._camera.getFovY(),
                // (float) _newtWindow.getWidth() / (float) _newtWindow.getHeight(),
                // _canvasRenderer._camera.getFrustumNear(),
                // _canvasRenderer._camera.getFrustumFar());
                // return true;
                // }
                // });
                // }
            });

            // Request the focus here as it cannot work when the window is not visible
            _newtWindow.requestFocus();
            applySettings();

            _canvasRenderer.setContext(getContext());

            _newtWindow.invoke(true, new GLRunnable() {
                @Override
                public boolean run(final GLAutoDrawable glAutoDrawable) {
                    _canvasRenderer.init(_settings, _canvasRenderer._doSwap);
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

        if (/* isShowing() */isVisible()) {
            _newtWindow.invoke(true, _drawerGLRunnable);
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
    public void close() {
        _newtWindow.destroy();
    }

    @Override
    public boolean isActive() {
        return _newtWindow.hasFocus();
    }

    @Override
    public boolean isClosing() {
        return _isClosing;
    }

    @Override
    public void setVSyncEnabled(final boolean enabled) {
        _newtWindow.invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable glAutoDrawable) {
                _newtWindow.getGL().setSwapInterval(enabled ? 1 : 0);
                return false;
            }
        });
    }

    @Override
    public void setTitle(final String title) {
        _newtWindow.setTitle(title);
    }

    @Override
    public void setIcon(final Image[] iconImages) {
        // FIXME supported by NEWT but not yet implemented, use System.setProperty("newt.window.icons",
        // "my-path-to-my-icon-file/icon.png");
    }

    @Override
    public void moveWindowTo(final int locX, final int locY) {
        _newtWindow.setTopLevelPosition(locX, locY);
    }

    public boolean isResizable() {
        return _newtWindow.isResizable();
    }

    public void setResizable(final boolean resizable) {
        _newtWindow.setResizable(resizable);
    }

    public boolean isDecorated() {
        return !_newtWindow.isUndecorated();
    }

    public void setDecorated(final boolean decorated) {
        _newtWindow.setUndecorated(!decorated);
    }

    public boolean isSticky() {
        return _newtWindow.isSticky();
    }

    public void setSticky(final boolean sticky) {
        _newtWindow.setSticky(sticky);
    }

    public boolean isAlwaysOnTop() {
        return _newtWindow.isAlwaysOnTop();
    }

    public void setAlwaysOnTop(final boolean alwaysOnTop) {
        _newtWindow.setAlwaysOnTop(alwaysOnTop);
    }

    public boolean isAlwaysOnBottom() {
        return _newtWindow.isAlwaysOnBottom();
    }

    public void setAlwaysOnBottom(final boolean alwaysOnBottom) {
        _newtWindow.setAlwaysOnBottom(alwaysOnBottom);
    }

    /**
     * Returns a list of monitor devices used in fullscreen mode, ignored in windowed mode
     *
     * @return list of monitor devices used in fullscreen mode, ignored in windowed mode
     */
    public List<MonitorDevice> getMonitorDevices() {
        return Collections.unmodifiableList(_monitorDevices);
    }

    public void setMonitorDevices(final List<MonitorDevice> monitorDevices) {
        if (_monitorDevices == null || _monitorDevices.isEmpty()) {
            throw new IllegalArgumentException("The list of monitor devices cannot be null or empty");
        }

        _monitorDevices = monitorDevices;
        // FIXME recompute the width and the height of the settings, apply the settings anew
    }

    @Override
    public GLWindow getNewtWindow() {
        return _newtWindow;
    }
}
