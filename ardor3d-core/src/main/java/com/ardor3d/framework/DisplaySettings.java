/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

public class DisplaySettings {
    /** canvas (unrotated) width */
    private final int _width;
    /** canvas (unrotated) height */
    private final int _height;
    /** number of color bits used to represent the color of a single pixel */
    private final int _colorDepth;
    private final int _frequency;
    private final int _alphaBits;
    private final int _depthBits;
    private final int _stencilBits;
    /** number of samples used to anti-alias */
    private final int _samples;
    /** true if the canvas should assume exclusive access to the screen */
    private final boolean _fullScreen;
    /** true if the canvas should be rendered stereoscopically (for 3D glasses) */
    private final boolean _stereo;
    /** OpenGL shared canvas renderer, can be null */
    private final CanvasRenderer _shareContext;
    /** rotation in degrees, can be equal to 0, 90, 180 or 270 */
    private final int _rotation;

    /**
     * Creates a new <code>DisplaySettings</code> object.
     *
     * @param width
     *            the canvas (unrotated) width
     * @param height
     *            the canvas (unrotated) height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param alphaBits
     *            the numner of bits used to represent the translucency of a single pixel
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param stencilBits
     *            the number of bits making up the stencil buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @param stereo
     *            true if the canvas should be rendered stereoscopically (for 3D glasses)
     * @param shareContext
     *            the renderer used to render the canvas (see "ardor3d.useMultipleContexts" property)
     * @param rotation
     *            the rotation of the first monitor
     * @see http://en.wikipedia.org/wiki/Z-buffering
     * @see http://en.wikipedia.org/wiki/Multisample_anti-aliasing
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     * @see http://en.wikipedia.org/wiki/Alpha_compositing
     * @see http://en.wikipedia.org/wiki/Stencil_buffer
     * @see http://en.wikipedia.org/wiki/Stereoscopy
     * @see http://www.ardor3d.com/forums/viewtopic.php?f=13&t=318&p=2311&hilit=ardor3d.useMultipleContexts#p2311
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo, final CanvasRenderer shareContext, final int rotation) {
        super();
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = alphaBits;
        _depthBits = depthBits;
        _stencilBits = stencilBits;
        _samples = samples;
        _fullScreen = fullScreen;
        _stereo = stereo;
        _shareContext = shareContext;
        _rotation = rotation;
    }

    /**
     * Convenience method
     *
     * @param width
     *            the canvas (unrotated) width
     * @param height
     *            the canvas (unrotated) height
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @see http://en.wikipedia.org/wiki/Z-buffering
     * @see http://en.wikipedia.org/wiki/Multisample_anti-aliasing
     */
    public DisplaySettings(final int width, final int height, final int depthBits, final int samples) {
        this(width, height, 0, 0, 0, depthBits, 0, samples, false, false, null, 0);
    }

    /**
     * Convenience method
     *
     * @param width
     *            the canvas (unrotated) width
     * @param height
     *            the canvas (unrotated) height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final boolean fullScreen) {
        this(width, height, colorDepth, frequency, 0, 8, 0, 0, fullScreen, false, null, 0);
    }

    /**
     * Convenience method equivalent to <code>DisplaySettings(width, height, colorDepth, frequency,
     * alphaBits, depthBits, stencilBits, samples, fullScreen, stereo, null, 0)</code>
     *
     * @param width
     *            the canvas (unrotated) width
     * @param height
     *            the canvas (unrotated) height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param alphaBits
     *            the numner of bits used to represent the translucency of a single pixel
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param stencilBits
     *            the number of bits making up the stencil buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @param stereo
     *            true if the canvas should be rendered stereoscopically (for 3D glasses)
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     * @see http://en.wikipedia.org/wiki/Alpha_compositing
     * @see http://en.wikipedia.org/wiki/Stencil_buffer
     * @see http://en.wikipedia.org/wiki/Stereoscopy
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo) {
        this(width, height, colorDepth, frequency, alphaBits, depthBits, stencilBits, samples, fullScreen, stereo,
                null, 0);
    }

    /**
     * Creates a new <code>DisplaySettings</code> object with no rotation.
     *
     * @param width
     *            the canvas (unrotated) width
     * @param height
     *            the canvas (unrotated) height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param alphaBits
     *            the numner of bits used to represent the translucency of a single pixel
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param stencilBits
     *            the number of bits making up the stencil buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @param stereo
     *            true if the canvas should be rendered stereoscopically (for 3D glasses)
     * @param shareContext
     *            the renderer used to render the canvas (see "ardor3d.useMultipleContexts" property)
     * @see http://en.wikipedia.org/wiki/Z-buffering
     * @see http://en.wikipedia.org/wiki/Multisample_anti-aliasing
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     * @see http://en.wikipedia.org/wiki/Alpha_compositing
     * @see http://en.wikipedia.org/wiki/Stencil_buffer
     * @see http://en.wikipedia.org/wiki/Stereoscopy
     * @see http://www.ardor3d.com/forums/viewtopic.php?f=13&t=318&p=2311&hilit=ardor3d.useMultipleContexts#p2311
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo, final CanvasRenderer shareContext) {
        this(width, height, colorDepth, frequency, alphaBits, depthBits, stencilBits, samples, fullScreen, stereo,
                shareContext, 0);
    }

    public CanvasRenderer getShareContext() {
        return _shareContext;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public int getColorDepth() {
        return _colorDepth;
    }

    public int getFrequency() {
        return _frequency;
    }

    public int getAlphaBits() {
        return _alphaBits;
    }

    public int getDepthBits() {
        return _depthBits;
    }

    public int getStencilBits() {
        return _stencilBits;
    }

    public int getSamples() {
        return _samples;
    }

    public boolean isFullScreen() {
        return _fullScreen;
    }

    public boolean isStereo() {
        return _stereo;
    }

    public int getRotation() {
        return _rotation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DisplaySettings that = (DisplaySettings) o;

        return _colorDepth == that._colorDepth
                && _frequency == that._frequency
                && _fullScreen == that._fullScreen
                && _height == that._height
                && _width == that._width
                && _alphaBits == that._alphaBits
                && _depthBits == that._depthBits
                && _stencilBits == that._stencilBits
                && _samples == that._samples
                && _stereo == that._stereo
                && ((_shareContext == that._shareContext) || (_shareContext != null && _shareContext
                        .equals(that._shareContext))) && _rotation == that._rotation;
    }

    @Override
    public int hashCode() {
        int result;
        result = 17;
        result = 31 * result + _height;
        result = 31 * result + _width;
        result = 31 * result + _colorDepth;
        result = 31 * result + _frequency;
        result = 31 * result + _alphaBits;
        result = 31 * result + _depthBits;
        result = 31 * result + _stencilBits;
        result = 31 * result + _samples;
        result = 31 * result + (_fullScreen ? 1 : 0);
        result = 31 * result + (_stereo ? 1 : 0);
        result = 31 * result + (_shareContext == null ? 0 : _shareContext.hashCode());
        result = 31 * result + _rotation;
        return result;
    }
}
