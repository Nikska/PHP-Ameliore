package org.nikska.module.php.refactoring;

import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.util.Lookup;

/**
 *
 * @author nikska
 */
public final class PhpMoveRefactoring extends AbstractRefactoring {

    private String newName;
    private String modifier;
    private String newType;

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public PhpMoveRefactoring(Lookup lkp) {
        super(lkp);
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public String getNewName() {
        return newName;
    }
    
    public void setNewType(String newType) {
        this.newType = newType;
    }

    public String getNewType() {
        return this.newType;
    }
    
}
