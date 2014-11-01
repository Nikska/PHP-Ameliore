/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.nikska.module.php.refactoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import org.netbeans.modules.csl.spi.support.ModificationResult;
import org.netbeans.modules.csl.spi.support.ModificationResult.Difference;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.spi.ProgressProviderAdapter;
import org.netbeans.modules.refactoring.spi.RefactoringCommit;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.nikska.module.php.refactoring.MoveSupport.Results;
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
 * @author Radek Matous
 */
public class PhpMoveRefactoringPlugin extends ProgressProviderAdapter implements RefactoringPlugin {

    protected AbstractRefactoring refactoring;
    private MoveSupport usages;
    private final List<ModelElement> modelElements;
    
    public PhpMoveRefactoringPlugin(MoveRefactoring refactoring) {
        this.refactoring = refactoring;
        this.usages = refactoring.getRefactoringSource().lookup(MoveSupport.class);
        modelElements = usages.getModelElements();
        String test = "";
    }

    @NbBundle.Messages({
        "MSG_Error_ElementEmpty=The element name cannot be empty.",
        "MSG_Error_SameName=The element has the same name as before.",
        "# {0} - New file name",
        "MSG_Error_FileExists=The file with name \"{0}\" already exists."
    })
    @Override
    public Problem checkParameters() {
        return null;
    }

    public RenameRefactoring getRefactoring() {
        return (RenameRefactoring) refactoring;
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
                /*FileRenamer fileRenamer = FileRenamer.NONE;
                if (fo.equals(declarationFileObject) && renameDeclarationFile != null) {
                    fileRenamer = new DelcarationFileRenamer(fo, renameDeclarationFile);
                }*/
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
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        diffs.add(new Difference(Difference.Kind.REMOVE,
                bounds.getBegin(),
                bounds.getEnd(),
                text,
                "",
                "Desc"));
        
        int classOffsetEnd = usages.getClassDeclaration().getEndOffset() - 1;
        PositionRef begin = ces.createPositionRef(classOffsetEnd, Position.Bias.Backward);

        diffs.add(new Difference(Difference.Kind.INSERT,
                begin,
                begin,
                "",
                text + "\n",
                "Desc2"));
        
        if (!diffs.isEmpty()) {
            modificationResult.addDifferences(usages.getDeclarationFileObject(), diffs);
        }
    }
}
