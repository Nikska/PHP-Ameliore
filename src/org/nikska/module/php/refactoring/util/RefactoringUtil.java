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
package org.nikska.module.php.refactoring.util;

import java.util.List;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;

/**
 * @author Loïc Laverdant
 */
public class RefactoringUtil {
    
    public static ClassDeclaration getFirstClassDeclaration(PHPParseResult parserResult) {
        Model model = parserResult.getModel();
        ClassScope classe = ModelUtils.getFirst(ModelUtils.getDeclaredClasses(model.getFileScope()));
        if (classe != null) {
            OffsetRange range = classe.getBlockRange();
            if (range != null) {
                List<ASTNode> nodes = NavUtils.underCaret(parserResult, range.getStart());
                for(ASTNode node : nodes) {
                    if (node instanceof ClassDeclaration) {
                        return (ClassDeclaration) node;
                    }
                }
            }
        }
        return null;
    }
}
