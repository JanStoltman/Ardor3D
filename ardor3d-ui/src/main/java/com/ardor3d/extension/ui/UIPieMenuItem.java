/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;

/**
 *
 */
public class UIPieMenuItem extends UIMenuItem {

    public UIPieMenuItem(final String text) {
        this(text, null);
    }

    public UIPieMenuItem(final String text, final SubTex icon) {
        this(text, icon, true, null);
    }

    public UIPieMenuItem(final String text, final SubTex icon, final boolean closeMenuOnSelect,
            final ActionListener listener) {
        super(text, icon, closeMenuOnSelect, listener);
    }

    public UIPieMenuItem(final String text, final SubTex icon, final UIPieMenu subMenu, final int size) {
        super(text, icon, false, null);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                showSubMenu(subMenu, size);
            }
        });
    }

    protected void showSubMenu(final UIPieMenu subMenu, final int size) {
        final UIHud hud = getHud();
        if (hud == null) {
            return;
        }

        boolean setup = false;
        final Vector3 showAt = new Vector3(getWorldTranslation());
        if (getParent() instanceof UIPieMenu) {
            final UIPieMenu pie = (UIPieMenu) getParent();
            if (pie.getCenterItem() != this) {
                subMenu.setOuterRadius(pie.getOuterRadius() + size);
                subMenu.setInnerRadius(pie.getOuterRadius());
                subMenu.setTotalArcLength(pie.getSliceRadians());
                subMenu.setStartAngle(pie.getSliceIndex(this) * pie.getSliceRadians() + pie.getStartAngle());
                showAt.set(pie.getWorldTranslation());
                hud.closePopupMenusAfter(pie);
                setup = true;
            }
        }
        if (!setup) {
            subMenu.setOuterRadius(size);
            subMenu.setInnerRadius(0);
            subMenu.setTotalArcLength(MathUtils.TWO_PI);
            subMenu.setStartAngle(0);
        }

        subMenu.updateMinimumSizeFromContents();
        subMenu.layout();

        hud.showSubPopupMenu(subMenu);
        subMenu.showAt((int) showAt.getX(), (int) showAt.getY());
    }

}
