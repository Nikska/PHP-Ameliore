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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.UiUtils;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.QuerySupportFactory;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.model.FindUsageSupport;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelFactory;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.Occurence;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.model.VariableName;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

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
    //private final Set<ModelElement> declarations;
    private ModelElement modelElement;
    private Set<Modifier> modifier;
    private ElementQuery.Index idx;
    private EditorCookie editorCookie;
    private int offset;
    private PHPParseResult result;
    private final Results results;
    private final OffsetRange offsetRange;
    //private final Document document;
    //private final int caretStart;
    //private final int caretEnd;

    private MoveSupport(ElementQuery.Index idx, PHPParseResult result, int offset, OffsetRange offsetRange, FileObject fo) {
        this.result = result;
        this.offset = offset;
        this.results = new Results();
        this.fo = fo;
        this.offsetRange = offsetRange;
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

    /*public ElementKind getElementKind() {
        return modelElement.getPHPElement().getKind();
    }*/

    public PhpElementKind getPhpElementKind() {
        return modelElement.getPhpElementKind();
    }

    public Set<Modifier> getModifiers() {
        ModelElement attributeElement = getModelElement();
        return getModifiers(attributeElement);
    }

    private static Occurence findOccurence(final Model model, final int offset) {
        Occurence result = model.getOccurencesSupport(offset).getOccurence();
        if (result == null) {
            result = model.getOccurencesSupport(offset + "$".length()).getOccurence(); //NOI18N
        }
        return result;
    }
    
    public static MoveSupport getInstance(final PHPParseResult info, final int offset, OffsetRange offsetRange) {
        
        Model model = ModelFactory.getModel(info);
        model.getOccurencesSupport(offsetRange);
        final Occurence occurence = findOccurence(model, offset);
        final Set<ModelElement> declarations = new HashSet<>();
        final Collection<? extends PhpElement> allDeclarations = occurence != null ? occurence.getAllDeclarations() : Collections.<PhpElement>emptyList();
        boolean canContinue = occurence != null && allDeclarations.size() > 0 && allDeclarations.size() < 5;
        if (canContinue && occurence != null && EnumSet.of(Occurence.Accuracy.EXACT, Occurence.Accuracy.MORE, Occurence.Accuracy.UNIQUE).contains(occurence.degreeOfAccuracy())) {
            FileObject parserFo = info.getSnapshot().getSource().getFileObject();
            for (final PhpElement declarationElement : allDeclarations) {
                try {
                    final FileObject fileObject = declarationElement.getFileObject();
                    if (fileObject != null && parserFo != fileObject) {
                        ParserManager.parse(Collections.singleton(Source.create(fileObject)), new UserTask() {

                            @Override
                            public void run(ResultIterator resultIterator) throws Exception {
                                Result parserResult = resultIterator.getParserResult();
                                if (parserResult != null && parserResult instanceof PHPParseResult) {
                                    Model modelForDeclaration = ModelFactory.getModel((PHPParseResult) parserResult);
                                    declarations.add(modelForDeclaration.findDeclaration(declarationElement));
                                }
                            }
                        });
                    } else {
                        declarations.add(model.findDeclaration(declarationElement));
                    }
                } catch (ParseException ex) {
                    Exceptions.printStackTrace(ex);
                    return null;
                }
            }
        }
        
        
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
