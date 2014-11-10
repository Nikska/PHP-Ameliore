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
import java.util.Collection;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUIBypass;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Loïc Laverdant
 */
public class PhpMovefactoringUI implements RefactoringUI, RefactoringUIBypass {

    private final AbstractRefactoring refactoring;
    private MovePanel panel;
    private final MoveSupport usage;

    public PhpMovefactoringUI(MoveSupport usage) {
        Collection<Object> lookupContent = new ArrayList<>();
        this.usage = usage;
        lookupContent.add(usage);
        this.refactoring = new PhpMoveRefactoring(Lookups.fixed(lookupContent.toArray()));
        this.refactoring.getContext().add(UI.Constants.REQUEST_PREVIEW);
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
            panel = new MovePanel(usage, parent, NbBundle.getMessage(MovePanel.class, "LBL_Move")); //NOI18N
        }

        return panel;
    }

    @Override
    public Problem setParameters() {
        if (refactoring instanceof PhpMoveRefactoring) {
            String newName = panel.getNameValue();
            String modifierName = panel.getModifier();
            ((PhpMoveRefactoring) refactoring).setNewName(newName);
            ((PhpMoveRefactoring) refactoring).setModifier(modifierName);
            ((PhpMoveRefactoring) refactoring).setNewType(panel.getNewType());
            ((PhpMoveRefactoring) refactoring).setParserResult((PHPParseResult) panel.getParserResult());
        }
        return refactoring.checkParameters();
    }

    @Override
    public Problem checkParameters() {
        if (refactoring instanceof PhpMoveRefactoring) {
            ((PhpMoveRefactoring) refactoring).setNewName(panel.getNameValue());
            ((PhpMoveRefactoring) refactoring).setModifier(panel.getModifier());
            ((PhpMoveRefactoring) refactoring).setNewType(panel.getNewType());
            ((PhpMoveRefactoring) refactoring).setParserResult((PHPParseResult) panel.getParserResult());
        }
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
