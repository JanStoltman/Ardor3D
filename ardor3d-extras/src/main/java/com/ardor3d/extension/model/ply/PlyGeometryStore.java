/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.ply;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;

public class PlyGeometryStore {

    private int _totalMeshes = 0;

    private final PlyDataStore _dataStore;

    private final Node _root;

    private final List<PlyMaterial> _materialLibrary;

    private List<PlyFaceInfo> _plyFaceInfoList;

    private List<PlyEdgeInfo> _plyEdgeInfoList;

    private Texture _texture;

    private String _textureName;

    private final GeometryTool _geometryTool;

    public PlyGeometryStore() {
        this(new GeometryTool());
    }

    public PlyGeometryStore(final GeometryTool geometryTool) {
        super();
        _dataStore = new PlyDataStore();
        _root = new Node();
        _materialLibrary = new ArrayList<>();
        _geometryTool = geometryTool;
    }

    public PlyDataStore getDataStore() {
        return _dataStore;
    }

    public Node getScene() {
        return _root;
    }

    public List<PlyMaterial> getMaterialLibrary() {
        return _materialLibrary;
    }

    public String getTextureName() {
        return _textureName;
    }

    public void setTextureName(final String textureName) {
        _textureName = textureName;
    }

    public Texture getTexture() {
        return _texture;
    }

    public void setTexture(final Texture texture) {
        _texture = texture;
    }

    public TextureState getTextureState() {
        if (_texture != null) {
            final TextureState tState = new TextureState();
            tState.setTexture(_texture, 0);
            return tState;
        }
        return null;
    }

    void addLine(final PlyEdgeInfo edgeInfo) {
        if (_plyEdgeInfoList == null) {
            _plyEdgeInfoList = new ArrayList<>();
        }
        _plyEdgeInfoList.add(edgeInfo);
    }

    void addFace(final PlyFaceInfo faceInfo) {
        if (_plyFaceInfoList == null) {
            _plyFaceInfoList = new ArrayList<>();
        }
        _plyFaceInfoList.add(faceInfo);
    }

    @SuppressWarnings("null")
    void commitObjects() {
        if (_plyEdgeInfoList != null) {
            // TODO
            _plyEdgeInfoList = null;
        }
        if (_plyFaceInfoList != null) {
            final String name = "ply_mesh" + _totalMeshes;
            final Mesh mesh = new Mesh(name);
            boolean hasTexCoords = false;
            final boolean hasNormals = _dataStore.getNormals() != null && !_dataStore.getNormals().isEmpty();
            final boolean hasColors = _dataStore.getColors() != null && !_dataStore.getColors().isEmpty();
            // FIXME sort the faces by vertex count per face, puts them into distinct regions
            int vertexCount = 0;
            for (final PlyFaceInfo plyFaceInfo : _plyFaceInfoList) {
                vertexCount += plyFaceInfo.getVertexIndices().size();
                if (plyFaceInfo.getTextureCoordinates() != null && !plyFaceInfo.getTextureCoordinates().isEmpty()) {
                    hasTexCoords = true;
                }
            }
            final FloatBuffer vertices = BufferUtils.createVector3Buffer(vertexCount);
            final IndexBufferData<? extends Buffer> indices = BufferUtils.createIndexBufferData(vertexCount,
                    vertexCount - 1);

            final FloatBuffer normals = hasNormals ? BufferUtils.createFloatBuffer(vertices.capacity()) : null;
            final FloatBuffer colors = hasColors ? BufferUtils.createFloatBuffer(vertexCount * 4) : null;
            final FloatBuffer uvs = hasTexCoords ? BufferUtils.createFloatBuffer(vertexCount * 2) : null;

            // FIXME handle material indices if any
            final EnumSet<MatchCondition> matchConditions = EnumSet.noneOf(MatchCondition.class);
            int dummyVertexIndex = 0;
            for (final PlyFaceInfo plyFaceInfo : _plyFaceInfoList) {
                for (final Integer vertexIndex : plyFaceInfo.getVertexIndices()) {
                    indices.put(dummyVertexIndex);
                    dummyVertexIndex++;
                    final Vector3 vertex = _dataStore.getVertices().get(vertexIndex.intValue());
                    vertices.put(vertex.getXf()).put(vertex.getYf()).put(vertex.getZf());
                    if (hasNormals) {
                        final Vector3 normal = _dataStore.getNormals().get(vertexIndex.intValue());
                        normals.put(normal.getXf()).put(normal.getYf()).put(normal.getZf());
                    }
                    if (hasColors) {
                        final ColorRGBA color = _dataStore.getColors().get(vertexIndex.intValue());
                        colors.put(color.getRed()).put(color.getGreen()).put(color.getBlue()).put(color.getAlpha());
                    }
                }
                if (hasTexCoords) {
                    for (final Float texCoord : plyFaceInfo.getTextureCoordinates()) {
                        uvs.put(texCoord);
                    }
                }
            }

            vertices.rewind();
            mesh.getMeshData().setVertexBuffer(vertices);
            indices.rewind();
            mesh.getMeshData().setIndices(indices);
            if (hasNormals) {
                normals.rewind();
                mesh.getMeshData().setNormalBuffer(normals);
                matchConditions.add(MatchCondition.Normal);
            }
            if (hasColors) {
                colors.rewind();
                mesh.getMeshData().setColorBuffer(colors);
                matchConditions.add(MatchCondition.Color);
            }
            if (hasTexCoords) {
                uvs.rewind();
                mesh.getMeshData().setTextureBuffer(uvs, 0);
                matchConditions.add(MatchCondition.UVs);
            }

            _geometryTool.minimizeVerts(mesh, matchConditions);
            mesh.updateModelBound();
            _root.attachChild(mesh);
            _totalMeshes++;
            _plyFaceInfoList = null;
        }
    }

    void cleanup() {
        _plyFaceInfoList = null;
        _plyEdgeInfoList = null;
    }
}
