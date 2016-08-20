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

import java.util.ArrayList;
import java.util.List;

public class PlyFaceInfo {

    private List<Integer> _vertexIndices;

    private List<Integer> _materialIndices;

    private List<Float> _textureCoordinates;

    public PlyFaceInfo() {
        super();
    }

    public void addVertexIndex(final int vertexIndex) {
        if (_vertexIndices == null) {
            _vertexIndices = new ArrayList<>();
        }
        _vertexIndices.add(Integer.valueOf(vertexIndex));
    }

    public List<Integer> getVertexIndices() {
        return _vertexIndices;
    }

    public void addMaterialIndex(final int materialIndex) {
        if (_materialIndices == null) {
            _materialIndices = new ArrayList<>();
        }
        _materialIndices.add(Integer.valueOf(materialIndex));
    }

    public List<Integer> getMaterialIndices() {
        return _materialIndices;
    }

    public void addTextureCoordinate(final float textureCoordinate) {
        if (_textureCoordinates == null) {
            _textureCoordinates = new ArrayList<>();
        }
        _textureCoordinates.add(Float.valueOf(textureCoordinate));
    }

    public List<Float> getTextureCoordinates() {
        return _textureCoordinates;
    }
}
