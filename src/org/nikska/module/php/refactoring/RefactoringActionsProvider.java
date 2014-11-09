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

import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.api.ElementQuery;
import static org.netbeans.modules.php.editor.api.ElementQueryFactory.getIndexQuery;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.openide.cookies.EditorCookie;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/**
 *
 * @author Loïc Laverdant
 */
@org.openide.util.lookup.ServiceProvider(service = org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider.class, position = 400)
public class RefactoringActionsProvider extends ActionsImplementationProvider {

    private boolean isFromEditor(Lookup lookup) {
        //TODO: is from editor? review, improve
        EditorCookie ec = lookup.lookup(EditorCookie.class);
        return ec != null && ec.getOpenedPanes() != null;
    }

    @Override
    public boolean canMove(Lookup lookup) {
        return true;
    }

    @Override
    public void doMove(final Lookup lookup) {
        EditorCookie ec = lookup.lookup(EditorCookie.class);
        if (isFromEditor(lookup)) {
            moveElement(ec);
        }
    }

    private void moveElement(EditorCookie ec) {
        new RefactoringTask.TextComponentTask(ec) {

            @Override
            protected RefactoringUIHolder createRefactoringUI(final PHPParseResult info, final int offset, OffsetRange offsetRange) {
                RefactoringUIHolder result = RefactoringUIHolder.NONE;
                ElementQuery.Index index = getIndexQuery(info);
                MoveSupport ctx = MoveSupport.getInstance(index, info, offset, offsetRange);
                if (ctx != null) {
                    result = new RefactoringUIHolderImpl(new PhpMovefactoringUI(ctx));
                }
                return result;
            }
        }.run();
    }

    static final class RefactoringUIHolderImpl implements RefactoringTask.RefactoringUIHolder {

        private final RefactoringUI refactoringUI;

        public RefactoringUIHolderImpl(RefactoringUI refactoringUI) {
            assert refactoringUI != null;
            this.refactoringUI = refactoringUI;
        }

        @Override
        public void processUI(boolean parsingInProgress) {
            UI.openRefactoringUI(refactoringUI, TopComponent.getRegistry().getActivated());
        }

    }
}
