/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.util.Ardor3dException;
import com.jogamp.newt.MonitorMode;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;

public class CapsUtil {

    static {
        // The GLSL mode of GLJPanel used in JoglSwingCanvas seems to cause a lot of troubles when using our own GLSL
        // shaders and with the wireframe state. Therefore, it is disabled as early as possible
        System.setProperty("jogl.gljpanel.noglsl", "true");
    }

    public GLProfile getProfile() {
        // tries to get the most capable profile, programmable or fixed, desktop or embedded, forward or backward
        // compatible
        GLProfile profile = GLProfile.getMaximum(true);
        final boolean isForwardCompatible = (!profile.isGL4() && profile.isGL3() && !profile.isGL3bc())
                || (profile.isGL4() && !profile.isGL4bc());
        if (isForwardCompatible) {
            // Ardor3D doesn't support forward compatible yet
            profile = GLProfile.getMaxFixedFunc(true);
        } else {
            final boolean isES2orES3 = profile.isGLES2() || profile.isGLES3();
            // Ardor3D doesn't fully support ES 2.0 and later yet, favors ES 1 if possible
            // FIXME remove this kludge when Ardor3D gets some VAO support
            if (isES2orES3 && GLProfile.isAvailable(GLProfile.GLES1)) {
                profile = GLProfile.get(GLProfile.GLES1);
            }
        }
        return profile;
    }

    public GLCapabilities getCapsForSettings(final DisplaySettings settings) {
        return getCapsForSettings(settings, true, false, false, false);
    }

    /**
     * for internal use only, tolerates artificial display settings containing hints
     *
     * @param settings
     * @param onscreen
     * @param bitmapRequested
     * @param pbufferRequested
     * @param fboRequested
     * @return
     */
    GLCapabilities getCapsForSettingsWithHints(final DisplaySettings settings, final boolean onscreen,
            final boolean bitmapRequested, final boolean pbufferRequested, final boolean fboRequested) {
        final DisplaySettings realSettings;
        if (settings.isFullScreen() && (settings.getWidth() == 0 || settings.getHeight() == 0)) {
            realSettings = new DisplaySettings(1, 1, settings.getColorDepth(), settings.getFrequency(),
                    settings.getAlphaBits(), settings.getDepthBits(), settings.getStencilBits(), settings.getSamples(),
                    true, settings.isStereo(), settings.getShareContext(), settings.getRotation());
        } else {
            realSettings = settings;
        }
        return getCapsForSettings(realSettings, onscreen, bitmapRequested, pbufferRequested, fboRequested);
    }

    public GLCapabilities getCapsForSettings(final DisplaySettings settings, final boolean onscreen,
            final boolean bitmapRequested, final boolean pbufferRequested, final boolean fboRequested) {

        // Validate window dimensions.
        if (settings.getWidth() <= 0 || settings.getHeight() <= 0) {
            throw new Ardor3dException("Invalid resolution values: " + settings.getWidth() + " " + settings.getHeight());
        }

        // Validate bit depth.
        if ((settings.getColorDepth() != 32) && (settings.getColorDepth() != 16) && (settings.getColorDepth() != 24)
                && (settings.getColorDepth() != 0) && (settings.getColorDepth() != -1)) {
            throw new Ardor3dException("Invalid pixel depth: " + settings.getColorDepth());
        }

        // Validate rotation
        if (settings.getRotation() != MonitorMode.ROTATE_0 && settings.getRotation() != MonitorMode.ROTATE_90
                && settings.getRotation() != MonitorMode.ROTATE_180 && settings.getRotation() != MonitorMode.ROTATE_270) {
            throw new Ardor3dException("Invalid rotation: " + settings.getRotation());
        }

        final GLCapabilities caps = new GLCapabilities(getProfile());
        caps.setHardwareAccelerated(true);
        caps.setDoubleBuffered(true);
        caps.setAlphaBits(settings.getAlphaBits());
        caps.setDepthBits(settings.getDepthBits());
        caps.setNumSamples(settings.getSamples());
        caps.setSampleBuffers(settings.getSamples() != 0);
        caps.setStereo(settings.isStereo());
        caps.setStencilBits(settings.getStencilBits());
        switch (settings.getColorDepth()) {
            case 32:
            case 24:
                caps.setRedBits(8);
                caps.setBlueBits(8);
                caps.setGreenBits(8);
                break;
            case 16:
                caps.setRedBits(4);
                caps.setBlueBits(4);
                caps.setGreenBits(4);
                break;
        }
        caps.setOnscreen(onscreen);
        if (!onscreen) {
            caps.setBitmap(bitmapRequested);
            caps.setPBuffer(pbufferRequested);
            caps.setFBO(fboRequested);
        }
        return caps;
    }

    public DisplaySettings getSettingsForCaps(final GLCapabilitiesImmutable glCaps, final int width, final int height,
            final int frequency, final boolean fullscreen, final CanvasRenderer shareContext, final int rotation) {
        final int colorDepth = glCaps.getRedBits() + glCaps.getGreenBits() + glCaps.getBlueBits();
        final DisplaySettings settings = new DisplaySettings(width, height, colorDepth, frequency,
                glCaps.getAlphaBits(), glCaps.getDepthBits(), glCaps.getStencilBits(), glCaps.getNumSamples(),
                fullscreen, glCaps.getStereo(), shareContext, rotation);
        return settings;
    }
}
