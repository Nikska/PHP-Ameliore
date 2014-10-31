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
import java.util.Collection;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUIBypass;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Radek Matous
 */
public class PhpMovefactoringUI implements RefactoringUI, RefactoringUIBypass {

    private final AbstractRefactoring refactoring;
    //private final String name;
    //private final ElementKind kind;
    private RenamePanel panel;
    //private final PhpElementKind phpKind;

    public PhpMovefactoringUI(MoveSupport usage) {
        //kind = usage.getElementKind();
        //phpKind = usage.getPhpElementKind();
        //name = getElementName(usage.getName(), kind);
        Collection<Object> lookupContent = new ArrayList<>();
        lookupContent.add(usage);
        this.refactoring = new MoveRefactoring(Lookups.fixed(lookupContent.toArray()));
        this.refactoring.getContext().add(UI.Constants.REQUEST_PREVIEW);
    }

    static String getElementName(final String name, final ElementKind kind) {
        String retval = name;
        if (kind.equals(ElementKind.VARIABLE) || kind.equals(ElementKind.FIELD)) {
            while (retval.length() > 1 && retval.startsWith("$")) { //NOI18N
                retval = retval.substring(1);
            }
        }
        return retval;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(PhpMovefactoringUI.class, "LBL_Move"); //NOI18N
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(PhpMovefactoringUI.class, "LBL_Move_Descr"); //NOI18N
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public CustomRefactoringPanel getPanel(ChangeListener parent) {
        if (panel == null) {
            panel = new RenamePanel(parent, NbBundle.getMessage(RenamePanel.class, "LBL_Move"), true, true); //NOI18N
        }

        return panel;
    }

    @Override
    public Problem setParameters() {
        /*String newName = panel.getNameValue();
        if (refactoring instanceof MoveRefactoring) {
            ((MoveRefactoring) refactoring).setNewName(newName);
            ((MoveRefactoring) refactoring).getContext().add(new RenameDeclarationFile(panel.renameDeclarationFile(), panel.lowerCaseFileName()));
        }*/
        return refactoring.checkParameters();
    }

    @Override
    public Problem checkParameters() {
        /*if (!panel.isUpdateReferences()) {
            return null;
        }
        if (refactoring instanceof RenameRefactoring) {
            ((RenameRefactoring) refactoring).setNewName(panel.getNameValue());
            ((RenameRefactoring) refactoring).getContext().add(new RenameDeclarationFile(panel.renameDeclarationFile(), panel.lowerCaseFileName()));
        }*/
        return refactoring.checkParameters();
    }

    @Override
    public boolean hasParameters() {
        return true;
    }

    @Override
    public AbstractRefactoring getRefactoring() {
        return this.refactoring;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.nikska.modules.php.refactoring.PhpRenameRefactoringUI");
    }

    @Override
    public boolean isRefactoringBypassRequired() {
        return false; //TODO fix this
    }

    @Override
    public void doRefactoringBypass() throws IOException {
        //TODO implement
    }
}
