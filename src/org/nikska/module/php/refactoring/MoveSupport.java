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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.Icon;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.UiUtils;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.QuerySupportFactory;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.model.FindUsageSupport;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelFactory;
import org.netbeans.modules.php.editor.model.Occurence;
import org.netbeans.modules.php.editor.model.TypeScope;
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

    private PhpElementKind kind;
    private ModelElement modelElement;
    private Set<Modifier> modifier;
    private final int offset;
    private final PHPParseResult sourceResult;
    private final Results results;
    private final OffsetRange offsetRange;
    private final ElementQuery.Index index;
    private ClassDeclaration classDeclaration;
    private String newName;
    private final Set<PhpElement> variableUsedInMoveScope;
    private final Set<PhpElement> variableBeforeMoveScope;
    private final Set<PhpElement> variableAfterMoveScope;
    private final Set<PhpElement> variableAssignedInMoveScope;
    private final List<ASTNode> nodes;
    
    public static final String TYPE_METHOD = "Method";
    public static final String TYPE_FUNCTION = "Function";
    public static final String TYPE_NEW_FILE = "New File";

    private MoveSupport(PHPParseResult result, int offset, OffsetRange offsetRange) {
        this.sourceResult = result;
        this.offset = offset;
        this.results = new Results();
        FileObject fo = result.getSnapshot().getSource().getFileObject();
        this.offsetRange = offsetRange;
        this.index = ElementQueryFactory.createIndexQuery(QuerySupportFactory.getDependent(fo));
        initClassElement();
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

    public void setNameName(String newName) {
        this.newName = newName;
    }

    public String getNewName() {
        return newName;
    }

    private void initClassElement() {
        List<ASTNode> nodes = NavUtils.underCaret(sourceResult, offset);
        for (ASTNode node : nodes) {
            if (node instanceof ClassDeclaration && node.getEndOffset() != offset) {
                classDeclaration = (ClassDeclaration) node;
            }
        }
    }

    public ClassDeclaration getClassDeclaration() {
        return classDeclaration;
    }

    public PHPParseResult getParseResult() {
        return sourceResult;
    }

    public int getBegin() {
        return this.offsetRange.getStart();
    }

    public int getEnd() {
        return this.offsetRange.getEnd();
    }

    void setModelElement(ModelElement modelElement) {
        this.modelElement = modelElement;
    }

    public String getName() {
        return modelElement.getName();
    }

    public FileObject getSourceFileObject() {
        return sourceResult.getSnapshot().getSource().getFileObject();
    }

    public int getOffset() {
        return offset;
    }

    public PhpElementKind getKind() {
        return kind;
    }

    public PhpElementKind getPhpElementKind() {
        return modelElement.getPhpElementKind();
    }

    public Set<Modifier> getModifiers() {
        ModelElement attributeElement = getModelElement();
        return getModifiers(attributeElement);
    }

    public static MoveSupport getInstance(ElementQuery.Index index, final PHPParseResult info, final int offset, OffsetRange offsetRange) {
        return new MoveSupport(info, offset, offsetRange);
    }

    public ModelElement getModelElement() {
        return modelElement;
    }

    public List<ModelElement> getModelElements() {
        return new ArrayList<>();
    }

    private Set<Modifier> getModifiers(ModelElement mElement) {
        if (modifier == null) {
            Set<Modifier> retval = Collections.emptySet();
            if (mElement != null && mElement.getInScope() instanceof TypeScope) {
                retval = EnumSet.noneOf(Modifier.class);
                if (mElement.getPhpModifiers().isPrivate()) {
                    retval.add(Modifier.PRIVATE);
                } else if (mElement.getPhpModifiers().isProtected()) {
                    retval.add(Modifier.PROTECTED);
                }
                if (mElement.getPhpModifiers().isPublic()) {
                    retval.add(Modifier.PUBLIC);
                }
                if (mElement.getPhpModifiers().isStatic()) {
                    retval.add(Modifier.STATIC);
                }
            }
            modifier = retval;
        }
        return modifier;
    }

    public static boolean isAlreadyInResults(ASTNode node, Set<ASTNode> results) {
        OffsetRange newOne = new OffsetRange(node.getStartOffset(), node.getEndOffset());
        for (Iterator<ASTNode> it = results.iterator(); it.hasNext();) {
            ASTNode aSTNode = it.next();
            OffsetRange oldOne = new OffsetRange(aSTNode.getStartOffset(), aSTNode.getEndOffset());
            if (newOne.containsInclusive(oldOne.getStart()) || oldOne.containsInclusive(newOne.getStart())) {
                return true;
            }
        }
        return false;
    }

    private ElementKind getElementKind() {
        return ElementKind.OTHER;
    }

    Results getResults() {
        return results;
    }

    public String getParameters() {
        String parameters = "";
        boolean hasParameter = false;
        for (PhpElement element : variableUsedInMoveScope) {
            if (variableBeforeMoveScope.contains(element)) {
                if (hasParameter) {
                    parameters += ", ";
                }
                parameters += element.getName();
                hasParameter = true;
            }
        }
        return parameters;
    }

    public String getReturnsAssignment() {
        String returns = "";
        boolean hasReturns = false;
        int countReturn = 0;
        for (PhpElement element : variableAssignedInMoveScope) {
            if (variableAfterMoveScope.contains(element)) {
                if (hasReturns) {
                    returns += ", ";
                }
                returns += element.getName();
                hasReturns = true;
                countReturn++;
            }
        }

        if (countReturn > 1) {
            returns = "list(" + returns + ")";
        }

        return returns + " = ";
    }

    public String getReturns() {
        String returns = "";
        boolean hasReturns = false;
        int countReturn = 0;
        for (PhpElement element : variableAssignedInMoveScope) {
            if (variableAfterMoveScope.contains(element)) {
                if (hasReturns) {
                    returns += ", ";
                }
                returns += element.getName();
                hasReturns = true;
                countReturn++;
            }
        }

        if (countReturn > 1) {
            returns = "array(" + returns + ")";
        }

        return "return " + returns + ";";
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
                } else if (firstOccurence != null
                        && !firstOccurence.getOccurenceRange().equals(occurence.getOccurenceRange())
                        && occurence.getOccurenceRange().getEnd() < firstOccurence.getOccurenceRange().getStart()) {
                    firstOccurence = occurence;
                }
            }
        }

        return firstOccurence;
    }

    private boolean isInBlock(OffsetRange occurenceRange, OffsetRange blockOffsetRange) {
        if (occurenceRange.getEnd() > blockOffsetRange.getStart()
                && occurenceRange.getStart() < blockOffsetRange.getEnd()) {
            return true;
        }
        return false;
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

        if (firstOccurence.getOccurenceRange().equals(firstScopeElementRange)) {
            return true;
        }

        return false;
    }

    private boolean addToVariableUsedBlock(Collection<Occurence> occurences, OffsetRange offsetRange) {
        Occurence firstOccuranceInBlock = getFirstOccuranceInBlock(occurences, offsetRange);

        if (firstOccuranceInBlock == null) {
            return false;
        }
        return !isFirstOccuranceAssignment(occurences, offsetRange);
    }

    public final class Results {

        Collection<MoveElement> elements = new TreeSet<>(new Comparator<MoveElement>() {

            @Override
            public int compare(MoveElement o1, MoveElement o2) {
                String path1 = o1.getFile() != null ? o1.getFile().getPath() : ""; //NOI18N
                String path2 = o2.getFile() != null ? o2.getFile().getPath() : ""; //NOI18N
                int retval = path1.compareTo(path2);
                if (retval == 0) {
                    int offset1 = o1.getPosition().getBegin().getOffset();
                    int offset2 = o2.getPosition().getBegin().getOffset();
                    retval = offset1 < offset2 ? -1 : 1;
                }
                return retval;
            }
        });

        private Results() {
        }

        public void addEntry(FileObject fileObject) {
            Icon icon = UiUtils.getElementIcon(MoveSupport.this.getElementKind(), Collections.<Modifier>emptyList());
            MoveElement moveElement = MoveElement.create("Name",
                    sourceResult,
                    new OffsetRange(offsetRange.getStart(), offsetRange.getStart()),
                    icon);
            if (moveElement != null) {
                elements.add(moveElement);
            }
        }

        public Collection<MoveElement> getResultElements() {
            return Collections.unmodifiableCollection(elements);
        }
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
