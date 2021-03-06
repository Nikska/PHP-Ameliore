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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.QuerySupportFactory;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.model.FindUsageSupport;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelFactory;
import org.netbeans.modules.php.editor.model.Occurence;
import org.netbeans.modules.php.editor.model.VariableName;
import org.netbeans.modules.php.editor.model.VariableScope;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.netbeans.modules.php.editor.model.Scope;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;

/**
 *
 * @author Loïc Laverdant
 */
@ActionReferences({
    @ActionReference(id = @ActionID(category = "Refactoring", id = "org.nikska.modules.refactoring.api.ui.MoveAction"), path = "Loaders/text/x-php5/Actions", position = 1700)
})
public final class MoveSupport {
    public static final String TYPE_FUNCTION = "Function";
    public static final String TYPE_NEW_FILE = "New File";
    public static final String TYPE_PARENT_METHOD = "Parent method";
    public static final String TYPE_METHOD = "Method";

    @Deprecated
    private final PHPParseResult sourceResult;
    private final OffsetRange offsetRange;
    private final ElementQuery.Index index;
    private final Set<VariableName> variableUsedInMoveScope;
    private final Set<VariableName> variableBeforeMoveScope;
    private final Set<VariableName> variableAfterMoveScope;
    private final Set<VariableName> variableAssignedInMoveScope;
    private final List<ASTNode> nodes;

