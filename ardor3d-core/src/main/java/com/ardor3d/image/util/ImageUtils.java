/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;

public abstract class ImageUtils {

    public static final int getPixelByteSize(final ImageDataFormat format, final PixelDataType type) {
        return type.getBytesPerPixel(format.getComponents());
    }

    public static final TextureStoreFormat getTextureStoreFormat(final TextureStoreFormat format, final Image image) {
        if (format != TextureStoreFormat.GuessCompressedFormat
                && format != TextureStoreFormat.GuessNoCompressedFormat) {
            return format;
        }
        if (image == null) {
            throw new Error("Unable to guess format type... Image is null.");
        }

        final PixelDataType type = image.getDataType();
        final ImageDataFormat dataFormat = image.getDataFormat();
        switch (dataFormat) {
            case ColorIndex:
            case BGRA:
            case RGBA:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRGBA;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.RGBA8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.RGBA16;
                    case HalfFloat:
                        return TextureStoreFormat.RGBA16F;
                    case Float:
                        return TextureStoreFormat.RGBA32F;
                }
                break;
            case BGR:
            case RGB:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRGB;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.RGB8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.RGB16;
                    case HalfFloat:
                        return TextureStoreFormat.RGB16F;
                    case Float:
                        return TextureStoreFormat.RGB32F;
                }
                break;
            case RG:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRG;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.RG8;
                    case Short:
                    case UnsignedShort:
                        return TextureStoreFormat.RG16;
                    case Int:
                        return TextureStoreFormat.RG16I;
                    case UnsignedInt:
                        return TextureStoreFormat.RG16UI;
                    case HalfFloat:
                        return TextureStoreFormat.RG16F;
                    case Float:
                        return TextureStoreFormat.RG32F;
                }
                break;
            case Luminance:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedLuminance;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Luminance8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Luminance16;
                    case HalfFloat:
                        return TextureStoreFormat.Luminance16F;
                    case Float:
                        return TextureStoreFormat.Luminance32F;
                }
                break;
            case LuminanceAlpha:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedLuminanceAlpha;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Luminance4Alpha4;
                    case Short:
                    case UnsignedShort:
                        return TextureStoreFormat.Luminance8Alpha8;
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Luminance16Alpha16;
                    case HalfFloat:
                        return TextureStoreFormat.LuminanceAlpha16F;
                    case Float:
                        return TextureStoreFormat.LuminanceAlpha32F;
                }
                break;
            case Alpha:
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Alpha8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Alpha16;
                    case HalfFloat:
                        return TextureStoreFormat.Alpha16F;
                    case Float:
                        return TextureStoreFormat.Alpha32F;
                }
                break;
            case Red:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRed;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.R8;
                    case Short:
                    case UnsignedShort:
                        return TextureStoreFormat.R16;
                    case Int:
                        return TextureStoreFormat.R16I;
                    case UnsignedInt:
                        return TextureStoreFormat.R16UI;
                    case HalfFloat:
                        return TextureStoreFormat.R16F;
                    case Float:
                        return TextureStoreFormat.R32F;
                }
                break;
            case Intensity:
            case Green:
            case Blue:
            case StencilIndex:
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Intensity8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Intensity16;
                    case HalfFloat:
                        return TextureStoreFormat.Intensity16F;
                    case Float:
                        return TextureStoreFormat.Intensity32F;
                }
                break;
            case Depth:
                // XXX: Should we actually switch here? Depth textures can be slightly fussy.
                return TextureStoreFormat.Depth;
            case PrecompressedDXT1:
                return TextureStoreFormat.NativeDXT1;
            case PrecompressedDXT1A:
                return TextureStoreFormat.NativeDXT1A;
            case PrecompressedDXT3:
                return TextureStoreFormat.NativeDXT3;
            case PrecompressedDXT5:
                return TextureStoreFormat.NativeDXT5;
            case PrecompressedLATC_L:
                return TextureStoreFormat.NativeLATC_L;
            case PrecompressedLATC_LA:
                return TextureStoreFormat.NativeLATC_LA;
        }

        throw new Error("Unhandled type / format combination: " + type + " / " + dataFormat);
    }

    public static ColorRGBA getRGBA(final Image img, final int x, final int y, final ColorRGBA store) {
        return getRGBA(img, 0, x, y, store);
    }

    public static ColorRGBA getRGBA(final Image img, final int index, final int x, final int y, final ColorRGBA store) {
        final ColorRGBA result = store == null ? new ColorRGBA() : store;
        final int rgba = getRGBA(img, index, x, y);
        return (result.fromIntRGBA(rgba));
    }

    public static int getARGB(final Image img, final int x, final int y) {
        return getARGB(img, 0, x, y);
    }

    public static int getARGB(final Image img, final int index, final int x, final int y) {
        final ByteBuffer imgData = img.getData(index);
        final int bytesPerPixel = ImageUtils.getPixelByteSize(img.getDataFormat(), img.getDataType());
        final int dataIndex = bytesPerPixel * (x + (y * img.getWidth()));
        final int argb;
        switch (img.getDataFormat()) {
            case Alpha:
                argb = ((imgData.get(dataIndex) & 0xFF) << 24);
                break;
            case Red:
                argb = (0xFF << 24) | ((imgData.get(dataIndex) & 0xFF) << 16);
                break;
            case Green:
                argb = (0xFF << 24) | ((imgData.get(dataIndex) & 0xFF) << 8);
                break;
            case Blue:
                argb = (0xFF << 24) | (imgData.get(dataIndex) & 0xFF);
                break;
            case RG:
                argb = (0xFF << 24) | ((imgData.get(dataIndex) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 1) & 0xFF) << 8) | (0x00);
                break;
            case RGB:
                argb = (0xFF << 24) | ((imgData.get(dataIndex) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 1) & 0xFF) << 8) | (imgData.get(dataIndex + 2) & 0xFF);
                break;
            case BGR:
                argb = (0xFF << 24) | ((imgData.get(dataIndex + 2) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 1) & 0xFF) << 8) | (imgData.get(dataIndex) & 0xFF);
                break;
            case RGBA:
                argb = ((imgData.get(dataIndex + 3) & 0xFF) << 24) | ((imgData.get(dataIndex) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 1) & 0xFF) << 8) | (imgData.get(dataIndex + 2) & 0xFF);
                break;
            case BGRA:
                argb = ((imgData.get(dataIndex + 3) & 0xFF) << 24) | ((imgData.get(dataIndex + 2) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 1) & 0xFF) << 8) | (imgData.get(dataIndex) & 0xFF);
                break;
            default:
                throw new UnsupportedOperationException("Image data format " + img.getDataFormat() + " not supported!");
        }
        return (argb);
    }

    public static int getRGBA(final Image img, final int x, final int y) {
        return getRGBA(img, 0, x, y);
    }

    public static int getRGBA(final Image img, final int index, final int x, final int y) {
        final ByteBuffer imgData = img.getData(index);
        final int bytesPerPixel = ImageUtils.getPixelByteSize(img.getDataFormat(), img.getDataType());
        final int dataIndex = bytesPerPixel * (x + (y * img.getWidth()));
        final int rgba;
        switch (img.getDataFormat()) {
            case Alpha:
                rgba = (imgData.get(dataIndex) & 0xFF);
                break;
            case Red:
                rgba = (0xFF << 24) | ((imgData.get(dataIndex) & 0xFF) << 24);
                break;
            case Green:
                rgba = (0xFF << 24) | ((imgData.get(dataIndex) & 0xFF) << 16);
                break;
            case Blue:
                rgba = (0xFF << 24) | (imgData.get(dataIndex) & 0xFF << 8);
                break;
            case RG:
                rgba = ((imgData.get(dataIndex) & 0xFF) << 24) | ((imgData.get(dataIndex + 1) & 0xFF) << 16)
                        | (0x00 << 8) | (0xFF);
                break;
            case RGB:
                rgba = ((imgData.get(dataIndex) & 0xFF) << 24) | ((imgData.get(dataIndex + 1) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 2) & 0xFF) << 8) | (0xFF);
                break;
            case BGR:
                rgba = ((imgData.get(dataIndex + 2) & 0xFF) << 24) | ((imgData.get(dataIndex + 1) & 0xFF) << 16)
                        | ((imgData.get(dataIndex) & 0xFF) << 8) | (0xFF);
                break;
            case RGBA:
                rgba = ((imgData.get(dataIndex) & 0xFF) << 24) | ((imgData.get(dataIndex + 1) & 0xFF) << 16)
                        | ((imgData.get(dataIndex + 2) & 0xFF) << 8) | (imgData.get(dataIndex + 3) & 0xFF);
                break;
            case BGRA:
                rgba = ((imgData.get(dataIndex + 2) & 0xFF) << 24) | ((imgData.get(dataIndex + 1) & 0xFF) << 16)
                        | ((imgData.get(dataIndex) & 0xFF) << 8) | (imgData.get(dataIndex + 3) & 0xFF);
                break;
            default:
                throw new UnsupportedOperationException("Image data format " + img.getDataFormat() + " not supported!");
        }
        return (rgba);
    }

    public static void setARGB(final Image img, final int x, final int y, final int argb) {
        setARGB(img, 0, x, y, argb);
    }

    public static void setARGB(final Image img, final int index, final int x, final int y, final int argb) {
        final ByteBuffer imgData = img.getData(index);
        final int bytesPerPixel = ImageUtils.getPixelByteSize(img.getDataFormat(), img.getDataType());
        final int dataIndex = bytesPerPixel * (x + (y * img.getWidth()));
        switch (img.getDataFormat()) {
            case Alpha:
                imgData.put(dataIndex, (byte) ((argb >> 24) & 0xFF));
                break;
            case Red:
                imgData.put(dataIndex, (byte) ((argb >> 16) & 0xFF));
                break;
            case Green:
                imgData.put(dataIndex, (byte) ((argb >> 8) & 0xFF));
                break;
            case Blue:
                imgData.put(dataIndex, (byte) (argb & 0xFF));
                break;
            case RG:
                imgData.put(dataIndex, (byte) ((argb >> 16) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((argb >> 8) & 0xFF));
                break;
            case RGB:
                imgData.put(dataIndex, (byte) ((argb >> 16) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((argb >> 8) & 0xFF));
                imgData.put(dataIndex + 2, (byte) (argb & 0xFF));
                break;
            case BGR:
                imgData.put(dataIndex + 2, (byte) ((argb >> 16) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((argb >> 8) & 0xFF));
                imgData.put(dataIndex, (byte) (argb & 0xFF));
                break;
            case RGBA:
                imgData.put(dataIndex, (byte) ((argb >> 16) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((argb >> 8) & 0xFF));
                imgData.put(dataIndex + 2, (byte) (argb & 0xFF));
                imgData.put(dataIndex + 3, (byte) ((argb >> 24) & 0xFF));
                break;
            case BGRA:
                imgData.put(dataIndex + 2, (byte) ((argb >> 16) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((argb >> 8) & 0xFF));
                imgData.put(dataIndex, (byte) (argb & 0xFF));
                imgData.put(dataIndex + 3, (byte) ((argb >> 24) & 0xFF));
                break;
            default:
                throw new UnsupportedOperationException("Image data format " + img.getDataFormat() + " not supported!");
        }
    }

    public static void setRGBA(final Image img, final int x, final int y, final ColorRGBA color) {
        setRGBA(img, 0, x, y, color);
    }

    public static void setRGBA(final Image img, final int index, final int x, final int y, final ColorRGBA color) {
        final int rgba = color.asIntRGBA();
        setRGBA(img, index, x, y, rgba);
    }

    public static void setRGBA(final Image img, final int x, final int y, final int rgba) {
        setRGBA(img, 0, x, y, rgba);
    }

    public static void setRGBA(final Image img, final int index, final int x, final int y, final int rgba) {
        final ByteBuffer imgData = img.getData(index);
        final int bytesPerPixel = ImageUtils.getPixelByteSize(img.getDataFormat(), img.getDataType());
        final int dataIndex = bytesPerPixel * (x + (y * img.getWidth()));
        switch (img.getDataFormat()) {
            case Alpha:
                imgData.put(dataIndex, (byte) ((rgba) & 0xFF));
                break;
            case Red:
                imgData.put(dataIndex, (byte) ((rgba >> 24) & 0xFF));
                break;
            case Green:
                imgData.put(dataIndex, (byte) ((rgba >> 16) & 0xFF));
                break;
            case Blue:
                imgData.put(dataIndex, (byte) ((rgba >> 8) & 0xFF));
                break;
            case RG:
                imgData.put(dataIndex, (byte) ((rgba >> 24) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((rgba >> 16) & 0xFF));
                break;
            case RGB:
                imgData.put(dataIndex, (byte) ((rgba >> 24) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((rgba >> 16) & 0xFF));
                imgData.put(dataIndex + 2, (byte) ((rgba >> 8) & 0xFF));
                break;
            case BGR:
                imgData.put(dataIndex + 2, (byte) ((rgba >> 24) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((rgba >> 16) & 0xFF));
                imgData.put(dataIndex, (byte) ((rgba >> 8) & 0xFF));
                break;
            case RGBA:
                imgData.put(dataIndex, (byte) ((rgba >> 24) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((rgba >> 16) & 0xFF));
                imgData.put(dataIndex + 2, (byte) ((rgba >> 8) & 0xFF));
                imgData.put(dataIndex + 3, (byte) (rgba & 0xFF));
                break;
            case BGRA:
                imgData.put(dataIndex + 2, (byte) ((rgba >> 24) & 0xFF));
                imgData.put(dataIndex + 1, (byte) ((rgba >> 16) & 0xFF));
                imgData.put(dataIndex, (byte) ((rgba >> 8) & 0xFF));
                imgData.put(dataIndex + 3, (byte) (rgba & 0xFF));
                break;
            default:
                throw new UnsupportedOperationException("Image data format " + img.getDataFormat() + " not supported!");
        }
    }
}
