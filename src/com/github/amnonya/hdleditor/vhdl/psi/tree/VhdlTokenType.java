package com.github.amnonya.hdleditor.vhdl.psi.tree;

import com.github.amnonya.hdleditor.vhdl.lang.VhdlLanguage;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class VhdlTokenType extends IElementType {
    public VhdlTokenType(@NotNull @NonNls String debugName) {
        super(debugName, VhdlLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "VhdlTokenType." + super.toString();
    }
}

