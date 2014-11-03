/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
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
import javax.swing.Icon;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.UiUtils;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
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
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.netbeans.modules.php.editor.model.Scope;

/**
 *
 * @author Radek Matous
 */
@ActionReferences({
    @ActionReference(id = @ActionID(category = "Refactoring", id = "org.nikska.modules.refactoring.api.ui.MoveAction"), path = "Loaders/text/x-php5/Actions", position = 1700)
})
public final class MoveSupport {

    private ASTNode node;
    private FileObject fo;
    private PhpElementKind kind;
    private ModelElement modelElement;
    private Set<Modifier> modifier;
    private int offset;
    private PHPParseResult result;
    private final Results results;
    private final OffsetRange offsetRange;
    private ElementQuery.Index index;
    private ClassDeclaration classDeclaration;
    private String newName;
    private final Set<PhpElement> variableInMoveScope;
    private final Set<PhpElement> variableBeforeMoveScope;
    private final Set<PhpElement> variableAfterMoveScope;

    private MoveSupport(ElementQuery.Index idx, PHPParseResult result, int offset, OffsetRange offsetRange, FileObject fo) {
        this.result = result;
        this.offset = offset;
        this.results = new Results();
        this.fo = fo;
        this.offsetRange = offsetRange;
        this.index = idx;
        initClassElement();
        Model model = ModelFactory.getModel(result);
        variableInMoveScope = new HashSet<>();
        variableBeforeMoveScope = new HashSet<>();
        variableAfterMoveScope = new HashSet<>();
        
        VariableScope variableScope = model.getVariableScope(offsetRange.getStart());
        if (variableScope != null) {
            Collection<? extends VariableName> declaredVariables = variableScope.getDeclaredVariables();
            for (VariableName varName : declaredVariables) {
                FindUsageSupport usageSupport = FindUsageSupport.getInstance(index, varName);
                Collection<Occurence> occurences = usageSupport.occurences(variableScope.getFileObject());
                if (occurences != null) {
                    boolean addToVariableBlock = addToVariableBlock(occurences, offsetRange);
                    for (Occurence occurence : occurences) {
                        
                        //Recherche les variables du block move
                        if (isInBlock(occurence.getOccurenceRange(), offsetRange) && addToVariableBlock) {
                            variableInMoveScope.add(varName);
                        }
                        //Recherche les variables avant le block move
                        if (occurence.getOccurenceRange().getEnd() < offsetRange.getStart()) {
                            variableBeforeMoveScope.add(varName);
                        }
                        //Recherche les variables après le block move
                        if (occurence.getOccurenceRange().getStart() > offsetRange.getEnd()) {
                            variableAfterMoveScope.add(varName);
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
    
    private void initClassElement()
    {
        List<ASTNode> nodes = NavUtils.underCaret(result, offset);
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
        return result;
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

    public ASTNode getASTNode() {
        return node;
    }

    public FileObject getDeclarationFileObject() {
        return fo;
    }
    
    private FileObject getFileObject(Lookup lookup) {
        Collection<? extends Node> nodes = lookup.lookupAll(Node.class);
        Node n = (nodes.size() == 1) ? nodes.iterator().next() : null;
        DataObject dob = (n != null) ? n.getLookup().lookup(DataObject.class) : null;
        return (dob != null) ? dob.getPrimaryFile() : null;
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

    private static Occurence findOccurence(final Model model, final OffsetRange offsetRange) {
        Occurence result = model.getOccurencesSupport(offsetRange).getOccurence();
        if (result == null) {
            //result = model.getOccurencesSupport(offset + "$".length()).getOccurence(); //NOI18N
        }
        return result;
    }
    
    public static MoveSupport getInstance(ElementQuery.Index index, final PHPParseResult info, final int offset, OffsetRange offsetRange) {
        FileObject fileObject = info.getSnapshot().getSource().getFileObject();
        final Index indexQuery = ElementQueryFactory.createIndexQuery(QuerySupportFactory.getDependent(fileObject));
        return new MoveSupport(indexQuery, info, offset, offsetRange, fileObject);
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
        for (PhpElement element : variableInMoveScope) {
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

    private Occurence getFirstOccuranceInBlock(Collection<Occurence> occurences, OffsetRange offsetRange) {
        Occurence firstOccurence = null;
        for (Occurence occurence : occurences) {
            if (isInBlock(occurence.getOccurenceRange(), offsetRange)) {
                
                if (firstOccurence == null) {
                    firstOccurence = occurence;
                }
                else if (firstOccurence != null && 
                    !firstOccurence.getOccurenceRange().equals(occurence.getOccurenceRange()) &&
                    occurence.getOccurenceRange().getEnd() < firstOccurence.getOccurenceRange().getStart()) {
                    firstOccurence = occurence;
                }
            }
        }
        
        return firstOccurence;
    }
    
    private boolean isInBlock(OffsetRange occurenceRange, OffsetRange blockOffsetRange) {
        if (occurenceRange.getEnd() > blockOffsetRange.getStart() &&
            occurenceRange.getStart() < blockOffsetRange.getEnd()) {
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
            OffsetRange scopeElementRange = scopeElement.getOffsetRange(result);
            if (isInBlock(scopeElementRange, offsetRange)) {
                if (scopeElement.getName().startsWith("$")) {
                    scopeElementRange = new OffsetRange(scopeElementRange.getStart() + 1, scopeElementRange.getEnd());
                }
                if (firstScopeElementRange == null) {
                    firstScopeElementRange = scopeElementRange;
                }
                else if(firstScopeElementRange.getStart() > scopeElementRange.getEnd()) {
                    firstScopeElementRange = scopeElementRange;
                }
            }
        }
        
        if (firstOccurence.getOccurenceRange().equals(firstScopeElementRange)) {
            return true;
        }
        
        return false;
    }

    private boolean addToVariableBlock(Collection<Occurence> occurences, OffsetRange offsetRange) {
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

        private void clear() {
            elements.clear();
        }

        public void addEntry(FileObject fileObject) {
            Icon icon = UiUtils.getElementIcon(MoveSupport.this.getElementKind(), Collections.<Modifier>emptyList());
            MoveElement moveElement = MoveElement.create(
                    "Name",
                    fileObject,
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
}
