/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.renderer.Renderer;

public interface IPopOver {

    void showAt(int x, int y);

    void setHud(UIHud hud);

    UIComponent getUIComponent(int hudX, int hudY);

    void onDraw(Renderer renderer);

    void updateGeometricState(double time, boolean initiator);

    void close();

    boolean isAttachedToHUD();

}