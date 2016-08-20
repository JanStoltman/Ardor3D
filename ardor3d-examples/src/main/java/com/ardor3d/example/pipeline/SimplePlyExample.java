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

import java.net.URISyntaxException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.ply.PlyGeometryStore;
import com.ardor3d.extension.model.ply.PlyImporter;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * Simplest example of loading a PLY model.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimplePlyExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimplePlyExample.jpg", //
        maxHeapMemory = 64)
public class SimplePlyExample extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimplePlyExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Ply Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(3.5, 1.5, 5));

        // Load the PLY scene
        final long time = System.currentTimeMillis();
        final PlyImporter importer = new PlyImporter();
        try {
            importer.setTextureLocator(new SimpleResourceLocator(ResourceLocatorTool
                    .getClassPathResource(SimpleObjExample.class, "com/ardor3d/example/media/models/ply/")));
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }
        final PlyGeometryStore storage = importer.load("ply/big_spider.ply");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        final Node model = storage.getScene();
        // the ply model is usually z-up - switch to y-up
        model.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
        _root.attachChild(model);
    }
}
