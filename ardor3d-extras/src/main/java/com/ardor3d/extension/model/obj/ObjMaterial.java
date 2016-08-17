/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.obj;

import com.ardor3d.extension.model.util.AbstractMaterial;
import com.ardor3d.image.Texture;
import com.ardor3d.renderer.state.TextureState;

/**
 * WaveFront OBJ material (MTL). <code>Ns</code> matches with the shininess, <code>d</code> matches with the alpha
 * component(s), <code>Ka</code> matches with the ambient RGB components, <code>Kd</code> matches with the diffuse RGB
 * components, <code>Ks</code> matches with the specular RGB components.
 */
public class ObjMaterial extends AbstractMaterial {
    private final String name;

    private String textureName;

    private Texture map_Kd;

    private int illumType;

    public ObjMaterial(final String name) {
        super();
        this.name = name;
        illumType = 2;
    }

    public TextureState getTextureState() {
        if (map_Kd != null) {
            final TextureState tState = new TextureState();
            tState.setTexture(map_Kd, 0);
            return tState;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getTextureName() {
        return textureName;
    }

    public void setTextureName(final String textureName) {
        this.textureName = textureName;
    }

    public Texture getMap_Kd() {
        return map_Kd;
    }

    public void setMap_Kd(final Texture map_Kd) {
        this.map_Kd = map_Kd;
    }

    public int getIllumType() {
        return illumType;
    }

    public void setIllumType(final int illumType) {
        this.illumType = illumType;
    }
}
