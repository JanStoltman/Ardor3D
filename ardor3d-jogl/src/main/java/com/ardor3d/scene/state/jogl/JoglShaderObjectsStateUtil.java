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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ShaderObjectsStateRecord;
import com.ardor3d.scene.state.jogl.shader.JoglShaderUtil;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3ES3;
import com.jogamp.opengl.GLContext;

public abstract class JoglShaderObjectsStateUtil {
    private static final Logger logger = Logger.getLogger(JoglShaderObjectsStateUtil.class.getName());

    protected static void sendToGL(final GLSLShaderObjectsState state, final ContextCapabilities caps) {
        final GL gl = GLContext.getCurrentGL();

        if (state.getVertexShader() == null && state.getFragmentShader() == null) {
            logger.warning("Could not find shader resources!" + "(both inputbuffers are null)");
            state._needSendShader = false;
            return;
        }

        if (state._programID == -1) {
            if (gl.isGL2()) {
                state._programID = (int) gl.getGL2().glCreateProgramObjectARB();
            } else {
                if (gl.isGL2ES2()) {
                    state._programID = gl.getGL2ES2().glCreateProgram();
                }
            }
        }

        if (state.getVertexShader() != null) {
            if (state._vertexShaderID != -1) {
                removeVertShader(state);
            }
            if (gl.isGL2()) {
                state._vertexShaderID = (int) gl.getGL2().glCreateShaderObjectARB(GL2ES2.GL_VERTEX_SHADER);
            } else {
                if (gl.isGL2ES2()) {
                    state._vertexShaderID = gl.getGL2ES2().glCreateShader(GL2ES2.GL_VERTEX_SHADER);
                }
            }

            // Create the sources
            final byte array[] = new byte[state.getVertexShader().limit()];
            state.getVertexShader().rewind();
            state.getVertexShader().get(array);
            if (gl.isGL2()) {
                gl.getGL2().glShaderSourceARB(state._vertexShaderID, 1, new String[] { new String(array) },
                        new int[] { array.length }, 0);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glShaderSource(state._vertexShaderID, 1, new String[] { new String(array) },
                            new int[] { array.length }, 0);
                }
            }

            // Compile the vertex shader
            final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
            final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
            compiled.clear();
            if (gl.isGL2()) {
                gl.getGL2().glCompileShaderARB(state._vertexShaderID);
                gl.getGL2()
                        .glGetObjectParameterivARB(state._vertexShaderID, GL2.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glCompileShader(state._vertexShaderID);
                    gl.getGL2ES2().glGetShaderiv(state._vertexShaderID, GL2ES2.GL_COMPILE_STATUS, compiled);
                }
            }
            checkShaderError(compiled.get(0), state._vertexShaderID, state._vertexShaderName);

            // Attach the program
            if (gl.isGL2()) {
                gl.getGL2().glAttachObjectARB(state._programID, state._vertexShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glAttachShader(state._programID, state._vertexShaderID);
                }
            }
        } else if (state._vertexShaderID != -1) {
            removeVertShader(state);
            state._vertexShaderID = -1;
        }

        if (state.getFragmentShader() != null) {
            if (state._fragmentShaderID != -1) {
                removeFragShader(state);
            }

            if (gl.isGL2()) {
                state._fragmentShaderID = (int) gl.getGL2().glCreateShaderObjectARB(GL2ES2.GL_FRAGMENT_SHADER);
            } else {
                if (gl.isGL2ES2()) {
                    state._fragmentShaderID = gl.getGL2ES2().glCreateShader(GL2ES2.GL_FRAGMENT_SHADER);
                }
            }

            // Create the sources
            final byte array[] = new byte[state.getFragmentShader().limit()];
            state.getFragmentShader().rewind();
            state.getFragmentShader().get(array);
            if (gl.isGL2()) {
                gl.getGL2().glShaderSourceARB(state._fragmentShaderID, 1, new String[] { new String(array) },
                        new int[] { array.length }, 0);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glShaderSource(state._fragmentShaderID, 1, new String[] { new String(array) },
                            new int[] { array.length }, 0);
                }
            }

