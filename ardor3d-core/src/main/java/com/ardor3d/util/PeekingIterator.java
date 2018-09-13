/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.util.Iterator;

public interface PeekingIterator<E> extends Iterator<E> {

    E peek();

    /**
     * {@inheritDoc}
     */
    @Override
    E next();

    /**
     * {@inheritDoc}
     */
    @Override
    void remove();
}
