/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nikska.module.php.refactoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import org.netbeans.api.lexer.Language;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.spi.support.ModificationResult;
import org.netbeans.modules.csl.spi.support.ModificationResult.Difference;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ProgressProviderAdapter;
import org.netbeans.modules.refactoring.spi.RefactoringCommit;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.nikska.module.php.refactoring.MoveSupport.Results;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.PositionBounds;
import org.openide.text.PositionRef;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Loïc Laverdant
 */
public class PhpMoveRefactoringPlugin extends ProgressProviderAdapter implements RefactoringPlugin {

    protected AbstractRefactoring refactoring;
    private final MoveSupport usages;
    private final List<ModelElement> modelElements;

    public PhpMoveRefactoringPlugin(PhpMoveRefactoring refactoring) {
        this.refactoring = refactoring;
        this.usages = refactoring.getRefactoringSource().lookup(MoveSupport.class);
        modelElements = usages.getModelElements();
        String test = "";
    }

    @NbBundle.Messages({
        "MSG_Error_ElementEmpty=The element name cannot be empty.",})
    @Override
    public Problem checkParameters() {
        String newName = getRefactoring().getNewName();
        String newType = getRefactoring().getNewType();
        if (newName != null && newType.equals("Method")) {
            String trimmedNewName = newName.trim();
            if (trimmedNewName.length() == 0) {
                return new Problem(true, Bundle.MSG_Error_ElementEmpty());
            }
        }
        return null;
    }

    public PhpMoveRefactoring getRefactoring() {
        return (PhpMoveRefactoring) refactoring;
    }

    @Override
    public Problem fastCheckParameters() {
        return null;
    }

    @Override
    public Problem prepare(final RefactoringElementsBag elementsBag) {
        fireProgressListenerStep();
        Results results = usages.getResults();
        results.addEntry(usages.getDeclarationFileObject());
        refactorResults(results, elementsBag, usages.getDeclarationFileObject());
        fireProgressListenerStop();
        return null;
    }

    protected void refactorResults(Results results, RefactoringElementsBag refactoringElements, FileObject declarationFileObject) {
        final ModificationResult modificationResult = new ModificationResult();

        refactorElement(modificationResult);
        refactoringElements.registerTransaction(new RefactoringCommit(Collections.singletonList(modificationResult)));
        for (FileObject fo : modificationResult.getModifiedFileObjects()) {
            for (Difference diff : modificationResult.getDifferences(fo)) {
                refactoringElements.add(refactoring, MoveDiffElement.create(diff, fo, modificationResult));
            }
        }
    }

    private void refactorElement(ModificationResult modificationResult) {

        DataObject dob = null;
        try {
            dob = DataObject.find(usages.getDeclarationFileObject());
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        CloneableEditorSupport cloneableEditorSupport = null;
        Object obj = dob.getLookup().lookup(org.openide.cookies.OpenCookie.class);
        if (obj instanceof CloneableEditorSupport) {
            cloneableEditorSupport = (CloneableEditorSupport) obj;
        }
        obj = dob.getLookup().lookup(org.openide.cookies.EditorCookie.class);
        if (cloneableEditorSupport == null && obj instanceof CloneableEditorSupport) {
            cloneableEditorSupport = (CloneableEditorSupport) obj;
        }

        CloneableEditorSupport ces = cloneableEditorSupport;
        moveDiff(ces, modificationResult);
    }

    @Override
    public Problem preCheck() {
        return null;
    }

    @Override
    public void cancelRequest() {
    }

    private void moveDiff(CloneableEditorSupport ces, ModificationResult modificationResult) {
        List<Difference> diffs = new ArrayList<>();

        PositionRef ref1 = ces.createPositionRef(usages.getBegin(), Position.Bias.Forward);
        PositionRef ref2 = ces.createPositionRef(usages.getEnd(), Position.Bias.Forward);
        PositionBounds bounds = new PositionBounds(ref1, ref2);
        String text = "";
        try {
            text = bounds.getText();
        } catch (BadLocationException | IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        diffs.add(new Difference(Difference.Kind.CHANGE,
                bounds.getBegin(),
                bounds.getEnd(),
                text,
                reformatNewText(usages.getBegin(), usages.getEnd() - usages.getBegin(), getUsageNewDeclaration()),
                "Method usage : " + getRefactoring().getNewName() + "(" + usages.getParameters() + ");"));

        int classOffsetEnd = usages.getClassDeclaration().getEndOffset() - 1;
        PositionRef begin = ces.createPositionRef(classOffsetEnd, Position.Bias.Backward);
        String newMethod = getStartNewDeclaration() + text + getReturnDeclaration() + getEndNewDeclaration();
        diffs.add(new Difference(Difference.Kind.INSERT,
                begin,
                begin,
                "",
                reformatNewText(classOffsetEnd, 0, newMethod),
                "Methode declaration"));

        if (!diffs.isEmpty()) {
            modificationResult.addDifferences(usages.getDeclarationFileObject(), diffs);
        }

    }

    private String reformatNewText(int offsetBegin, int length, String newText) {

        try {
            FileObject file = usages.getDeclarationFileObject();
            DataObject od = DataObject.find(file);
            EditorCookie ec = od.getLookup().lookup(EditorCookie.class);
            if (ec != null) {
                BaseDocument bdoc = (BaseDocument) ec.openDocument();
                String mimeType = (String) bdoc.getProperty("mimeType"); //NOI18N
                BaseDocument newDoc = new BaseDocument(false, mimeType);
                Language language = (Language) bdoc.getProperty(Language.class);
                newDoc.putProperty(Language.class, language);
                newDoc.insertString(0, bdoc.getText(0, bdoc.getLength()), null);

                if (length > 0) {
                    newDoc.remove(offsetBegin, length);
                }
                newDoc.insertString(offsetBegin, newText, null);
                int reformatLenght = Utilities.reformat(newDoc, offsetBegin, offsetBegin + newText.length());
                String reformatedText = newDoc.getText(offsetBegin, reformatLenght);
                return reformatedText;
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException | BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }

        return newText;
    }

    private String getUsageNewDeclaration() {
        String newDeclaration = usages.getReturnsAssignment() + "$this->" + getRefactoring().getNewName() + "(" + usages.getParameters() + ");";
        return newDeclaration;
    }

    private String getStartNewDeclaration() {
        String newDeclaration = getRefactoring().getModifier() + " function " + getRefactoring().getNewName() + "(" + usages.getParameters() + ") {\n";
        return newDeclaration;
    }

    private String getEndNewDeclaration() {
        String newDeclaration = "\n}";
        return newDeclaration;
    }

    private String getReturnDeclaration() {
        String returns = "\n" + usages.getReturns() + "\n";
        return returns;
    }

}
