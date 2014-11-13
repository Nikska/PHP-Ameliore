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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelFactory;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.PHPParseResult;

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
    
    public static Set<ClassElement> getSuperClasses(PHPParseResult parserResult, OffsetRange offsetRange) {
        List<ASTNode> nodes = NavUtils.underCaret(parserResult, offsetRange.getStart());
        Set<ClassElement> classes = new HashSet<>();

        for (ASTNode node : nodes) {
            if (node instanceof ClassDeclaration && node.getStartOffset() != offsetRange.getStart()) {
                ClassDeclaration classDeclaration = (ClassDeclaration) node;
                classes.addAll(getSuperClasses(parserResult, classDeclaration.getName().getName()));
            }
        }
        return classes;
    }
    
    public static Set<ClassElement> getSuperClasses(PHPParseResult parserResult, String className) {
        Set<ClassElement> classes = new HashSet<>();
        Model model = ModelFactory.getModel(parserResult);
        ElementQuery.Index index = model.getIndexScope().getIndex();
        ClassElement classElement = ModelUtils.getFirst(index.getClasses(NameKind.exact(className)));

        if (classElement !=null && classElement.getSuperClassName() != null) {
            String superClassName = classElement.getSuperClassName().getName();
            ClassElement superClassElement = ModelUtils.getFirst(index.getClasses(NameKind.exact(superClassName)));
            classes.add(superClassElement);
            classes.addAll(getSuperClasses(parserResult, superClassName));
        }

        return classes;
    }
    
}
