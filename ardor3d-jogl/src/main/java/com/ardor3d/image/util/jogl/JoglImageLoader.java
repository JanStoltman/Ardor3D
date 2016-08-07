/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.framework.jogl.CapsUtil;
import com.ardor3d.image.Image;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageLoader;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.util.geom.BufferUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class JoglImageLoader implements ImageLoader {

    public static boolean createOnHeap = false;

    protected final CapsUtil _capsUtil;

    /**
     * Flag indicating whether the mipmaps are produced by JOGL (retrieved from the file or generated)
     */
    private boolean _mipmapsProductionEnabled;

    private enum TYPE {
        BYTE(ByteBuffer.class), SHORT(ShortBuffer.class), CHAR(CharBuffer.class), INT(IntBuffer.class), FLOAT(
                FloatBuffer.class), LONG(LongBuffer.class), DOUBLE(DoubleBuffer.class);

        private final Class<? extends Buffer> bufferClass;

        private TYPE(final Class<? extends Buffer> bufferClass) {
            this.bufferClass = bufferClass;
        }
    };

    private static final String[] _supportedFormats = computeSupportedFormats();

    private static final String[] computeSupportedFormats() {
        final List<String> supportedFormatsList = new ArrayList<>();
        if (Platform.AWT_AVAILABLE) {
            supportedFormatsList.add("." + TextureIO.GIF.toUpperCase());
        }
        supportedFormatsList.add("." + TextureIO.DDS.toUpperCase());
        supportedFormatsList.add("." + TextureIO.JPG.toUpperCase());
        supportedFormatsList.add("." + TextureIO.PNG.toUpperCase());
        supportedFormatsList.add("." + TextureIO.SGI.toUpperCase());
        supportedFormatsList.add("." + TextureIO.SGI_RGB.toUpperCase());
        return supportedFormatsList.toArray(new String[supportedFormatsList.size()]);
    }

    public static String[] getSupportedFormats() {
        return _supportedFormats;
    }

    public static void registerLoader() {
        registerLoader(new JoglImageLoader(), _supportedFormats);
        registerLoader(new JoglTgaImageLoader(), JoglTgaImageLoader.getSupportedFormats());
    }

    public static void registerLoader(final JoglImageLoader joglImageLoader, final String[] supportedFormats) {
        ImageLoaderUtil.registerHandler(joglImageLoader, supportedFormats);
    }

    public JoglImageLoader() {
        this(new CapsUtil());
    }

    public JoglImageLoader(final CapsUtil capsUtil) {
        _capsUtil = capsUtil;
    }

    public Image makeArdor3dImage(final TextureData textureData, final boolean verticalFlipNeeded) {
        final Buffer textureDataBuffer = textureData.getBuffer();
        final Image ardorImage = new Image();
        TYPE bufferDataType = getBufferDataType(textureDataBuffer);
        if (bufferDataType == null) {
            throw new UnsupportedOperationException("Unknown buffer type " + textureDataBuffer.getClass().getName());
        } else {
            int dataSizeInBytes = textureDataBuffer.capacity() * Buffers.sizeOfBufferElem(textureDataBuffer);
            ByteBuffer scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(dataSizeInBytes) : Buffers
                    .newDirectByteBuffer(dataSizeInBytes);
            if (verticalFlipNeeded) {
                flipImageData(textureDataBuffer, scratch, dataSizeInBytes, bufferDataType, textureData.getWidth(),
                        textureData.getHeight());
            } else {
                copyImageData(textureDataBuffer, scratch, bufferDataType);
            }
            ardorImage.setWidth(textureData.getWidth());
            ardorImage.setHeight(textureData.getHeight());
            ardorImage.setData(scratch);
            ardorImage.setDataFormat(JoglTextureUtil.getImageDataFormat(textureData.getPixelFormat()));
            /**
             * A ByteBuffer is always used to store the image data, otherwise we should call
             * JoglTextureUtil.getPixelDataType(textureData.getPixelType())
             */
            ardorImage.setDataType(PixelDataType.UnsignedByte);
            if (textureData.getMipmapData() != null) {
                for (final Buffer mipmapData : textureData.getMipmapData()) {
                    dataSizeInBytes = mipmapData.capacity() * Buffers.sizeOfBufferElem(mipmapData);
                    scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(dataSizeInBytes) : Buffers
                            .newDirectByteBuffer(dataSizeInBytes);
                    bufferDataType = getBufferDataType(mipmapData);
                    if (verticalFlipNeeded) {
                        flipImageData(mipmapData, scratch, dataSizeInBytes, bufferDataType, textureData.getWidth(),
                                textureData.getHeight());
                    } else {
                        copyImageData(mipmapData, scratch, bufferDataType);
                    }
                    ardorImage.addData(scratch);
                }
                final int[] mipMapSizes = new int[ardorImage.getDataSize()];
                int imageDataIndex = 0;
                for (final Buffer imageData : ardorImage.getData()) {
                    mipMapSizes[imageDataIndex] = imageData.capacity();
                    imageDataIndex++;
                }
                ardorImage.setMipMapByteSizes(mipMapSizes);
            }
            return ardorImage;
        }
    }

    @Override
    public Image load(final InputStream is, final boolean verticalFlipNeeded) throws IOException {
        final TextureData textureData = TextureIO.newTextureData(_capsUtil.getProfile(), is, _mipmapsProductionEnabled,
                null /* JOGL >= 2.3.2 is able to guess the file suffix */);
        if (textureData == null) {
            return null;
        }
        return makeArdor3dImage(textureData, textureData.getMustFlipVertically() == verticalFlipNeeded);
    }

    private TYPE getBufferDataType(final Buffer buffer) {
        TYPE bufferDataType = null;
        for (final TYPE type : TYPE.values()) {
            if (type.bufferClass.isAssignableFrom(buffer.getClass())) {
                bufferDataType = type;
                break;
            }
        }
        return bufferDataType;
    }

    protected void copyImageData(final Buffer src, final ByteBuffer dest, final TYPE bufferDataType) {
        final int srcPos = src.position();
        final int destPos = dest.position();
        switch (bufferDataType) {
            case BYTE:
                dest.put((ByteBuffer) src);
                break;
            case SHORT:
                dest.asShortBuffer().put((ShortBuffer) src);
                break;
            case CHAR:
                dest.asCharBuffer().put((CharBuffer) src);
                break;
            case INT:
                dest.asIntBuffer().put((IntBuffer) src);
                break;
            case FLOAT:
                dest.asFloatBuffer().put((FloatBuffer) src);
            case LONG:
                dest.asLongBuffer().put((LongBuffer) src);
                break;
            case DOUBLE:
                dest.asDoubleBuffer().put((DoubleBuffer) src);
                break;
            default:
                // it should never happen
        }
        src.position(srcPos);
        dest.position(destPos);
    }

    protected void flipImageData(final Buffer src, final ByteBuffer dest, final int dataSizeInBytes,
            final TYPE bufferDataType, final int width, final int height) {
        final int srcPos = src.position();
        final int destPos = dest.position();
        final int bytesPerPixel = dataSizeInBytes / (width * height);
        final int bytesPerElement = Buffers.sizeOfBufferElem(src);
        final int elementsPerPixel = bytesPerPixel / bytesPerElement;
        final int elementsPerLine = width * elementsPerPixel;
        final int bytesPerLine = bytesPerPixel * width;// width = pixels per line
        byte[] byteBuf = null;
        short[] shortBuf = null;
        char[] charBuf = null;
        int[] intBuf = null;
        float[] floatBuf = null;
        long[] longBuf = null;
        double[] doubleBuf = null;
        switch (bufferDataType) {
            case BYTE:
                byteBuf = new byte[elementsPerLine];
                break;
            case SHORT:
                shortBuf = new short[elementsPerLine];
                break;
            case CHAR:
                charBuf = new char[elementsPerLine];
                break;
            case INT:
                intBuf = new int[elementsPerLine];
                break;
            case FLOAT:
                floatBuf = new float[elementsPerLine];
                break;
            case LONG:
                longBuf = new long[elementsPerLine];
                break;
            case DOUBLE:
                doubleBuf = new double[elementsPerLine];
                break;
            default:
                // it should never happen
        }
        while (dest.hasRemaining()) {
            final int srcFirstPixelIndex = dest.position() / bytesPerPixel;
            final int srcFirstPixelComponentOffset = dest.position() - (srcFirstPixelIndex * bytesPerPixel);
            final int srcFirstColumnIndex = srcFirstPixelIndex % width;
            final int scrFirstRowIndex = (srcFirstPixelIndex - srcFirstColumnIndex) / width;
            final int dstFirstColumnIndex = srcFirstColumnIndex;
            final int dstFirstRowIndex = (height - 1) - scrFirstRowIndex;
            final int dstFirstPixelIndex = dstFirstRowIndex * width + dstFirstColumnIndex;
            final int dstFirstPixelComponentOffset = srcFirstPixelComponentOffset;
            final int dstFirstElementIndex = dstFirstPixelIndex * bytesPerPixel + dstFirstPixelComponentOffset;
            switch (bufferDataType) {
                case BYTE:
                    ((ByteBuffer) src).position(dstFirstElementIndex);
                    ((ByteBuffer) src).get(byteBuf);
                    dest.put(byteBuf);
                    break;
                case SHORT:
                    ((ShortBuffer) src).position(dstFirstElementIndex);
                    ((ShortBuffer) src).get(shortBuf);
                    dest.asShortBuffer().put(shortBuf);
                    dest.position(dest.position() + bytesPerLine);
                    break;
                case CHAR:
                    ((CharBuffer) src).position(dstFirstElementIndex);
                    ((CharBuffer) src).get(charBuf);
                    dest.asCharBuffer().put(charBuf);
                    dest.position(dest.position() + bytesPerLine);
                    break;
                case INT:
                    ((IntBuffer) src).position(dstFirstElementIndex);
                    ((IntBuffer) src).get(intBuf);
                    dest.asIntBuffer().put(intBuf);
                    dest.position(dest.position() + bytesPerLine);
                    break;
                case FLOAT:
                    ((FloatBuffer) src).position(dstFirstElementIndex);
                    ((FloatBuffer) src).get(floatBuf);
                    dest.asFloatBuffer().put(floatBuf);
                    dest.position(dest.position() + bytesPerLine);
                    break;
                case LONG:
                    ((LongBuffer) src).position(dstFirstElementIndex);
                    ((LongBuffer) src).get(longBuf);
                    dest.asLongBuffer().put(longBuf);
                    dest.position(dest.position() + bytesPerLine);
                    break;
                case DOUBLE:
                    ((DoubleBuffer) src).position(dstFirstElementIndex);
                    ((DoubleBuffer) src).get(doubleBuf);
                    dest.asDoubleBuffer().put(doubleBuf);
                    dest.position(dest.position() + bytesPerLine);
                    break;
                default:
                    // it should never happen
            }
        }
        src.position(srcPos);
        dest.position(destPos);
    }

    public boolean isMipmapsProductionEnabled() {
        return _mipmapsProductionEnabled;
    }

    public void setMipmapsProductionEnabled(final boolean mipmapsProductionEnabled) {
        _mipmapsProductionEnabled = mipmapsProductionEnabled;
    }
}
