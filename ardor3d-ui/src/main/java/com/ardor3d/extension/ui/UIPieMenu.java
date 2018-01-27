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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Vector2;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.visitor.Visitor;

/**
 * A special frame meant to display menu items.
 */
public class UIPieMenu extends UIContainer implements IPopOver {

    public static final int DEFAULT_INNER_RADIUS = 50;

    private int _innerRadius, _outerRadius;
    private double _radians = 1.0;

    private boolean _menuDirty = true;

    private UIPieMenuItem _center;

    public UIPieMenu(final UIHud hud) {
        this(hud, UIPieMenu.DEFAULT_INNER_RADIUS, Math.min(hud.getWidth() / 2, hud.getHeight() / 2));
    }

    public UIPieMenu(final UIHud hud, final int innerRadius) {
        this(hud, innerRadius, Math.min(hud.getWidth() / 2, hud.getHeight() / 2));
    }

    public UIPieMenu(final UIHud hud, final int innerRadius, final int outerRadius) {
        super();
        _innerRadius = innerRadius;
        _outerRadius = outerRadius;
        setHud(hud);
        applySkin();
    }

    @Override
    public void showAt(final int x, final int y) {
        setHudXY(x - _outerRadius, y - _outerRadius);
        updateGeometricState(0, true);
    }

    @Override
    public void setHud(final UIHud hud) {
        _parent = hud;
        attachedToHud();
    }

    public int getInnerRadius() {
        return _innerRadius;
    }

    public int getOuterRadius() {
        return _outerRadius;
    }

    public void setInnerRadius(final int radius) {
        _menuDirty = true;
        _innerRadius = radius;
    }

    public void setOuterRadius(final int radius) {
        _menuDirty = true;
        _outerRadius = radius;
    }

    public double getCurrentArcLength() {
        return _radians;
    }

    public void addItem(final UIPieMenuItem item) {
        _menuDirty = true;
        add(item);
    }

    public void removeItem(final UIPieMenuItem item) {
        _menuDirty = true;
        remove(item);
    }

    public UIPieMenuItem getCenterItem() {
        return _center;
    }

    public void setCenterItem(final UIPieMenuItem item) {
        _menuDirty = true;
        if (_center != null) {
            remove(_center);
        }
        if (item != null) {
            add(item);
        }
        _center = item;
    }

    public void clearCenterItem() {
        setCenterItem(null);
    }

    public void clearItems() {
        _menuDirty = true;
        removeAllComponents();
    }

    @Override
    public UIComponent getUIComponent(final int hudX, final int hudY) {
        final Vector2 vec = new Vector2(hudX - getHudX() - _outerRadius, hudY - _outerRadius);

        // check we are inside the pie
        final double distSq = vec.lengthSquared();
        if (distSq < _innerRadius * _innerRadius) {
            return _center != null ? _center : this;
        }
        if (distSq > _outerRadius * _outerRadius) {
            return this;
        }

        vec.normalizeLocal();

        double r = Math.atan2(1, 0) - Math.atan2(vec.getY(), vec.getX());

        r += _radians / 2;
        if (r < 0) {
            r += MathUtils.TWO_PI;
        }

        int index = (int) (r / _radians);
        for (int i = 0; i < getNumberOfChildren(); i++) {
            final Spatial s = getChild(i);
            if (s == _center) {
                continue;
            }
            if (s instanceof UIComponent) {
                if (index == 0) {
                    return (UIComponent) s;
                }
                index--;
            }
        }
        return this;
    }

    @Override
    public void layout() {
        if (!_menuDirty) {
            return;
        }

        final List<Spatial> content = getChildren();
        if (content == null) {
            return;
        }

        // gather our components
        final List<UIComponent> comps = new ArrayList<>();
        final Rectangle2 storeA = Rectangle2.fetchTempInstance();
        for (int i = 0; i < content.size(); i++) {
            final Spatial spat = content.get(i);
            if (spat instanceof UIComponent) {
                final UIComponent comp = (UIComponent) spat;
                final Rectangle2 minRect = comp.getRelativeMinComponentBounds(storeA);
                comp.fitComponentIn(minRect.getWidth(), minRect.getHeight());
                if (comp == _center) {
                    final Rectangle2 rect = comp.getRelativeComponentBounds(storeA);
                    comp.setLocalXY(_outerRadius - rect.getWidth() / 2, _outerRadius - rect.getHeight() / 2);
                    continue;
                }
                comps.add(comp);
            }
        }

        // if we don't have components to layout, exit
        if (comps.isEmpty()) {
            Rectangle2.releaseTempInstance(storeA);
            return;
        }

        // Figure out slice size
        _radians = MathUtils.TWO_PI / Math.max(2, comps.size());
        final int radius = (_innerRadius + _outerRadius) / 2;
        double position = 0;
        for (int i = 0, maxI = comps.size(); i < maxI; i++) {
            final UIComponent comp = comps.get(i);

            final Rectangle2 rect = comp.getRelativeComponentBounds(storeA);
            final int x = (int) MathUtils.round(radius * MathUtils.sin(position));
            final int y = (int) MathUtils.round(radius * MathUtils.cos(position));

            comp.setLocalXY(_outerRadius + x - rect.getWidth() / 2, _outerRadius + y - rect.getHeight() / 2);

            // step forward
            position += _radians;
        }

        Rectangle2.releaseTempInstance(storeA);
        _menuDirty = false;
    }

    @Override
    public void updateMinimumSizeFromContents() {
        setMinimumContentSize(_outerRadius * 2, _outerRadius * 2);
    }

    @Override
    public void close() {
        final UIHud hud = getHud();
        if (hud == null) {
            throw new IllegalStateException("UIPieMenu is not attached to a hud.");
        }

        // Close any open tooltip
        hud.getTooltip().setVisible(false);

        // clear any resources for standin
        clearStandin();

        // clean up any state
        acceptVisitor(new Visitor() {
            @Override
            public void visit(final Spatial spatial) {
                if (spatial instanceof StateBasedUIComponent) {
                    final StateBasedUIComponent comp = (StateBasedUIComponent) spatial;
                    comp.switchState(comp.getDefaultState());
                }
            }
        }, true);

        hud.remove(this);
        _parent = null;
    }

    public int getSliceIndex(final UIPieMenuItem item) {
        final List<Spatial> content = getChildren();
        if (content == null) {
            return -1;
        }

        int x = 0;
        for (int i = 0; i < content.size(); i++) {
            final Spatial spat = content.get(i);
            if (spat == _center) {
                continue;
            }
            if (spat == item) {
                return x;
            }
            if (spat instanceof UIPieMenuItem) {
                x++;
            }
        }

        return -1;
    }
}