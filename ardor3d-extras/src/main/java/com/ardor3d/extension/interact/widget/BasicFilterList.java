/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;

public class BasicFilterList implements IFilterList {
    final List<UpdateFilter> _filters = new ArrayList<>();

    @Override
    public Iterator<UpdateFilter> iterator() {
        return _filters.iterator();
    }

    @Override
    public void applyFilters(final InteractManager manager) {
        // apply any filters to our state
        for (final UpdateFilter filter : _filters) {
            filter.applyFilter(manager);
        }
    }

    @Override
    public void beginDrag(final InteractManager manager) {
        for (final UpdateFilter filter : _filters) {
            filter.beginDrag(manager);
        }
    }

    @Override
    public void endDrag(final InteractManager manager) {
        for (final UpdateFilter filter : _filters) {
            filter.endDrag(manager);
        }
    }

    @Override
    public int size() {
        return _filters.size();
    }

    @Override
    public UpdateFilter get(final int index) {
        return _filters.get(index);
    }

    @Override
    public boolean add(final UpdateFilter filter) {
        return _filters.add(filter);
    }

    @Override
    public boolean remove(final UpdateFilter filter) {
        return _filters.remove(filter);
    }

    @Override
    public void clear() {
        _filters.clear();
    }

}
