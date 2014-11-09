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
package org.nikska.module.php.editor.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.api.elements.ElementFilter;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ClassInstanceCreation;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.MethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceName;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultTreePathVisitor;
import org.netbeans.modules.php.editor.verification.CustomisableRule;
import org.netbeans.modules.php.editor.verification.HintRule;
import org.netbeans.modules.php.editor.verification.PHPRuleContext;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;

/**
 * @author Loïc Laverdant
 */
public class UndeclaredHint extends HintRule implements CustomisableRule {

    private static final String HINT_ID = "Undeclared.Function.Hint"; //NOI18N
    private final String CHECK_UNDECLARED_FUNCTION = "php.verification.check.undeclared.function";
    private final Boolean CHECK_UNDECLARED_FUNCTION_DEFAULT = false;
    private final String CHECK_UNDECLARED_CLASS = "php.verification.check.undeclared.class";
    private final Boolean CHECK_UNDECLARED_CLASS_DEFAULT = true;
    private Preferences preferences;

    @Override
    public String getId() {
        return HINT_ID; //NOI18N
    }

    @Override
    @Messages("UndeclaredHintDesc=Undeclared Type function or class")
    public String getDescription() {
        return Bundle.UndeclaredHintDesc();
    }

    @Override
    @Messages("UndeclaredHintDispName=Undeclared Type")
    public String getDisplayName() {
        return Bundle.UndeclaredHintDispName();
    }

    @Override
    public HintSeverity getDefaultSeverity() {
        return HintSeverity.ERROR;
    }

