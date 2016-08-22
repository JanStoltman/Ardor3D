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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;

public class PlyGeometryStore {

    private static final Logger LOGGER = Logger.getLogger(PlyGeometryStore.class.getName());

    private int _totalMeshes = 0;

    private int _totalLines = 0;

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
            final String name = "ply_line" + _totalLines;
            boolean hasColors = false;
            final boolean hasNormals = _dataStore.getNormals() != null && !_dataStore.getNormals().isEmpty();
            final int vertexCount = _plyEdgeInfoList.size() * 2;
            final Vector3[] vertices = new Vector3[vertexCount];
            final Vector3[] normals = hasNormals ? null : new Vector3[vertexCount];
            ReadOnlyColorRGBA[] colors = null;
            final IndexBufferData<? extends Buffer> indices = BufferUtils.createIndexBufferData(vertexCount,
                    vertexCount - 1);
            int edgeVertexIndex = 0;
            for (final PlyEdgeInfo plyEdgeInfo : _plyEdgeInfoList) {
                indices.put(edgeVertexIndex).put(edgeVertexIndex + 1);
                vertices[edgeVertexIndex] = _dataStore.getVertices().get(plyEdgeInfo.getIndex1());
                vertices[edgeVertexIndex + 1] = _dataStore.getVertices().get(plyEdgeInfo.getIndex2());
                if (hasNormals) {
                    normals[edgeVertexIndex] = _dataStore.getNormals().get(plyEdgeInfo.getIndex1());
                    normals[edgeVertexIndex + 1] = _dataStore.getNormals().get(plyEdgeInfo.getIndex2());
                }
                if (plyEdgeInfo.getColor() != null) {
                    if (colors == null) {
                        colors = new ReadOnlyColorRGBA[vertexCount];
                    }
                    colors[edgeVertexIndex] = plyEdgeInfo.getColor();
                    colors[edgeVertexIndex + 1] = plyEdgeInfo.getColor();
                    hasColors = true;
                }
                edgeVertexIndex += 2;
            }
            final Line line = new Line(name, vertices, normals, colors, null);
            indices.rewind();
            line.getMeshData().setIndices(indices);
            final EnumSet<MatchCondition> matchConditions = EnumSet.noneOf(MatchCondition.class);
            if (hasNormals) {
                matchConditions.add(MatchCondition.Normal);
            }
            if (hasColors) {
                matchConditions.add(MatchCondition.Color);
            }
            _geometryTool.minimizeVerts(line, matchConditions);

            final TextureState tState = getTextureState();
            if (tState != null) {
                line.setRenderState(tState);
            }

