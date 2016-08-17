/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.util;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;

/**
 * common material parameters
 */
public abstract class AbstractMaterial {

    private float ambientRed, ambientGreen, ambientBlue, ambientAlpha;

    private float diffuseRed, diffuseGreen, diffuseBlue, diffuseAlpha;

    private float emissiveRed, emissiveGreen, emissiveBlue, emissiveAlpha;

    private float specularRed, specularGreen, specularBlue, specularAlpha;

    private float shininess;

    private boolean forceBlend;

    protected AbstractMaterial() {
        super();
        ambientRed = -1;
        ambientGreen = -1;
        ambientBlue = -1;
        ambientAlpha = -1;
        diffuseRed = -1;
        diffuseGreen = -1;
        diffuseBlue = -1;
        diffuseAlpha = -1;
        emissiveRed = -1;
        emissiveGreen = -1;
        emissiveBlue = -1;
        emissiveAlpha = -1;
        specularRed = -1;
        specularGreen = -1;
        specularBlue = -1;
        specularAlpha = -1;
        shininess = -1;
    }

    public BlendState getBlendState() {
        if (forceBlend || (ambientAlpha != -1 && ambientAlpha < 1.0f) || (diffuseAlpha != -1 && diffuseAlpha < 1.0f)
                || (emissiveAlpha != -1 && emissiveAlpha < 1.0f) || (specularAlpha != -1 && specularAlpha < 1.0f)) {
            final BlendState blend = new BlendState();
            blend.setBlendEnabled(true);
            blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            blend.setTestEnabled(true);
            blend.setTestFunction(BlendState.TestFunction.GreaterThan);
            blend.setReference(0);
            return blend;
        }
        return null;
    }

    public MaterialState getMaterialState() {
        if ((ambientRed != -1 && ambientGreen != -1 && ambientBlue != -1)
                || (diffuseRed != -1 && diffuseGreen != -1 && diffuseBlue != -1)
                || (emissiveRed != -1 && emissiveGreen != -1 && emissiveBlue != -1)
                || (specularRed != -1 && specularGreen != -1 && specularBlue != -1) || shininess != -1) {
            final MaterialState material = new MaterialState();
            if (ambientRed != -1 && ambientGreen != -1 && ambientBlue != -1) {
                final float alpha = ambientAlpha == -1 ? 1 : MathUtils.clamp(ambientAlpha, 0, 1);
                material.setAmbient(new ColorRGBA(ambientRed, ambientGreen, ambientBlue, alpha));
            }
            if (diffuseRed != -1 && diffuseGreen != -1 && diffuseBlue != -1) {
                final float alpha = diffuseAlpha == -1 ? 1 : MathUtils.clamp(diffuseAlpha, 0, 1);
                material.setDiffuse(new ColorRGBA(diffuseRed, diffuseGreen, diffuseBlue, alpha));
            }
            if (emissiveRed != -1 && emissiveGreen != -1 && emissiveBlue != -1) {
                final float alpha = emissiveAlpha == -1 ? 1 : MathUtils.clamp(emissiveAlpha, 0, 1);
                material.setEmissive(new ColorRGBA(emissiveRed, emissiveGreen, emissiveBlue, alpha));
            }
            if (specularRed != -1 && specularGreen != -1 && specularBlue != -1) {
                final float alpha = specularAlpha == -1 ? 1 : MathUtils.clamp(specularAlpha, 0, 1);
                material.setSpecular(new ColorRGBA(specularRed, specularGreen, specularBlue, alpha));
            }
            if (shininess != -1) {
                material.setShininess(shininess);
            }
            return material;
        }
        return null;
    }

    public float getAmbientRed() {
        return ambientRed;
    }

    public void setAmbientRed(final float ambientRed) {
        this.ambientRed = ambientRed;
    }

    public float getAmbientGreen() {
        return ambientGreen;
    }

    public void setAmbientGreen(final float ambientGreen) {
        this.ambientGreen = ambientGreen;
    }

    public float getAmbientBlue() {
        return ambientBlue;
    }

    public void setAmbientBlue(final float ambientBlue) {
        this.ambientBlue = ambientBlue;
    }

    public float getDiffuseRed() {
        return diffuseRed;
    }

    public void setDiffuseRed(final float diffuseRed) {
        this.diffuseRed = diffuseRed;
    }

    public float getDiffuseGreen() {
        return diffuseGreen;
    }

    public void setDiffuseGreen(final float diffuseGreen) {
        this.diffuseGreen = diffuseGreen;
    }

    public float getDiffuseBlue() {
        return diffuseBlue;
    }

    public void setDiffuseBlue(final float diffuseBlue) {
        this.diffuseBlue = diffuseBlue;
    }

    public float getEmissiveRed() {
        return emissiveRed;
    }

    public void setEmissiveRed(final float emissiveRed) {
        this.emissiveRed = emissiveRed;
    }

    public float getEmissiveGreen() {
        return emissiveGreen;
    }

    public void setEmissiveGreen(final float emissiveGreen) {
        this.emissiveGreen = emissiveGreen;
    }

    public float getEmissiveBlue() {
        return emissiveBlue;
    }

    public void setEmissiveBlue(final float emissiveBlue) {
        this.emissiveBlue = emissiveBlue;
    }

    public float getSpecularRed() {
        return specularRed;
    }

    public void setSpecularRed(final float specularRed) {
        this.specularRed = specularRed;
    }

    public float getSpecularGreen() {
        return specularGreen;
    }

    public void setSpecularGreen(final float specularGreen) {
        this.specularGreen = specularGreen;
    }

    public float getSpecularBlue() {
        return specularBlue;
    }

    public void setSpecularBlue(final float specularBlue) {
        this.specularBlue = specularBlue;
    }

    public float getAmbientAlpha() {
        return ambientAlpha;
    }

    public void setAmbientAlpha(final float ambientAlpha) {
        this.ambientAlpha = ambientAlpha;
    }

    public float getDiffuseAlpha() {
        return diffuseAlpha;
    }

    public void setDiffuseAlpha(final float diffuseAlpha) {
        this.diffuseAlpha = diffuseAlpha;
    }

    public float getEmissiveAlpha() {
        return emissiveAlpha;
    }

    public void setEmissiveAlpha(final float emissiveAlpha) {
        this.emissiveAlpha = emissiveAlpha;
    }

    public float getSpecularAlpha() {
        return specularAlpha;
    }

    public void setSpecularAlpha(final float specularAlpha) {
        this.specularAlpha = specularAlpha;
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(final float shininess) {
        this.shininess = shininess;
    }

    public boolean isForceBlend() {
        return forceBlend;
    }

    public void setForceBlend(final boolean forceBlend) {
        this.forceBlend = forceBlend;
    }
}
