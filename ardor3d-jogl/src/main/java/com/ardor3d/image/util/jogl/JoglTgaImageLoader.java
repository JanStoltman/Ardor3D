/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util.jogl;

import java.io.IOException;
import java.io.InputStream;

import com.ardor3d.image.Image;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Ardor3D loader using the build-in JOGL TGA loader. As this format has no magic number, it cannot use the detection
 * mechanism implemented in JoglImageLoader
 */
public class JoglTgaImageLoader extends JoglImageLoader {

    private static final String[] _supportedFormats = new String[] { "." + TextureIO.TGA.toUpperCase() };

    @Override
    public Image load(final InputStream is, final boolean verticalFlipNeeded) throws IOException {
        final TextureData textureData = TextureIO.newTextureData(_capsUtil.getProfile(), is,
                isMipmapsProductionEnabled(), TextureIO.TGA);
        if (textureData == null) {
            return null;
        }
        return makeArdor3dImage(textureData, textureData.getMustFlipVertically() == verticalFlipNeeded);
    }

    public static String[] getSupportedFormats() {
        return _supportedFormats;
    }

    public static void registerLoader() {
        registerLoader(new JoglTgaImageLoader(), _supportedFormats);
    }

    public static void registerLoader(final JoglTgaImageLoader joglTgaImageLoader, final String[] supportedFormats) {
        ImageLoaderUtil.registerHandler(joglTgaImageLoader, supportedFormats);
    }
}
