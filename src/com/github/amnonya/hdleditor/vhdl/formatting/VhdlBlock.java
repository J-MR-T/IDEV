package com.github.amnonya.hdleditor.vhdl.formatting;

import com.github.amnonya.hdleditor.vhdl.psi.VhdlTypes;
import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class VhdlBlock implements ASTBlock {

    private final VhdlBlock myParent;
    private final Alignment myAlignment;
    private final Indent myIndent;
    private final ASTNode myNode;
    private final Wrap myWrap;
    private final SpacingBuilder mySpacingBuilder;
    //    private final PyBlockContext myContext; // for settings. put in constructor (@NotNull)
    private List<VhdlBlock> mySubBlocks = null;
    private Map<ASTNode, VhdlBlock> mySubBlockByNode = null;
//    private final boolean myEmptySequence;

    // Shared among multiple children sub-blocks
    private Alignment myChildAlignment = null;
    private Alignment[] myListAlignments = null;
    //    private Wrap myDictWrapping = null;
//    private Wrap myFromImportWrapping = null;
    private Boolean myIncomplete;

    private static final TokenSet NORMAL_INDENTED_BLOCKS = TokenSet.create(
            VhdlTypes.USE_CLAUSE, VhdlTypes.ENTITY_HEADER, VhdlTypes.GENERIC_INTERFACE_LIST,
            VhdlTypes.PORT_INTERFACE_LIST, VhdlTypes.ARCHITECTURE_DECLARATIVE_PART,
            VhdlTypes.ARCHITECTURE_STATEMENT_PART, VhdlTypes.PROCESS_DECLARATIVE_PART, VhdlTypes.PROCESS_STATEMENT_PART,
            VhdlTypes.CASE_STATEMENT_ALTERNATIVE, VhdlTypes.SEQUENCE_OF_STATEMENTS, VhdlTypes.PACKAGE_DECLARATIVE_PART,
            VhdlTypes.PACKAGE_BODY_DECLARATIVE_PART, VhdlTypes.SUBPROGRAM_DECLARATIVE_PART,
            VhdlTypes.SUBPROGRAM_STATEMENT_PART, /*TODO: fix later: VhdlTypes.ENUMERATION_LIST,*/
            VhdlTypes.ELEMENT_DECLARATION, VhdlTypes.PROCEDURE_PARAMETER_LIST, VhdlTypes.FUNCTION_PARAMETER_LIST,
            VhdlTypes.BLOCK_DECLARATIVE_PART, VhdlTypes.BLOCK_STATEMENT_PART
    );

    private static final TokenSet LABEL_INDENTED_BLOCKS = TokenSet.create(
            VhdlTypes.LABEL
    );

    private static final TokenSet ALWAYS_WRAPPED_BLOCKS = TokenSet.create(
            VhdlTypes.GENERIC_CLAUSE, VhdlTypes.PORT_CLAUSE, VhdlTypes.INTERFACE_GENERIC_DECLARATION,
            VhdlTypes.INTERFACE_PORT_DECLARATION, VhdlTypes.ARCHITECTURE_DECLARATIVE_PART,
            VhdlTypes.ARCHITECTURE_STATEMENT_PART, VhdlTypes.PROCESS_DECLARATIVE_PART, VhdlTypes.PROCESS_STATEMENT_PART,
            VhdlTypes.CASE_STATEMENT_ALTERNATIVE, VhdlTypes.SEQUENCE_OF_STATEMENTS, VhdlTypes.T_BEGIN, VhdlTypes.T_END,
            VhdlTypes.PACKAGE_DECLARATIVE_PART, VhdlTypes.PACKAGE_BODY_DECLARATIVE_PART,
            VhdlTypes.SUBPROGRAM_DECLARATIVE_PART, VhdlTypes.SUBPROGRAM_STATEMENT_PART, VhdlTypes.ENUMERATION_LITERAL,
            VhdlTypes.ELEMENT_DECLARATION, VhdlTypes.T_RECORD, VhdlTypes.PROCEDURE_PARAMETER_CONSTANT_DECLARATION,
            VhdlTypes.PROCEDURE_PARAMETER_SIGNAL_DECLARATION, VhdlTypes.PROCEDURE_PARAMETER_VARIABLE_DECLARATION,
            VhdlTypes.FUNCTION_PARAMETER_CONSTANT_DECLARATION, VhdlTypes.FUNCTION_PARAMETER_SIGNAL_DECLARATION
    );

    public VhdlBlock(@Nullable VhdlBlock parent, @NotNull ASTNode node, @Nullable Alignment alignment,
                     @NotNull Indent indent, @Nullable Wrap wrap, SpacingBuilder spacingBuilder) {
        myParent = parent;
        myAlignment = alignment;
        myIndent = indent;
        myNode = node;
        myWrap = wrap;
        mySpacingBuilder = spacingBuilder;
//        myEmptySequence = isEmptySequence(node);

//        final PyCodeStyleSettings pySettings = myContext.getPySettings();
        final IElementType myType = node.getElementType();

        if (myType == VhdlTypes.PORT_CLAUSE) {
            myListAlignments = new Alignment[]{
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
            };
        }
        if (myType == VhdlTypes.GENERIC_CLAUSE) {
            myListAlignments = new Alignment[]{
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
            };
        }
        if (myType == VhdlTypes.ARCHITECTURE_DECLARATIVE_PART
                || myType == VhdlTypes.BLOCK_DECLARATIVE_PART
                || myType == VhdlTypes.PACKAGE_DECLARATIVE_PART
                || myType == VhdlTypes.PACKAGE_BODY_DECLARATIVE_PART
                || myType == VhdlTypes.GENERATE_STATEMENT) {
            myListAlignments = new Alignment[]{
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
                    Alignment.createAlignment(true),
            };
        }

    }

    public ASTNode getNode() {
        return myNode;
    }

    @NotNull
    @Override
    public TextRange getTextRange() {
        return myNode.getTextRange();
    }

    @NotNull
    @Override
    public List<Block> getSubBlocks() {
        if (mySubBlocks == null) {
            mySubBlockByNode = buildSubBlocks();
            mySubBlocks = new ArrayList<>(mySubBlockByNode.values());
        }
        return Collections.unmodifiableList(mySubBlocks);
    }

    @Nullable
    @Override
    public Wrap getWrap() {
        return myWrap;
    }

    @Nullable
    @Override
    public Indent getIndent() {
        assert myIndent != null;
        return myIndent;
    }

    @Nullable
    @Override
    public Alignment getAlignment() {
        return myAlignment;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        // TODO:
        return mySpacingBuilder.getSpacing(this, child1, child2);
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(final int newChildIndex) {
        VhdlBlock childBlock = mySubBlocks.get(newChildIndex);
        IElementType childType = childBlock.myNode.getElementType();

        Indent childIndent = childBlock.myIndent;
        Alignment childAlignment = childBlock.myAlignment;

        if (childType == VhdlTypes.T_END) {
            childIndent = Indent.getNormalIndent();
        }

        return new ChildAttributes(childIndent, childAlignment);
    }

    @Override
    public boolean isIncomplete() {
        if (myIncomplete == null) {
            myIncomplete = FormatterUtil.isIncomplete(myNode);
        }
        return myIncomplete;
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @NotNull
    private Map<ASTNode, VhdlBlock> buildSubBlocks() {
        final Map<ASTNode, VhdlBlock> blocks = new LinkedHashMap<>();
        Iterable<ASTNode> childNodes = getSubBlockNodes();
        for (ASTNode child : childNodes) {
            final IElementType childType = child.getElementType();

            if (child.getTextRange().isEmpty()) {
                continue;
            }

            if (childType == TokenType.WHITE_SPACE) {
                continue;
            }

            blocks.put(child, buildSubBlock(child));
        }
        return Collections.unmodifiableMap(blocks);
    }

    @NotNull
    private Iterable<ASTNode> getSubBlockNodes() {
        return Arrays.asList(myNode.getChildren(null));
    }

    @NotNull
    private VhdlBlock buildSubBlock(@NotNull ASTNode child) {
        final IElementType myChildType = child.getElementType();
        final IElementType myType = myNode.getElementType();

        // Wrap:
        Wrap childWrap = Wrap.createWrap(WrapType.NONE, false);

//        if (ALWAYS_WRAPPED_BLOCKS.contains(myChildType)) {
//            childWrap = Wrap.createWrap(WrapType.ALWAYS, true);
//        }


        // Indent:
        Indent childIndent = Indent.getNoneIndent();

        if (NORMAL_INDENTED_BLOCKS.contains(myChildType)) {
            childIndent = Indent.getNormalIndent();
        } else if ((myChildType == VhdlTypes.T_SEVERITY)
                || (myChildType == VhdlTypes.T_REPORT && myType == VhdlTypes.ASSERTION)) {
            childIndent = Indent.getNormalIndent();
        } else if (LABEL_INDENTED_BLOCKS.contains(myChildType)) {
            childIndent = Indent.getLabelIndent();
        }


        // Alignment:
        Alignment childAlignment = null;

        if (myType == VhdlTypes.INTERFACE_PORT_DECLARATION) {
            if (myChildType == VhdlTypes.T_COLON) {
                childAlignment = myParent.myParent.myListAlignments[0];
            } else if (myChildType == VhdlTypes.MODE) {
                childAlignment = myParent.myParent.myListAlignments[1];
            } else if (myChildType == VhdlTypes.SUBTYPE_INDICATION) {
                childAlignment = myParent.myParent.myListAlignments[2];
            } else if (myChildType == VhdlTypes.T_BLOCKING_ASSIGNMENT) {
                childAlignment = myParent.myParent.myListAlignments[3];
            }
        } else if (myChildType == VhdlTypes.COMMENT) {
            Alignment commentAlignment = getPortDeclarationCommentAlignment(child);
            if (commentAlignment != null) {
                childAlignment = commentAlignment;
            }
        }
        if (myType == VhdlTypes.INTERFACE_GENERIC_DECLARATION) {
            if (myChildType == VhdlTypes.T_COLON) {
                childAlignment = myParent.myParent.myListAlignments[0];
            } else if (myChildType == VhdlTypes.T_BLOCKING_ASSIGNMENT) {
                childAlignment = myParent.myParent.myListAlignments[1];
            }
        } else if (myChildType == VhdlTypes.COMMENT) {
            Alignment commentAlignment = getGenericDeclarationCommentAlignment(child);
            if (commentAlignment != null) {
                childAlignment = commentAlignment;
            }
        }
        if (myType == VhdlTypes.SIGNAL_DECLARATION) {
            if (myChildType == VhdlTypes.T_COLON) {
                childAlignment = myParent.myListAlignments[0];
            } else if (myChildType == VhdlTypes.T_BLOCKING_ASSIGNMENT) {
                childAlignment = myParent.myListAlignments[1];
            }
        } else if (myType == VhdlTypes.CONSTANT_DECLARATION) {
            if (myChildType == VhdlTypes.T_COLON) {
                childAlignment = myParent.myListAlignments[0];
            } else if (myChildType == VhdlTypes.T_BLOCKING_ASSIGNMENT) {
                childAlignment = myParent.myListAlignments[1];
            }
        }

        // --------------------------------------------------------------------
        return new VhdlBlock(this, child, childAlignment, childIndent, childWrap, mySpacingBuilder);
    }

    private Alignment getPortDeclarationCommentAlignment(@NotNull ASTNode child) {
        IElementType myType = myNode.getElementType();
        ASTNode childPrevSibling = child.getTreePrev();
        if (childPrevSibling == null) {
            return null;
        }
        IElementType childPrevSiblingType = childPrevSibling.getElementType();

        if (myType == VhdlTypes.PORT_INTERFACE_LIST) {
            // No space between port declaration and comment:
            boolean noSpace = childPrevSiblingType == VhdlTypes.T_SEMICOLON;
            // Space between port declaration and comment, but they are still on the same line:
            boolean spaceWithoutLineBreak = childPrevSiblingType == TokenType.WHITE_SPACE && !childPrevSibling.getText().contains("\n");

            if (noSpace || spaceWithoutLineBreak) {
                // Comment is in the same line of the port declaration, i.e. it describes the port.
                // Port is not the last on the list.
                return myParent.myListAlignments[4];
            }
        } else if (myType == VhdlTypes.PORT_CLAUSE) {
            // No space between port declaration and comment:
            boolean noSpace = childPrevSiblingType == VhdlTypes.PORT_INTERFACE_LIST;
            // Space between port declaration and comment, but they are still on the same line:
            boolean spaceWithoutLineBreak = childPrevSiblingType == TokenType.WHITE_SPACE && !childPrevSibling.getText().contains("\n");

            if (noSpace || spaceWithoutLineBreak) {
                // Comment is in the same line of the port declaration, i.e. it describes the port.
                // Port is the last on the list.
                return myListAlignments[4];
            }
        }
        return null;
    }

    private Alignment getGenericDeclarationCommentAlignment(@NotNull ASTNode child) {
        IElementType myType = myNode.getElementType();
        ASTNode childPrevSibling = child.getTreePrev();
        if (childPrevSibling == null) {
            return null;
        }
        IElementType childPrevSiblingType = childPrevSibling.getElementType();

        if (myType == VhdlTypes.GENERIC_INTERFACE_LIST) {
            // No space between generic declaration and comment:
            boolean noSpace = childPrevSiblingType == VhdlTypes.T_SEMICOLON;
            // Space between generic declaration and comment, but they are still on the same line:
            boolean spaceWithoutLineBreak = childPrevSiblingType == TokenType.WHITE_SPACE && !childPrevSibling.getText().contains("\n");

            if (noSpace || spaceWithoutLineBreak) {
                // Comment is in the same line of the generic declaration, i.e. it describes the generic.
                // Generic is not the last on the list.
                return myParent.myListAlignments[2];
            }
        } else if (myType == VhdlTypes.GENERIC_CLAUSE) {
            // No space between port declaration and comment:
            boolean noSpace = childPrevSiblingType == VhdlTypes.GENERIC_INTERFACE_LIST;
            // Space between port declaration and comment, but they are still on the same line:
            boolean spaceWithoutLineBreak = childPrevSiblingType == TokenType.WHITE_SPACE && !childPrevSibling.getText().contains("\n");

            if (noSpace || spaceWithoutLineBreak) {
                // Comment is in the same line of the generic declaration, i.e. it describes the generic.
                // Generic is the last on the list.
                return myListAlignments[2];
            }
        }
        return null;
    }
}