            // Compile the fragment shader
            final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
            final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
            compiled.clear();
            if (gl.isGL2()) {
                gl.getGL2().glCompileShaderARB(state._fragmentShaderID);
                gl.getGL2().glGetObjectParameterivARB(state._fragmentShaderID, GL2.GL_OBJECT_COMPILE_STATUS_ARB,
                        compiled);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glCompileShader(state._fragmentShaderID);
                    gl.getGL2ES2().glGetShaderiv(state._fragmentShaderID, GL2ES2.GL_COMPILE_STATUS, compiled);
                }
            }
            checkShaderError(compiled.get(0), state._fragmentShaderID, state._vertexShaderName);

            // Attach the program
            if (gl.isGL2()) {
                gl.getGL2().glAttachObjectARB(state._programID, state._fragmentShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glAttachShader(state._programID, state._fragmentShaderID);
                }
            }
        } else if (state._fragmentShaderID != -1) {
            removeFragShader(state);
            state._fragmentShaderID = -1;
        }

        if (caps.isGeometryShader4Supported()) {
            if (state.getGeometryShader() != null) {
                if (state._geometryShaderID != -1) {
                    removeGeomShader(state);
                }

                if (gl.isGL2()) {
                    state._geometryShaderID = (int) gl.getGL2().glCreateShaderObjectARB(GL3.GL_GEOMETRY_SHADER);
                } else {
                    if (gl.isGL2ES2()) {
                        state._geometryShaderID = gl.getGL2ES2().glCreateShader(GL3.GL_GEOMETRY_SHADER);
                    }
                }

                // Create the sources
                final byte array[] = new byte[state.getGeometryShader().limit()];
                state.getGeometryShader().rewind();
                state.getGeometryShader().get(array);
                if (gl.isGL2()) {
                    gl.getGL2().glShaderSourceARB(state._geometryShaderID, 1, new String[] { new String(array) },
                            new int[] { array.length }, 0);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glShaderSource(state._geometryShaderID, 1, new String[] { new String(array) },
                                new int[] { array.length }, 0);
                    }
                }

                // Compile the geometry shader
                final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
                final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
                compiled.clear();
                if (gl.isGL2()) {
                    gl.getGL2().glCompileShaderARB(state._geometryShaderID);
                    gl.getGL2().glGetObjectParameterivARB(state._geometryShaderID, GL2.GL_OBJECT_COMPILE_STATUS_ARB,
                            compiled);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glCompileShader(state._geometryShaderID);
                        gl.getGL2ES2().glGetShaderiv(state._geometryShaderID, GL2ES2.GL_COMPILE_STATUS, compiled);
                    }
                }
                checkShaderError(compiled.get(0), state._geometryShaderID, state._geometryShaderName);

                // Attach the program
                if (gl.isGL2()) {
                    gl.getGL2().glAttachObjectARB(state._programID, state._geometryShaderID);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glAttachShader(state._programID, state._geometryShaderID);
                    }
                }
            } else if (state._geometryShaderID != -1) {
                removeGeomShader(state);
                state._geometryShaderID = -1;
            }
        }

        if (caps.isTessellationShadersSupported()) {
            if (state.getTessellationControlShader() != null) {
                if (state._tessellationControlShaderID != -1) {
                    removeTessControlShader(state);
                }

                if (gl.isGL2()) {
                    state._tessellationControlShaderID = (int) gl.getGL2().glCreateShaderObjectARB(
                            GL3.GL_TESS_CONTROL_SHADER);
                } else {
                    if (gl.isGL2ES2()) {
                        state._tessellationControlShaderID = gl.getGL2ES2().glCreateShader(GL3.GL_TESS_CONTROL_SHADER);
                    }
                }

                // Create the sources
                final byte array[] = new byte[state.getTessellationControlShader().limit()];
                state.getTessellationControlShader().rewind();
                state.getTessellationControlShader().get(array);
                if (gl.isGL2()) {
                    gl.getGL2().glShaderSourceARB(state._tessellationControlShaderID, 1,
                            new String[] { new String(array) }, new int[] { array.length }, 0);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glShaderSource(state._tessellationControlShaderID, 1,
                                new String[] { new String(array) }, new int[] { array.length }, 0);
                    }
                }

                // Compile the tessellation control shader
                final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
                final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
                compiled.clear();
                if (gl.isGL2()) {
                    gl.getGL2().glCompileShaderARB(state._tessellationControlShaderID);
                    gl.getGL2().glGetObjectParameterivARB(state._tessellationControlShaderID,
                            GL2.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glCompileShader(state._tessellationControlShaderID);
                        gl.getGL2ES2().glGetShaderiv(state._tessellationControlShaderID, GL2ES2.GL_COMPILE_STATUS,
                                compiled);
                    }
                }
                checkShaderError(compiled.get(0), state._tessellationControlShaderID,
                        state._tessellationControlShaderName);

                // Attach the program
                if (gl.isGL2()) {
                    gl.getGL2().glAttachObjectARB(state._programID, state._tessellationControlShaderID);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glAttachShader(state._programID, state._tessellationControlShaderID);
                    }
                }
            } else if (state._tessellationControlShaderID != -1) {
                removeTessControlShader(state);
                state._tessellationControlShaderID = -1;
            }
            if (state.getTessellationEvaluationShader() != null) {
                if (state._tessellationEvaluationShaderID != -1) {
                    removeTessEvalShader(state);
                }

                if (gl.isGL2()) {
                    state._tessellationEvaluationShaderID = (int) gl.getGL2().glCreateShaderObjectARB(
                            GL3.GL_TESS_CONTROL_SHADER);
                } else {
                    if (gl.isGL2ES2()) {
                        state._tessellationEvaluationShaderID = gl.getGL2ES2().glCreateShader(
                                GL3.GL_TESS_CONTROL_SHADER);
                    }
                }

                // Create the sources
                final byte array[] = new byte[state.getTessellationEvaluationShader().limit()];
                state.getTessellationEvaluationShader().rewind();
                state.getTessellationEvaluationShader().get(array);
                if (gl.isGL2()) {
                    gl.getGL2().glShaderSourceARB(state._tessellationEvaluationShaderID, 1,
                            new String[] { new String(array) }, new int[] { array.length }, 0);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glShaderSource(state._tessellationEvaluationShaderID, 1,
                                new String[] { new String(array) }, new int[] { array.length }, 0);
                    }
                }

                // Compile the tessellation evaluation shader
                final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
                final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
                compiled.clear();
                if (gl.isGL2()) {
                    gl.getGL2().glCompileShaderARB(state._tessellationEvaluationShaderID);
                    gl.getGL2().glGetObjectParameterivARB(state._tessellationEvaluationShaderID,
                            GL2.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glCompileShader(state._tessellationEvaluationShaderID);
                        gl.getGL2ES2().glGetShaderiv(state._tessellationEvaluationShaderID, GL2ES2.GL_COMPILE_STATUS,
                                compiled);
                    }
                }
                checkShaderError(compiled.get(0), state._tessellationEvaluationShaderID,
                        state._tessellationEvaluationShaderName);

                // Attach the program
                if (gl.isGL2()) {
                    gl.getGL2().glAttachObjectARB(state._programID, state._tessellationEvaluationShaderID);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glAttachShader(state._programID, state._tessellationEvaluationShaderID);
                    }
                }
            } else if (state._tessellationEvaluationShaderID != -1) {
                removeTessEvalShader(state);
                state._tessellationEvaluationShaderID = -1;
            }
        }

        if (caps.isComputeShaderSupported()) {
            if (state.getComputeShader() != null) {
                if (state._computeShaderID != -1) {
                    removeCompShader(state);
                }
                if (gl.isGL2()) {
                    state._computeShaderID = (int) gl.getGL2().glCreateShaderObjectARB(GL3ES3.GL_COMPUTE_SHADER);
                } else {
                    if (gl.isGL2ES2()) {
                        state._computeShaderID = gl.getGL2ES2().glCreateShader(GL3ES3.GL_COMPUTE_SHADER);
                    }
                }

                // Create the sources
                final byte array[] = new byte[state.getComputeShader().limit()];
                state.getComputeShader().rewind();
                state.getComputeShader().get(array);
                if (gl.isGL2()) {
                    gl.getGL2().glShaderSourceARB(state._computeShaderID, 1, new String[] { new String(array) },
                            new int[] { array.length }, 0);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glShaderSource(state._computeShaderID, 1, new String[] { new String(array) },
                                new int[] { array.length }, 0);
                    }
                }

                // Compile the compute shader
                final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
                final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
                compiled.clear();
                if (gl.isGL2()) {
                    gl.getGL2().glCompileShaderARB(state._computeShaderID);
                    gl.getGL2().glGetObjectParameterivARB(state._computeShaderID, GL2.GL_OBJECT_COMPILE_STATUS_ARB,
                            compiled);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glCompileShader(state._computeShaderID);
                        gl.getGL2ES2().glGetShaderiv(state._computeShaderID, GL2ES2.GL_COMPILE_STATUS, compiled);
                    }
                }
                checkShaderError(compiled.get(0), state._computeShaderID, state._computeShaderName);

                // Attach the program
                if (gl.isGL2()) {
                    gl.getGL2().glAttachObjectARB(state._programID, state._computeShaderID);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glAttachShader(state._programID, state._computeShaderID);
                    }
                }

            } else if (state._computeShaderID != -1) {
                removeCompShader(state);
                state._computeShaderID = -1;
            }
        }

        if (gl.isGL2()) {
            gl.getGL2().glLinkProgramARB(state._programID);
        } else {
            if (gl.isGL2ES2()) {
                gl.getGL2ES2().glLinkProgram(state._programID);
            }
        }
        checkLinkError(state._programID);
        state.setNeedsRefresh(true);
        state._needSendShader = false;
    }

    private static void checkLinkError(final int programId) {
        final GL gl = GLContext.getCurrentGL();

        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
        final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
        compiled.clear();
        if (gl.isGL2()) {
            gl.getGL2().glGetObjectParameterivARB(programId, GL2ES2.GL_LINK_STATUS, compiled);
        } else {
            if (gl.isGL2ES2()) {
                gl.getGL2ES2().glGetProgramiv(programId, GL2ES2.GL_LINK_STATUS, compiled);
            }
        }
        if (compiled.get(0) == GL.GL_FALSE) {
            if (gl.isGL2()) {
                gl.getGL2().glGetObjectParameterivARB(programId, GL2ES2.GL_INFO_LOG_LENGTH, compiled);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, compiled);
                }
            }
            final int length = compiled.get(0);
            String out = null;
            if (length > 0) {
                final ByteBuffer infoLogBuf = context.getDirectNioBuffersSet().getInfoLogBuffer();
                final ByteBuffer infoLog;
                if (length <= infoLogBuf.capacity()) {
                    infoLog = infoLogBuf;
                    infoLogBuf.rewind().limit(length);
                } else {
                    infoLog = BufferUtils.createByteBuffer(length);
                }
                if (gl.isGL2()) {
                    gl.getGL2().glGetInfoLogARB(programId, infoLog.limit(), compiled, infoLog);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glGetProgramInfoLog(programId, infoLog.limit(), compiled, infoLog);
                    }
                }

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            // throw new Ardor3dException("Error linking GLSL shader: " + out);
        }
    }

    /** Removes the fragment shader */
    private static void removeFragShader(final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();

        if (state._fragmentShaderID != -1) {
            if (gl.isGL2()) {
                gl.getGL2().glDetachObjectARB(state._programID, state._fragmentShaderID);
                gl.getGL2().glDeleteObjectARB(state._fragmentShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glDetachShader(state._programID, state._fragmentShaderID);
                    gl.getGL2ES2().glDeleteShader(state._fragmentShaderID);
                }
            }
        }
    }

    /** Removes the vertex shader */
    private static void removeVertShader(final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();

        if (state._vertexShaderID != -1) {
            if (gl.isGL2()) {
                gl.getGL2().glDetachObjectARB(state._programID, state._vertexShaderID);
                gl.getGL2().glDeleteObjectARB(state._vertexShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glDetachShader(state._programID, state._vertexShaderID);
                    gl.getGL2ES2().glDeleteShader(state._vertexShaderID);
                }
            }
        }
    }

    /** Removes the geometry shader */
    private static void removeGeomShader(final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();

        if (state._geometryShaderID != -1) {
            if (gl.isGL2()) {
                gl.getGL2().glDetachObjectARB(state._programID, state._geometryShaderID);
                gl.getGL2().glDeleteObjectARB(state._geometryShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glDetachShader(state._programID, state._geometryShaderID);
                    gl.getGL2ES2().glDeleteShader(state._geometryShaderID);
                }
            }
        }
    }

    /** Removes the tessellation control shader */
    private static void removeTessControlShader(final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();

        if (state._tessellationControlShaderID != -1) {
            if (gl.isGL2()) {
                gl.getGL2().glDetachObjectARB(state._programID, state._tessellationControlShaderID);
                gl.getGL2().glDeleteObjectARB(state._tessellationControlShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glDetachShader(state._programID, state._tessellationControlShaderID);
                    gl.getGL2ES2().glDeleteShader(state._tessellationControlShaderID);
                }
            }
        }
    }

    /** Removes the tessellation evaluation shader */
    private static void removeTessEvalShader(final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();

        if (state._tessellationEvaluationShaderID != -1) {
            if (gl.isGL2()) {
                gl.getGL2().glDetachObjectARB(state._programID, state._tessellationEvaluationShaderID);
                gl.getGL2().glDeleteObjectARB(state._tessellationEvaluationShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glDetachShader(state._programID, state._tessellationEvaluationShaderID);
                    gl.getGL2ES2().glDeleteShader(state._tessellationEvaluationShaderID);
                }
            }
        }
    }

    /** Removes the compute shader */
    private static void removeCompShader(final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();

        if (state._computeShaderID != -1) {
            if (gl.isGL2()) {
                gl.getGL2().glDetachObjectARB(state._programID, state._computeShaderID);
                gl.getGL2().glDeleteObjectARB(state._computeShaderID);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glDetachShader(state._programID, state._computeShaderID);
                    gl.getGL2ES2().glDeleteShader(state._computeShaderID);
                }
            }
        }
    }

    /**
     * Check for shader errors. If an error is detected, program exits.
     *
     * @param compilerState
     *            the compiler state for a given shader
     * @param shaderId
     *            shader's id
     */
    private static void checkShaderError(final int compilerState, final int shaderId, final String shaderName) {
        final GL gl = GLContext.getCurrentGL();

        if (compilerState == GL.GL_FALSE) {
            final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
            final IntBuffer iVal = context.getDirectNioBuffersSet().getSingleIntBuffer();
            iVal.clear();
            if (gl.isGL2()) {
                gl.getGL2().glGetObjectParameterivARB(shaderId, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glGetShaderiv(shaderId, GL2ES2.GL_INFO_LOG_LENGTH, iVal);
                }
            }
            final int length = iVal.get(0);
            String out = null;

            if (length > 0) {
                final ByteBuffer infoLogBuf = context.getDirectNioBuffersSet().getInfoLogBuffer();
                final ByteBuffer infoLog;
                if (length <= infoLogBuf.capacity()) {
                    infoLog = infoLogBuf;
                    infoLogBuf.rewind().limit(length);
                } else {
                    infoLog = BufferUtils.createByteBuffer(length);
                }
                if (gl.isGL2()) {
                    gl.getGL2().glGetInfoLogARB(shaderId, infoLog.limit(), iVal, infoLog);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glGetShaderInfoLog(shaderId, infoLog.limit(), iVal, infoLog);
                    }
                }

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            final String nameString = shaderName.equals("") ? "" : " [ " + shaderName + " ]";
            throw new Ardor3dException("Error compiling GLSL shader " + nameString + ": " + out);
        }
    }

    public static void apply(final JoglRenderer renderer, final GLSLShaderObjectsState state) {
        final GL gl = GLContext.getCurrentGL();
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (caps.isGLSLSupported()) {
            // Ask for the current state record
            final ShaderObjectsStateRecord record = (ShaderObjectsStateRecord) context
                    .getStateRecord(StateType.GLSLShader);
            context.setCurrentState(StateType.GLSLShader, state);

            if (state.isEnabled()) {
                if (state._needSendShader) {
                    sendToGL(state, caps);
                }

                if (state._shaderDataLogic != null) {
                    state._shaderDataLogic.applyData(state, state._mesh, renderer);
                }
            }

            if (!record.isValid() || record.getReference() != state || state.needsRefresh()) {
                record.setReference(state);
                if (state.isEnabled() && state._programID != -1) {
                    // clear any previously existing attributes
                    clearEnabledAttributes(record, gl);

                    // set our current shader
                    JoglShaderUtil.useShaderProgram(state._programID, record);

                    final List<ShaderVariable> attribs = state.getShaderAttributes();
                    for (int i = attribs.size(); --i >= 0;) {
                        final ShaderVariable shaderVariable = attribs.get(i);
                        if (shaderVariable.needsRefresh) {
                            JoglShaderUtil.updateAttributeLocation(shaderVariable, state._programID);
                            shaderVariable.needsRefresh = false;
                        }
                        JoglShaderUtil.updateShaderAttribute(renderer, shaderVariable, state.isUseAttributeVBO());
                    }

                    final List<ShaderVariable> uniforms = state.getShaderUniforms();
                    for (int i = uniforms.size(); --i >= 0;) {
                        final ShaderVariable shaderVariable = uniforms.get(i);
                        if (shaderVariable.needsRefresh) {
                            JoglShaderUtil.updateUniformLocation(shaderVariable, state._programID);
                            JoglShaderUtil.updateShaderUniform(shaderVariable);
                            shaderVariable.needsRefresh = false;
                        }
                    }
                } else {
                    JoglShaderUtil.useShaderProgram(0, record);

                    clearEnabledAttributes(record, gl);
                }
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }

    private static void clearEnabledAttributes(final ShaderObjectsStateRecord record, final GL gl) {
        // go through and disable any enabled attributes
        if (!record.enabledAttributes.isEmpty()) {
            for (int i = 0, maxI = record.enabledAttributes.size(); i < maxI; i++) {
                final ShaderVariable var = record.enabledAttributes.get(i);
                if (var.getSize() == 1) {
                    if (gl.isGL2()) {
                        gl.getGL2().glDisableVertexAttribArrayARB(var.variableID);
                    } else {
                        if (gl.isGL2ES2()) {
                            gl.getGL2ES2().glDisableVertexAttribArray(var.variableID);
                        }
                    }
                } else {
                    for (int j = 0, maxJ = var.getSize(); j < maxJ; j++) {
                        if (gl.isGL2()) {
                            gl.getGL2().glDisableVertexAttribArrayARB(var.variableID + j);
                        } else {
                            if (gl.isGL2ES2()) {
                                gl.getGL2ES2().glDisableVertexAttribArray(var.variableID + j);
                            }
                        }
                    }
                }
            }
            record.enabledAttributes.clear();
        }
    }
}
