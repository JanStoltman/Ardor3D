/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl.state.record;

import com.ardor3d.renderer.state.record.RendererRecord;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.glsl.fixedfunc.FixedFuncUtil;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;

public class JoglRendererRecord extends RendererRecord {

    protected final JoglMatrixBackend _matrixBackend;

    public JoglRendererRecord() {
        final GL gl = GLContext.getCurrentGL();
        if (gl.isGL2ES1()) {
            _matrixBackend = new JoglRealMatrixBackend();
        } else {
            _matrixBackend = new JoglSimulatedMatrixBackend();
            FixedFuncUtil.wrapFixedFuncEmul(gl, ShaderSelectionMode.AUTO,
                    ((JoglSimulatedMatrixBackend) _matrixBackend)._matrix);
        }
    }

    public JoglMatrixBackend getMatrixBackend() {
        return _matrixBackend;
    }
}
