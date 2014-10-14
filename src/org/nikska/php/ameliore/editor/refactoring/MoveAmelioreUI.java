/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nikska.php.ameliore.editor.refactoring;

import java.io.IOException;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUIBypass;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author nikska
 */
public class MoveAmelioreUI implements RefactoringUI, RefactoringUIBypass  {

    private final AbstractRefactoring refactoring;
    private MoveAmeliorePanel panel;
    private final FileObject file;
    //private String newName;
    
    public MoveAmelioreUI(FileObject file) {
	this.file = file;
        //this.newName = file.getName();
	this.refactoring = new RenameRefactoring(Lookups.fixed(file));
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(MoveAmelioreUI.class, "LBL_MoveAmelioreName");
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(MoveAmelioreUI.class, "LBL_MoveAmelioreDescription");
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public CustomRefactoringPanel getPanel(ChangeListener cl) {
        if (panel == null) {
	    panel = new MoveAmeliorePanel(); //NOI18N
	}
	return panel;
    }

    @Override
    public Problem setParameters() {
        return refactoring.checkParameters();
    }

    @Override
    public Problem checkParameters() {
        return refactoring.fastCheckParameters();
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
        return null;
    }

    @Override
    public boolean isRefactoringBypassRequired() {
        return false;
    }

    @Override
    public void doRefactoringBypass() throws IOException {
        
    }


}
