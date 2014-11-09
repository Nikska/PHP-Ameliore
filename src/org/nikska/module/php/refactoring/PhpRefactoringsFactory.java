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

import org.netbeans.modules.refactoring.api.*;
import org.netbeans.modules.refactoring.spi.*;
import org.openide.util.Lookup;

/**
 * @author Loïc Laverdant
 */
@org.openide.util.lookup.ServiceProvider(service = org.netbeans.modules.refactoring.spi.RefactoringPluginFactory.class, position = 100)
public class PhpRefactoringsFactory implements RefactoringPluginFactory {

    @Override
    public RefactoringPlugin createInstance(AbstractRefactoring refactoring) {
        if (refactoring instanceof PhpMoveRefactoring) {
            return createMove((PhpMoveRefactoring) refactoring);
        }
        return null;
    }

    private RefactoringPlugin createMove(PhpMoveRefactoring refactoring) {
        Lookup look = refactoring.getRefactoringSource();
        MoveSupport handle = look.lookup(MoveSupport.class);
        return (handle != null) ? new PhpMoveRefactoringPlugin(refactoring) : null;
    }
}
