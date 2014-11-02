/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nikska.module.php.refactoring;

import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.util.Lookup;

/**
 *
 * @author nikska
 */
public final class PhpMoveRefactoring extends AbstractRefactoring {

    private String newName;
    public PhpMoveRefactoring(Lookup lkp) {
        super(lkp);
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public String getNewName() {
        return newName;
    }
    
}
