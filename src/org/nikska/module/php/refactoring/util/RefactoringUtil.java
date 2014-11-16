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

import java.util.Collection;
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
import org.netbeans.modules.php.editor.parser.astnodes.DoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForEachStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForStatement;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Statement;
import org.netbeans.modules.php.editor.parser.astnodes.WhileStatement;

/**
 * @author Loïc Laverdant
 */
public class RefactoringUtil {
    
    public static boolean isInClassDeclaration(PHPParseResult parserResult, int offset) {
        ClassDeclaration classDeclaration  = getClassDeclaration(parserResult, offset);
        return classDeclaration != null;
    }
    
    public static ClassDeclaration getClassDeclaration(PHPParseResult parserResult, int offset) {
        Model model = parserResult.getModel();
        Collection<? extends ClassScope> classes = ModelUtils.getDeclaredClasses(model.getFileScope());
        for(ClassScope classScope : classes) {
            OffsetRange range = classScope.getBlockRange();
            List<ASTNode> nodes = NavUtils.underCaret(parserResult, range.getStart());
            for(ASTNode node : nodes) {
                if (node instanceof ClassDeclaration && 
                        node.getStartOffset() < offset && 
                        node.getEndOffset() > offset) {
                    return (ClassDeclaration) node;
                }
            }
        }
        return null;
    }
    
    @Deprecated
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
            if (superClassElement != null) {
                classes.add(superClassElement);
                classes.addAll(getSuperClasses(parserResult, superClassName));
            }
        }

        return classes;
    }
    
    public static OffsetRange getFullOffsetRange(ASTNode node) {
        int start = node.getStartOffset();
        int end = node.getEndOffset();
        return new OffsetRange(start, end);
    }
    
    public static OffsetRange getFullOffsetRange(ForEachStatement node) {
        int start = node.getStartOffset();
        Statement body = node.getStatement();
        int end = body.getEndOffset();
        return new OffsetRange(start, end);
    }

    public static OffsetRange getFullOffsetRange(ForStatement node) {
        int start = node.getStartOffset();
        Statement body = node.getBody();
        int end = body.getEndOffset();
        return new OffsetRange(start, end);
    }

    public static OffsetRange getFullOffsetRange(WhileStatement node) {
        int start = node.getStartOffset();
        Statement body = node.getBody();
        int end = body.getEndOffset();
        return new OffsetRange(start, end);
    }
    
    public static OffsetRange getFullOffsetRange(DoStatement node) {
        int start = node.getStartOffset();
        Statement body = node.getBody();
        int end = body.getEndOffset();
        return new OffsetRange(start, end);
    }
    
    public static OffsetRange getFullOffsetRange(IfStatement node) {
        int start = node.getStartOffset();
        int end = node.getEndOffset();
        if (node.getFalseStatement() instanceof IfStatement) {
            end = ((IfStatement) node.getFalseStatement()).getEndOffset();
        }
        else if (node.getTrueStatement() instanceof IfStatement) {
            end = ((IfStatement) node.getTrueStatement()).getEndOffset();
        }
            
        return new OffsetRange(start, end);
    }
    
}