    private MoveSupport(PHPParseResult result, OffsetRange offsetRange) {
        this.sourceResult = result;
        FileObject fo = result.getSnapshot().getSource().getFileObject();
        this.offsetRange = offsetRange;
        this.index = ElementQueryFactory.createIndexQuery(QuerySupportFactory.getDependent(fo));

        Model model = ModelFactory.getModel(result);
        variableUsedInMoveScope = new HashSet<>();
        variableBeforeMoveScope = new HashSet<>();
        variableAfterMoveScope = new HashSet<>();
        variableAssignedInMoveScope = new HashSet<>();

        this.nodes = NavUtils.underCaret(result, offsetRange.getStart());

        VariableScope variableScope = model.getVariableScope(offsetRange.getStart());
        if (variableScope != null) {
            Collection<? extends VariableName> declaredVariables = variableScope.getDeclaredVariables();
            for (VariableName varName : declaredVariables) {
                FindUsageSupport usageSupport = FindUsageSupport.getInstance(index, varName);
                Collection<Occurence> occurences = usageSupport.occurences(variableScope.getFileObject());
                if (occurences != null) {
                    boolean addToVariableUsedBlock = addToVariableUsedBlock(occurences, offsetRange);
                    for (Occurence occurence : occurences) {

                        //Recherche les variables du block move
                        if (isInBlock(occurence.getOccurenceRange(), offsetRange) && addToVariableUsedBlock) {
                            variableUsedInMoveScope.add(varName);
                        }
                        //Recherche les variables avant le block move
                        if (occurence.getOccurenceRange().getEnd() < offsetRange.getStart()) {
                            variableBeforeMoveScope.add(varName);
                        }
                        //Recherche les variables après le block move
                        if (occurence.getOccurenceRange().getStart() > offsetRange.getEnd()) {
                            variableAfterMoveScope.add(varName);
                        }
                        if (isInBlock(occurence.getOccurenceRange(), offsetRange) && isAssignedInBlock(occurence, offsetRange)) {
                            variableAssignedInMoveScope.add(varName);
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    public PHPParseResult getParseResult() {
        return sourceResult;
    }

    public int getBegin() {
        return this.offsetRange.getStart();
    }

    public int getEnd() {
        return this.offsetRange.getEnd();
    }

    public FileObject getSourceFileObject() {
        return sourceResult.getSnapshot().getSource().getFileObject();
    }

    public static MoveSupport getInstance(final PHPParseResult info, OffsetRange offsetRange) {
        return new MoveSupport(info, offsetRange);
    }

    public Set<VariableName> getParameters() {
        Set<VariableName> elements = new HashSet<>();
        for (VariableName element : variableUsedInMoveScope) {
            if (variableBeforeMoveScope.contains(element)) {
                elements.add(element);
            }
        }
        return elements;
    }

    public Set<VariableName> getReturnsAssignment() {
        Set<VariableName> elements = new HashSet<>();
        for (VariableName element : variableAssignedInMoveScope) {
            if (variableAfterMoveScope.contains(element)) {
                elements.add(element);
            }
        }

        return elements;
    }
    
    public Set<VariableName> getReturns() {
        Set<VariableName> elements = new HashSet<>();
        for (VariableName element : variableAssignedInMoveScope) {
            if (variableAfterMoveScope.contains(element)) {
                elements.add(element);
            }
        }

        return elements;
    }

    private boolean isAssignedInBlock(Occurence occurence, OffsetRange offsetRange) {

        //Recherche les assignement
        List<? extends ModelElement> scopeElements = null;
        for (PhpElement declaration : occurence.getAllDeclarations()) {
            if (declaration instanceof Scope) {
                scopeElements = ((Scope) declaration).getElements();
            }
            break;
        }

        if (scopeElements == null || scopeElements.isEmpty()) {
            return false;
        }

        for (ModelElement scopeElement : scopeElements) {
            OffsetRange scopeElementRange = scopeElement.getOffsetRange(sourceResult);
            if (isInBlock(scopeElementRange, offsetRange)) {
                if (scopeElement.getName().startsWith("$")) {
                    scopeElementRange = new OffsetRange(scopeElementRange.getStart() + 1, scopeElementRange.getEnd());
                }

                if (occurence.getOccurenceRange().equals(scopeElementRange)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Occurence getFirstOccuranceInBlock(Collection<Occurence> occurences, OffsetRange offsetRange) {
        Occurence firstOccurence = null;
        for (Occurence occurence : occurences) {
            if (isInBlock(occurence.getOccurenceRange(), offsetRange)) {

                if (firstOccurence == null) {
                    firstOccurence = occurence;
                } else if (!firstOccurence.getOccurenceRange().equals(occurence.getOccurenceRange())
                        && occurence.getOccurenceRange().getEnd() < firstOccurence.getOccurenceRange().getStart()) {
                    firstOccurence = occurence;
                }
            }
        }

        return firstOccurence;
    }

    private boolean isInBlock(OffsetRange occurenceRange, OffsetRange blockOffsetRange) {
        return occurenceRange.getEnd() > blockOffsetRange.getStart()
                && occurenceRange.getStart() < blockOffsetRange.getEnd();
    }

    private boolean isFirstOccuranceAssignment(Collection<Occurence> occurences, OffsetRange offsetRange) {
        Occurence firstOccurence = getFirstOccuranceInBlock(occurences, offsetRange);

        //Recherche les assignement
        List<? extends ModelElement> scopeElements = null;
        for (Occurence occurence : occurences) {
            for (PhpElement declaration : occurence.getAllDeclarations()) {
                if (declaration instanceof Scope) {
                    scopeElements = ((Scope) declaration).getElements();
                }
                break;
            }
            break;
        }

        if (scopeElements == null || scopeElements.isEmpty()) {
            return false;
        }

        OffsetRange firstScopeElementRange = null;
        for (ModelElement scopeElement : scopeElements) {
            OffsetRange scopeElementRange = scopeElement.getOffsetRange(sourceResult);
            if (isInBlock(scopeElementRange, offsetRange)) {
                if (scopeElement.getName().startsWith("$")) {
                    scopeElementRange = new OffsetRange(scopeElementRange.getStart() + 1, scopeElementRange.getEnd());
                }
                if (firstScopeElementRange == null) {
                    firstScopeElementRange = scopeElementRange;
                } else if (firstScopeElementRange.getStart() > scopeElementRange.getEnd()) {
                    firstScopeElementRange = scopeElementRange;
                }
            }
        }

        return firstOccurence.getOccurenceRange().equals(firstScopeElementRange);
    }

    private boolean addToVariableUsedBlock(Collection<Occurence> occurences, OffsetRange offsetRange) {
        Occurence firstOccuranceInBlock = getFirstOccuranceInBlock(occurences, offsetRange);

        if (firstOccuranceInBlock == null) {
            return false;
        }
        return !isFirstOccuranceAssignment(occurences, offsetRange);
    }

    public boolean isInMethod() {
        for (ASTNode node : nodes) {
            if (node instanceof ClassDeclaration && node.getStartOffset() != offsetRange.getStart()) {
                return true;
            }
        }
        return false;
    }

    public boolean isInFunction() {
        for (ASTNode node : nodes) {
            if (node instanceof FunctionDeclaration && node.getStartOffset() != offsetRange.getStart() && !isInMethod()) {
                return true;
            }
        }
        return false;
    }
}