            line.updateModelBound();
            _totalLines++;
            _plyEdgeInfoList = null;
        }
        if (_plyFaceInfoList != null) {
            final String name = "ply_mesh" + _totalMeshes;
            final Mesh mesh = new Mesh(name);
            boolean hasTexCoordsInFaces = false;
            final boolean hasTexCoordsInVertices = _dataStore.getTextureCoordinates() != null
                    && !_dataStore.getTextureCoordinates().isEmpty();
            final boolean hasNormals = _dataStore.getNormals() != null && !_dataStore.getNormals().isEmpty();
            final boolean hasColors = _dataStore.getColors() != null && !_dataStore.getColors().isEmpty();
            int vertexCount = 0;
            for (final PlyFaceInfo plyFaceInfo : _plyFaceInfoList) {
                vertexCount += plyFaceInfo.getVertexIndices().size();
                if (plyFaceInfo.getTextureCoordinates() != null && !plyFaceInfo.getTextureCoordinates().isEmpty()) {
                    hasTexCoordsInFaces = true;
                }
            }
            final FloatBuffer vertices = BufferUtils.createVector3Buffer(vertexCount);
            final IndexBufferData<? extends Buffer> indices = BufferUtils.createIndexBufferData(vertexCount,
                    vertexCount - 1);

            final FloatBuffer normals = hasNormals ? BufferUtils.createFloatBuffer(vertices.capacity()) : null;
            final FloatBuffer colors = hasColors ? BufferUtils.createFloatBuffer(vertexCount * 4) : null;
            final FloatBuffer uvs = hasTexCoordsInFaces || hasTexCoordsInVertices
                    ? BufferUtils.createFloatBuffer(vertexCount * 2) : null;

            int dummyVertexIndex = 0;
            final List<IndexMode> indexModeList = new ArrayList<>();
            final List<Integer> indexLengthList = new ArrayList<>();
            for (final PlyFaceInfo plyFaceInfo : _plyFaceInfoList) {
                final IndexMode previousIndexMode = indexModeList.isEmpty() ? null
                        : indexModeList.get(indexModeList.size() - 1);
                final IndexMode currentIndexMode;
                switch (plyFaceInfo.getVertexIndices().size()) {
                    case 3: {
                        currentIndexMode = IndexMode.Triangles;
                        break;
                    }
                    case 4: {
                        currentIndexMode = IndexMode.Quads;
                        break;
                    }
                    default: {
                        currentIndexMode = null;
                        break;
                    }
                }
                if (currentIndexMode == null) {
                    PlyGeometryStore.LOGGER.log(Level.SEVERE,
                            "The index mode cannot be determined for a face containing "
                                    + plyFaceInfo.getVertexIndices().size() + " vertices");
                } else {
                    if (previousIndexMode == null || currentIndexMode != previousIndexMode) {
                        indexModeList.add(currentIndexMode);
                        indexLengthList.add(currentIndexMode.getVertexCount());
                    } else {
                        final int previousIndexLength = indexLengthList.get(indexLengthList.size() - 1).intValue();
                        final int currentIndexLength = previousIndexLength + currentIndexMode.getVertexCount();
                        indexLengthList.set(indexLengthList.size() - 1, Integer.valueOf(currentIndexLength));
                    }
                    for (final Integer vertexIndex : plyFaceInfo.getVertexIndices()) {
                        indices.put(dummyVertexIndex);
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
                        if (hasTexCoordsInVertices) {
                            final Vector2 texCoords = _dataStore.getTextureCoordinates().get(vertexIndex.intValue());
                            uvs.put(texCoords.getXf()).put(texCoords.getYf());
                        }
                        dummyVertexIndex++;
                    }
                    if (hasTexCoordsInFaces) {
                        for (final Float texCoord : plyFaceInfo.getTextureCoordinates()) {
                            uvs.put(texCoord);
                        }
                    }
                }
            }

            vertices.rewind();
            mesh.getMeshData().setVertexBuffer(vertices);
            indices.rewind();
            mesh.getMeshData().setIndices(indices);
            if (indexModeList.size() == 1) {
                mesh.getMeshData().setIndexMode(indexModeList.get(0));
                mesh.getMeshData().setIndexLengths(null);
            } else {
                mesh.getMeshData().setIndexModes(indexModeList.toArray(new IndexMode[indexModeList.size()]));
                final int[] indexLengths = new int[indexLengthList.size()];
                for (int indexLengthIndex = 0; indexLengthIndex < indexLengths.length; indexLengthIndex++) {
                    indexLengths[indexLengthIndex] = indexLengthList.get(indexLengthIndex).intValue();
                }
                mesh.getMeshData().setIndexLengths(indexLengths);
            }
            final EnumSet<MatchCondition> matchConditions = EnumSet.noneOf(MatchCondition.class);
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
            if (hasTexCoordsInFaces || hasTexCoordsInVertices) {
                uvs.rewind();
                mesh.getMeshData().setTextureBuffer(uvs, 0);
                matchConditions.add(MatchCondition.UVs);
            }

            if (indexModeList.size() == 1) {
                _geometryTool.minimizeVerts(mesh, matchConditions);
            } else {
                // FIXME unsure about minimizeVerts preserving the index modes
            }

            final TextureState tState = getTextureState();
            if (tState != null) {
                mesh.setRenderState(tState);
            }

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
