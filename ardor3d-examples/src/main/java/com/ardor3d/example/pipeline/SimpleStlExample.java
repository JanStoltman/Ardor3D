/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.stl.StlGeometryStore;
import com.ardor3d.extension.model.stl.StlImporter;
import com.ardor3d.math.Vector3;

/**
 * Simplest example of loading a Wavefront STL model.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleStlExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleStlExample.jpg", //
        maxHeapMemory = 64)
public class SimpleStlExample extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleStlExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Stl Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 0, 70));

        // Load the STL scene
        final long time = System.currentTimeMillis();
        final StlImporter importer = new StlImporter();
        final StlGeometryStore storage = importer.load("stl/space_invader_magnet.stl");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        _root.attachChild(storage.getScene());
    }
}