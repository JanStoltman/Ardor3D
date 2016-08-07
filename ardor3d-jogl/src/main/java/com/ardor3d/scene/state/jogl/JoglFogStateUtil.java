/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLContext;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FogState.CoordinateSource;
import com.ardor3d.renderer.state.FogState.DensityFunction;
import com.ardor3d.renderer.state.FogState.Quality;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.FogStateRecord;

public abstract class JoglFogStateUtil {

    public static void apply(final JoglRenderer renderer, final FogState state) {
        final GL gl = GLContext.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final FogStateRecord record = (FogStateRecord) context.getStateRecord(StateType.Fog);
        context.setCurrentState(StateType.Fog, state);

        if (state.isEnabled()) {
            enableFog(true, record);

            if (record.isValid()) {
                if (record.fogStart != state.getStart()) {
                    if (gl.isGL2ES1()) {
                        gl.getGL2ES1().glFogf(GL2ES1.GL_FOG_START, state.getStart());
                    }
                    record.fogStart = state.getStart();
                }
                if (record.fogEnd != state.getEnd()) {
                    if (gl.isGL2ES1()) {
                        gl.getGL2ES1().glFogf(GL2ES1.GL_FOG_END, state.getEnd());
                    }
                    record.fogEnd = state.getEnd();
                }
                if (record.density != state.getDensity()) {
                    if (gl.isGL2ES1()) {
                        gl.getGL2ES1().glFogf(GL2ES1.GL_FOG_DENSITY, state.getDensity());
                    }
                    record.density = state.getDensity();
                }
            } else {
                if (gl.isGL2ES1()) {
                    gl.getGL2ES1().glFogf(GL2ES1.GL_FOG_START, state.getStart());
                }
                record.fogStart = state.getStart();
                if (gl.isGL2ES1()) {
                    gl.getGL2ES1().glFogf(GL2ES1.GL_FOG_END, state.getEnd());
                }
                record.fogEnd = state.getEnd();
                if (gl.isGL2ES1()) {
                    gl.getGL2ES1().glFogf(GL2ES1.GL_FOG_DENSITY, state.getDensity());
                }
                record.density = state.getDensity();
            }

            final ReadOnlyColorRGBA fogColor = state.getColor();
            applyFogColor(fogColor, record);
            applyFogMode(state.getDensityFunction(), record);
            applyFogHint(state.getQuality(), record);
            applyFogSource(state.getSource(), record, caps);
        } else {
            enableFog(false, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableFog(final boolean enable, final FogStateRecord record) {
        final GL gl = GLContext.getCurrentGL();

        if (record.isValid()) {
            if (enable && !record.enabled) {
                if (gl.isGL2ES1()) {
                    gl.glEnable(GL2ES1.GL_FOG);
                }
                record.enabled = true;
            } else if (!enable && record.enabled) {
                if (gl.isGL2ES1()) {
                    gl.glDisable(GL2ES1.GL_FOG);
                }
                record.enabled = false;
            }
        } else {
            if (enable) {
                if (gl.isGL2ES1()) {
                    gl.glEnable(GL2ES1.GL_FOG);
                }
            } else {
                if (gl.isGL2ES1()) {
                    gl.glDisable(GL2ES1.GL_FOG);
                }
            }
            record.enabled = enable;
        }
    }

    private static void applyFogColor(final ReadOnlyColorRGBA color, final FogStateRecord record) {
        final GL gl = GLContext.getCurrentGL();

        if (!record.isValid() || !color.equals(record.fogColor)) {
            record.fogColor.set(color);
            record.colorBuff.clear();
            record.colorBuff.put(record.fogColor.getRed()).put(record.fogColor.getGreen())
                    .put(record.fogColor.getBlue()).put(record.fogColor.getAlpha());
            record.colorBuff.flip();
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glFogfv(GL2ES1.GL_FOG_COLOR, record.colorBuff);
            }
        }
    }

    private static void applyFogSource(final CoordinateSource source, final FogStateRecord record,
            final ContextCapabilities caps) {
        final GL gl = GLContext.getCurrentGL();

        if (caps.isFogCoordinatesSupported()) {
            if (!record.isValid() || !source.equals(record.source)) {
                if (source == CoordinateSource.Depth) {
                    if (gl.isGL2()) {
                        gl.getGL2().glFogi(GL2.GL_FOG_COORDINATE_SOURCE, GL2.GL_FRAGMENT_DEPTH);
                    }
                } else {
                    if (gl.isGL2()) {
                        gl.getGL2().glFogi(GL2.GL_FOG_COORDINATE_SOURCE, GL2.GL_FOG_COORDINATE);
                    }
                }
            }
        }
    }

    private static void applyFogMode(final DensityFunction densityFunction, final FogStateRecord record) {
        final GL gl = GLContext.getCurrentGL();

        int glMode = 0;
        switch (densityFunction) {
            case Exponential:
                glMode = GL2ES1.GL_EXP;
                break;
            case Linear:
                glMode = GL.GL_LINEAR;
                break;
            case ExponentialSquared:
                glMode = GL2ES1.GL_EXP2;
                break;
        }

        if (!record.isValid() || record.fogMode != glMode) {
            if (gl.isGL2()) {
                gl.getGL2().glFogi(GL2ES1.GL_FOG_MODE, glMode);
            }
            record.fogMode = glMode;
        }
    }

    private static void applyFogHint(final Quality quality, final FogStateRecord record) {
        final GL gl = GLContext.getCurrentGL();

        int glHint = 0;
        switch (quality) {
            case PerVertex:
                glHint = GL.GL_FASTEST;
                break;
            case PerPixel:
                glHint = GL.GL_NICEST;
                break;
        }

        if (!record.isValid() || record.fogHint != glHint) {
            if (gl.isGL2ES1()) {
                gl.glHint(GL2ES1.GL_FOG_HINT, glHint);
            }
            record.fogHint = glHint;
        }
    }
}
