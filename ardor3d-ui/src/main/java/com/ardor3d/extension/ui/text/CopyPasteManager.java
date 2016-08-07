/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.ui.text.CopyPasteImpl;
import com.ardor3d.ui.text.NullCopyPasteImpl;

public enum CopyPasteManager {
    INSTANCE;

    private CopyPasteImpl _impl;

    public void setCopyPasteImpl(final CopyPasteImpl impl) {
        _impl = impl;
    }

    public String getClipBoardContents() {
        if (_impl == null) {
            try {
                _impl = (CopyPasteImpl) Class.forName("com.ardor3d.ui.text.awt.AwtCopyPasteImpl").newInstance();
            } catch (final Throwable t) {
                _impl = new NullCopyPasteImpl();
            }
        }
        return _impl.getClipBoardContents();
    }

    public void setClipBoardContents(final String contents) {
        if (_impl == null) {
            try {
                _impl = (CopyPasteImpl) Class.forName("com.ardor3d.ui.text.awt.AwtCopyPasteImpl").newInstance();
            } catch (final Throwable t) {
                _impl = new NullCopyPasteImpl();
            }
        }
        _impl.setClipBoardContents(contents);
    }
}