    @Override
    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() == null) {
            return;
        }
        FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
        if (fileObject != null) {
            final Model model = phpParseResult.getModel();
            UndeclaredFunctionFixVisitor undeclaredFunctionFixVisitor = new UndeclaredFunctionFixVisitor(model, fileObject, context.doc);
            phpParseResult.getProgram().accept(undeclaredFunctionFixVisitor);
            hints.addAll(undeclaredFunctionFixVisitor.getHints());
        }
    }

    @Override
    public JComponent getCustomizer(Preferences preferences) {
        JComponent customizer = new UndeclaredHintCustomizer(preferences, this);
        setCheckUndeclaredFunction(preferences, checkUndeclaredFunction(preferences));
        setCheckUndeclaredClass(preferences, checkUndeclaredClass(preferences));
        return customizer;
    }

    public void setCheckUndeclaredFunction(Preferences preferences, boolean isEnabled) {
        preferences.putBoolean(CHECK_UNDECLARED_FUNCTION, isEnabled);
    }

    public boolean checkUndeclaredFunction(Preferences preferences) {
        return preferences.getBoolean(CHECK_UNDECLARED_FUNCTION, CHECK_UNDECLARED_FUNCTION_DEFAULT);
    }

    public void setCheckUndeclaredClass(Preferences preferences, boolean isEnabled) {
        preferences.putBoolean(CHECK_UNDECLARED_CLASS, isEnabled);
    }

    public boolean checkUndeclaredClass(Preferences preferences) {
        return preferences.getBoolean(CHECK_UNDECLARED_CLASS, CHECK_UNDECLARED_CLASS_DEFAULT);
    }

    private class UndeclaredFunctionFixVisitor extends DefaultTreePathVisitor {

        private final FileObject fileObject;
        private final BaseDocument baseDocument;
        private final List<Hint> hints;
        private final Model model;

        public UndeclaredFunctionFixVisitor(Model model, FileObject fileObject, BaseDocument baseDocument) {
            this.fileObject = fileObject;
            this.baseDocument = baseDocument;
            this.model = model;
            hints = new ArrayList<>();
        }

        public List<Hint> getHints() {
            return hints;
        }

        private void createHint(ASTNode node, String message, OffsetRange offsetRange) {
            if (showHint(offsetRange, baseDocument)) {
                hints.add(
                        new Hint(UndeclaredHint.this,
                                message,
                                fileObject, offsetRange,
                                getHintFix(), 500));
            }
        }

        private void createHint(ASTNode node, String message) {
            OffsetRange offsetRange = new OffsetRange(node.getStartOffset(), node.getEndOffset());
            createHint(node, message, offsetRange);
        }

        private List<HintFix> getHintFix() {
            return null;
        }

        @Override
        @Messages("UndeclaredFunctionHintCustom=Function seems to be undeclared in its scope")
        public void visit(MethodInvocation methodInvocation) {
            String methName = CodeUtils.extractFunctionName(methodInvocation.getMethod());
            if (hasText(methName)) {
                Collection<? extends TypeScope> allTypes = ModelUtils.resolveType(model, methodInvocation);
                if (allTypes.size() == 1) {
                    TypeScope type = ModelUtils.getFirst(allTypes);
                    ElementQuery.Index index = model.getIndexScope().getIndex();
                    Set<MethodElement> allMethods = ElementFilter.forName(NameKind.exact(methName)).filter(index.getAllMethods(type));
                    if (allMethods.isEmpty()) {
                        createHint(methodInvocation, Bundle.UndeclaredFunctionHintCustom());
                    }
                }
            }
            super.visit(methodInvocation);
        }

        @Override
        @Messages("UndeclaredClassHintCustom=Class seems to be undeclared")
        public void visit(ClassInstanceCreation instanceCreation) {
            if (checkUndeclaredClass(preferences)) {
                String clzName = CodeUtils.extractClassName(instanceCreation.getClassName());
                clzName = (clzName != null && clzName.trim().length() > 0) ? clzName : null;
                ElementQuery.Index index = model.getIndexScope().getIndex();
                Set<ClassElement> classes = Collections.emptySet();
                if (hasText(clzName)) {
                    classes = index.getClasses(NameKind.exact(clzName));
                }
                if (clzName != null && classes.isEmpty()) {
                    createHint(instanceCreation, Bundle.UndeclaredClassHintCustom());
                }
            }
            super.visit(instanceCreation);
        }

        @Override
        @Messages("UndeclaredDeclationClassHintCustom=Class seems to be undeclared")
        public void visit(ClassDeclaration instanceDeclaration) {
            if (checkUndeclaredClass(preferences)) {
                NamespaceName superClass = (NamespaceName) instanceDeclaration.getSuperClass();
                if (superClass != null && !superClass.getSegments().isEmpty()) {
                    ElementQuery.Index index = model.getIndexScope().getIndex();

                    //Recherche le nom de la superClass
                    Identifier className = superClass.getSegments().get(superClass.getSegments().size() - 1);
                    String clzName = className.getName();
                    clzName = (clzName != null && clzName.trim().length() > 0) ? clzName : null;

                    Set<ClassElement> classes = Collections.emptySet();
                    if (hasText(clzName)) {
                        classes = index.getClasses(NameKind.exact(clzName));
                    }
                    if (clzName != null && classes.isEmpty()) {
                        createHint(superClass, Bundle.UndeclaredClassHintCustom());
                    }

                }
            }
            super.visit(instanceDeclaration);
        }

        public boolean hasText(String input) {
            return input != null && !input.trim().isEmpty();
        }

        private ClassElement getIndexedClass(String name) {
            ClassElement retval = null;
            ElementQuery.Index index = model.getIndexScope().getIndex();
            Collection<ClassElement> classes = Collections.emptyList();
            if ("self".equals(name) || "parent".equals(name)) { //NOI18N
                ClassDeclaration classDeclaration = null;
                for (ASTNode aSTNode : getPath()) {
                    if (aSTNode instanceof ClassDeclaration) {
                        classDeclaration = (ClassDeclaration) aSTNode;
                        break;
                    }
                }
                if (classDeclaration != null) {
                    String clzName = CodeUtils.extractClassName(classDeclaration);
                    classes = index.getClasses(NameKind.exact(clzName));
                }
            } else {
                classes = index.getClasses(NameKind.exact(name));
            }
            if (classes.size() == 1) {
                retval = classes.iterator().next();
                if ("parent".equals(name)) {
                    QualifiedName superClassQualifiedName = retval.getSuperClassName();
                    if (superClassQualifiedName != null) {
                        String superClassName = superClassQualifiedName.getName();
                        if (superClassName != null) {
                            classes = index.getClasses(NameKind.exact(superClassName));
                            retval = (classes.size() == 1) ? classes.iterator().next() : null;
                        }
                    }
                }
            }
            return retval;
        }
    }
}
