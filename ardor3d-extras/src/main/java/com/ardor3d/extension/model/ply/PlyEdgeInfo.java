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

import com.ardor3d.math.ColorRGBA;

public class PlyEdgeInfo {

    private Integer index1;

    private Integer index2;

    private ColorRGBA color;

    public PlyEdgeInfo() {
        super();
    }

    public Integer getIndex1() {
        return index1;
    }

    public void setIndex1(final Integer index1) {
        this.index1 = index1;
    }

    public Integer getIndex2() {
        return index2;
    }

    public void setIndex2(final Integer index2) {
        this.index2 = index2;
    }

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(final ColorRGBA color) {
        this.color = color;
    }
}
