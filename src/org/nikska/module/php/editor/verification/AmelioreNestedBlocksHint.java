/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nikska.module.php.editor.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.DoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForEachStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Statement;
import org.netbeans.modules.php.editor.parser.astnodes.WhileStatement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultTreePathVisitor;
import org.netbeans.modules.php.editor.verification.CustomisableRule;
import org.netbeans.modules.php.editor.verification.NestedBlocksHint;
import org.netbeans.modules.php.editor.verification.PHPRuleContext;
import org.nikska.module.php.refactoring.MoveSupport;
import org.nikska.module.php.refactoring.PhpMoveRefactoring;
import org.nikska.module.php.refactoring.PhpMoveRefactoringTool;
import org.nikska.module.php.refactoring.util.RefactoringUtil;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author nikska
 */
public class AmelioreNestedBlocksHint extends NestedBlocksHint implements CustomisableRule {

    private static final Logger LOGGER = Logger.getLogger(NestedBlocksHint.class.getName());
    private static final String NUMBER_OF_ALLOWED_NESTED_BLOCKS = "php.verification.number.of.allowed.nested.blocks"; //NOI18N
    private static final int DEFAULT_NUMBER_OF_ALLOWED_NESTED_BLOCKS = 2;
    private Preferences preferences;

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {

        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() != null) {
            FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
            if (fileObject != null) {
                CheckVisitor checkVisitor = new CheckVisitor(fileObject, context.doc, phpParseResult);
                phpParseResult.getProgram().accept(checkVisitor);
                hints.addAll(checkVisitor.getHints());
            }
        }
    }

    private final class CheckVisitor extends DefaultTreePathVisitor {

        private final FileObject fileObject;
        private final BaseDocument baseDocument;
        private final List<ASTNode> unallowedNestedBlocks;
        private final List<Hint> hints;
        private boolean isInFunctionDeclaration;

        private int countOfNestedBlocks;
        private final PHPParseResult parserResult;

        private CheckVisitor(FileObject fileObject, BaseDocument baseDocument, PHPParseResult parserResult) {
            this.fileObject = fileObject;
            this.baseDocument = baseDocument;
            unallowedNestedBlocks = new ArrayList<>();
            hints = new ArrayList<>();
            this.parserResult = parserResult;
        }

        @NbBundle.Messages(
                "NestedBlocksHintText=Refactoring in new method"
        )
        private Collection<? extends Hint> getHints() {
            for (ASTNode block : unallowedNestedBlocks) {
                if (RefactoringUtil.isInClassDeclaration(parserResult, block.getStartOffset())) {
                    createHint(block);
                }
            }
            return hints;
        }

        private void createHint(ASTNode block) {
            int lineEnd = block.getEndOffset();
            try {
                lineEnd = Utilities.getRowEnd(baseDocument, block.getStartOffset());
            } catch (BadLocationException ex) {
                LOGGER.log(Level.FINE, null, ex);
            }
            OffsetRange offsetRange = new OffsetRange(block.getStartOffset(), lineEnd);
            if (showHint(offsetRange, baseDocument)) {
                hints.add(new Hint(
                        AmelioreNestedBlocksHint.this,
                        Bundle.NestedBlocksHintText(),
                        fileObject,
                        offsetRange,
                        createFixes(block),
                        500));
            }
        }

        private List<HintFix> createFixes(ASTNode block) {
            List<HintFix> fixes = new ArrayList<>();
            fixes.add(new RefactoringHintFix(block, baseDocument, parserResult));
            return fixes;
        }

        @Override
        public void visit(FunctionDeclaration node) {
            scan(node.getFunctionName());
            scan(node.getFormalParameters());
            Block body = node.getBody();
            if (body != null) {
                isInFunctionDeclaration = true;
                scan(body.getStatements());
                isInFunctionDeclaration = false;
            }
        }

        @Override
        public void visit(ForStatement node) {
            Statement body = node.getBody();
            if (body instanceof Block) {
                super.visit(node);
            } else {
                if (isInFunctionDeclaration) {
                    countOfNestedBlocks++;
                    evaluatePossiblyUnallowedNestedBlock();
                    super.visit(node);
                    countOfNestedBlocks--;
                } else {
                    super.visit(node);
                }
            }
        }

        @Override
        public void visit(ForEachStatement node) {
            Statement body = node.getStatement();
            if (body instanceof Block) {
                super.visit(node);
            } else {
                if (isInFunctionDeclaration) {
                    countOfNestedBlocks++;
                    evaluatePossiblyUnallowedNestedBlock();
                    super.visit(node);
                    countOfNestedBlocks--;
                } else {
                    super.visit(node);
                }
            }
        }

        @Override
        public void visit(DoStatement node) {
            Statement body = node.getBody();
            if (body instanceof Block) {
                super.visit(node);
            } else {
                if (isInFunctionDeclaration) {
                    countOfNestedBlocks++;
                    evaluatePossiblyUnallowedNestedBlock();
                    super.visit(node);
                    countOfNestedBlocks--;
                } else {
                    super.visit(node);
                }
            }
        }

        @Override
        public void visit(WhileStatement node) {
            Statement body = node.getBody();
            if (body instanceof Block) {
                super.visit(node);
            } else {
                if (isInFunctionDeclaration) {
                    countOfNestedBlocks++;
                    evaluatePossiblyUnallowedNestedBlock();
                    super.visit(node);
                    countOfNestedBlocks--;
                } else {
                    super.visit(node);
                }
            }
        }

        @Override
        public void visit(IfStatement node) {
            addToPath(node);
            Statement trueStatement = node.getTrueStatement();
            if (trueStatement instanceof Block) {
                scan((Block) trueStatement);
            } else if (trueStatement != null) {
                if (isInFunctionDeclaration) {
                    countOfNestedBlocks++;
                    evaluatePossiblyUnallowedNestedBlock();
                    scan(trueStatement);
                    countOfNestedBlocks--;
                } else {
                    scan(trueStatement);
                }
            }
            Statement falseStatement = node.getFalseStatement();
            if (falseStatement instanceof Block) {
                scan((Block) falseStatement);
            } else if (falseStatement instanceof IfStatement) {
                scan((IfStatement) falseStatement);
            } else if (falseStatement != null) {
                if (isInFunctionDeclaration) {
                    countOfNestedBlocks++;
                    evaluatePossiblyUnallowedNestedBlock();
                    scan(falseStatement);
                    countOfNestedBlocks--;
                } else {
                    scan(falseStatement);
                }
            }
        }

        @Override
        public void visit(Block node) {
            if (isInFunctionDeclaration) {
                countOfNestedBlocks++;
                evaluatePossiblyUnallowedNestedBlock();
                super.visit(node);
                countOfNestedBlocks--;
            } else {
                super.visit(node);
            }
        }

        private void evaluatePossiblyUnallowedNestedBlock() {
            if ((isUnallowedNestedBlock(Rank.FIRST) && !isAllowedConditionInLoop())
                    || (isUnallowedNestedBlock(Rank.SECOND) && allowConditionBlock(preferences))) {
                unallowedNestedBlocks.add(getParentNode());
            }
        }

        private boolean isAllowedConditionInLoop() {
            return allowConditionBlock(preferences) && (getParentNode() instanceof IfStatement)
                    && isInLoopNode();
        }

        private boolean isInLoopNode() {
            boolean isLoopNode = false;
            List<ASTNode> path = getPath();
            int pathSize = path.size();
            if (pathSize > 1) {
                isLoopNode = isLoopNode(path.get(1));
            }
            if (!isLoopNode && pathSize > 2) {
                isLoopNode = (path.get(1) instanceof Block) && isLoopNode(path.get(2));
            }
            return isLoopNode;
        }

        private boolean isLoopNode(ASTNode node) {
            return (node instanceof WhileStatement) || (node instanceof DoStatement)
                    || (node instanceof ForEachStatement) || (node instanceof ForStatement);
        }

        private ASTNode getParentNode() {
            return getPath().get(0);
        }

        private boolean isUnallowedNestedBlock(Rank rank) {
            int numberOfAllowedNestedBlocks = getNumberOfAllowedNestedBlocks(preferences);
            return countOfNestedBlocks > numberOfAllowedNestedBlocks && (countOfNestedBlocks - numberOfAllowedNestedBlocks) == rank.getDistance();
        }

    }

    private enum Rank {

        FIRST(1),
        SECOND(2);

        private final int distance;

        private Rank(int distance) {
            this.distance = distance;
        }

        public int getDistance() {
            return distance;
        }

    }

    private class RefactoringHintFix implements HintFix {

        private final ASTNode block;
        private final BaseDocument doc;
        private final PHPParseResult parserResult;

        public RefactoringHintFix(ASTNode block, BaseDocument doc, PHPParseResult parserResult) {
            this.block = block;
            this.doc = doc;
            this.parserResult = parserResult;
        }

        @Override
        @NbBundle.Messages({
            "RefactoringDisp=Refactoring in new method:"
        })
        public String getDescription() {
            return Bundle.RefactoringDisp();
        }

        @Override
        public void implement() throws Exception {
            
            Collection<Object> lookupContent = new ArrayList<>();
            OffsetRange offsetRange = RefactoringUtil.getFullOffsetRange(block);
            MoveSupport usage = MoveSupport.getInstance(parserResult, offsetRange);
            lookupContent.add(usage);
            PhpMoveRefactoring refactoring = new PhpMoveRefactoring(Lookups.fixed(lookupContent.toArray()));
            configureRefactoring(refactoring);
            
            int templateOffset = getTemplateOffset();
            EditList edits = new EditList(doc);
            
            String useCode = getGeneratedCode(refactoring, usage);
            useCode = PhpMoveRefactoringTool.reformatNewText(refactoring.getResultFileObject(), offsetRange.getStart(), useCode.length(), useCode);
            edits.replace(offsetRange.getStart(), offsetRange.getEnd() - offsetRange.getStart(), useCode, true, 0); //NOI18N
            
            String newCode = getGeneratedNewCode(refactoring, usage);
            newCode = PhpMoveRefactoringTool.reformatNewText(refactoring.getResultFileObject(), templateOffset, 0, newCode);
            edits.replace(templateOffset, 0, "\n" + newCode, true, 0); //NOI18N
            edits.apply();
        }

        private int getTemplateOffset() {
            int offset = block.getEndOffset();
            ClassDeclaration classDeclaration = RefactoringUtil.getClassDeclaration(parserResult, offset);
            if (classDeclaration != null) {
                offset = classDeclaration.getEndOffset() - 1;
            }
            return offset;
        }

        private String getGeneratedNewCode(PhpMoveRefactoring refactoring, MoveSupport usage) {
            String generatedCode = "";

            try {
                String text = doc.getText(block.getStartOffset(), block.getEndOffset() - block.getStartOffset());
                generatedCode += PhpMoveRefactoringTool.getNewDeclaration(refactoring, text);

            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
            return generatedCode;
        }

        private String getGeneratedCode(PhpMoveRefactoring refactoring, MoveSupport usage) {
            String generatedCode = "";
            generatedCode += PhpMoveRefactoringTool.getUsageNewDeclaration(refactoring, usage.getReturnsAssignment(), usage.getParameters());
            return generatedCode;
        }

        private void configureRefactoring(PhpMoveRefactoring refactoring) {

            refactoring.setNewType(MoveSupport.TYPE_METHOD);
            refactoring.setNewName("refactoredMethod");
            refactoring.setModifier("private");
            refactoring.setParserResult(parserResult);
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

    }

    @Override
    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public int getNumberOfAllowedNestedBlocks(Preferences preferences) {
        assert preferences != null;
        return preferences.getInt(NUMBER_OF_ALLOWED_NESTED_BLOCKS, DEFAULT_NUMBER_OF_ALLOWED_NESTED_BLOCKS);
    }

}
