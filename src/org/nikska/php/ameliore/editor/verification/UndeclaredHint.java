/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nikska.php.ameliore.editor.verification;
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
 *
 * @author nikska
 */
public class UndeclaredHint extends HintRule implements CustomisableRule {
    
    private static final String HINT_ID = "Undeclared.Function.Hint"; //NOI18N
    private String CHECK_UNDECLARED_FUNCTION = "php.verification.check.undeclared.function";
    private Boolean CHECK_UNDECLARED_FUNCTION_DEFAULT = false;
    private String CHECK_UNDECLARED_CLASS = "php.verification.check.undeclared.class";
    private Boolean CHECK_UNDECLARED_CLASS_DEFAULT = true;
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
        //private IntroduceFix fix;

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
            /*IntroduceFix variableFix = getIntroduceFix();
            if (variableFix != null) {
                return Collections.<HintFix>singletonList(variableFix);
            }
            
            return null;*/
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
                        /*assert type != null;
                        FileObject fileObject = type.getFileObject();
                        BaseDocument document = fileObject != null ? GsfUtilities.getDocument(fileObject, true) : null;
                        if (document != null && fileObject.canWrite()) {
                            fix = new IntroduceMethodFix(document, methodInvocation, type);
                        }*/
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
                    /*ClassElement clz = getIndexedClass(clzName);
                    if (clz == null) {
                        fix = IntroduceSuggestion.IntroduceClassFix.getInstance(clzName, model, instanceCreation);
                    }*/
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
                        /*ClassElement clz = getIndexedClass(clzName);
                         if (clz == null) {
                         fix = IntroduceSuggestion.IntroduceClassFix.getInstance(clzName, model, instanceCreation);
                         }*/
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

        /**
         * @return or null
         */
        /*public IntroduceFix getIntroduceFix() {
            return null;//fix;
        }*/
    }

    /*private static class IntroduceMethodFix extends IntroduceFix {
        private final TypeScope type;
        private final PHPCompletionItem.MethodDeclarationItem item;
        private static final String UNKNOWN_FILE_NAME = "?"; //NOI18N

        public IntroduceMethodFix(BaseDocument doc, MethodInvocation node, TypeScope type) {
            super(doc, node);
            this.type = type;
            this.item = createMethodDeclarationItem(type, node);
        }

        @Override
        public void implement() throws Exception {
            int templateOffset = getOffset();
            EditList edits = new EditList(doc);
            edits.replace(templateOffset, 0, "\n" + item.getCustomInsertTemplate(), true, 0); //NOI18N
            edits.apply();
            templateOffset = Utilities.getRowEnd(doc, templateOffset + 1);
            UiUtils.open(type.getFileObject(), Utilities.getRowEnd(doc, templateOffset + 1) - 1);
        }

        @Override
        @Messages("UndeclaredFunctionHintMethodDesc=Create Method in Class ()")
        public String getDescription() {
            String clsName = type.getName();
            FileObject fileObject = type.getFileObject();
            //String fileName = fileObject == null ? UNKNOWN_FILE_NAME : fileObject.getNameExt();
            //return Bundle.UndeclaredFunctionHintMethodDesc(item.getMethod().asString(BaseFunctionElement.PrintAs.NameAndParamsDeclaration), clsName, fileName);
            return Bundle.UndeclaredFunctionHintMethodDesc();
        }

        int getOffset() throws BadLocationException {
            return UndeclaredFunctionHint.getOffset(doc, type, PhpElementKind.METHOD);
        }
    }*/

    /*abstract static class IntroduceFix implements HintFix {

        BaseDocument doc;
        ASTNode node;

        public IntroduceFix(BaseDocument doc, ASTNode node) {
            this.doc = doc;
            this.node = node;
        }

        OffsetRange getOffsetRange() {
            return new OffsetRange(node.getStartOffset(), node.getEndOffset());
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean isSafe() {
            return true;
        }
    }*/

    /*private static String getParameters(final List<Expression> parameters) {
        StringBuilder paramNames = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            Expression expression = parameters.get(i);
            String varName = null;
            if (expression instanceof Variable) {
                varName = CodeUtils.extractVariableName((Variable) expression);
            }
            if (varName == null) {
                varName = String.format("$param%d", i); //NOI18N
            }
            if (i > 0) {
                paramNames.append(", ");
            }
            paramNames.append(varName);
        }
        return paramNames.toString();
    }

    private static int getOffset(BaseDocument doc, TypeScope typeScope, PhpElementKind kind) throws BadLocationException {
        int offset = -1;
        Collection<ModelElement> elements = new HashSet<>();
        elements.addAll(typeScope.getDeclaredConstants());
        switch (kind) {
            case METHOD:
                if (typeScope instanceof ClassScope) {
                    ClassScope clz = (ClassScope) typeScope;
                    elements.addAll(clz.getDeclaredFields());
                    elements.addAll(clz.getDeclaredMethods());
                }
                break;
            case FIELD:
                if ((typeScope instanceof ClassScope)) {
                    ClassScope clz = (ClassScope) typeScope;
                    elements.addAll(clz.getDeclaredFields());
                }
                break;
            default:
                assert false;
        }
        int newOffset;
        for (ModelElement elem : elements) {
            newOffset = elem.getOffset();
            if (elem instanceof MethodScope) {
                newOffset = getOffsetAfterBlockCloseCurly(doc, newOffset);
            } else {
                newOffset = getOffsetAfterNextSemicolon(doc, newOffset);
            }
            if (newOffset > offset) {
                offset = newOffset;
            }
        }
        if (offset == -1) {
            offset = getOffsetAfterClassOpenCurly(doc, typeScope.getOffset());
        }
        return offset;
    }

    private static int getOffsetAfterBlockCloseCurly(BaseDocument doc, int offset) throws BadLocationException {
        int retval = offset;
        doc.readLock();
        try {
            TokenSequence<? extends PHPTokenId> ts = LexUtilities.getPHPTokenSequence(doc, retval);
            if (ts != null) {
                ts.move(retval);
                int curlyMatch = 0;
                while (ts.moveNext()) {
                    Token t = ts.token();
                    if (t.id() == PHPTokenId.PHP_CURLY_OPEN || t.id() == PHPTokenId.PHP_CURLY_CLOSE) {
                        if (t.id() == PHPTokenId.PHP_CURLY_OPEN) {
                            curlyMatch++;
                        } else if (t.id() == PHPTokenId.PHP_CURLY_CLOSE) {
                            curlyMatch--;
                        }
                        if (curlyMatch == 0) {
                            ts.moveNext();
                            retval = ts.offset();
                            break;
                        }
                    }
                }
            }
        } finally {
            doc.readUnlock();
        }
        return retval;
    }

    private static int getOffsetAfterNextSemicolon(BaseDocument doc, int offset) throws BadLocationException {
        return getOffsetAfterNextTokenId(doc, offset, PHPTokenId.PHP_SEMICOLON);
    }

    private static int getOffsetAfterClassOpenCurly(BaseDocument doc, int offset) throws BadLocationException {
        return getOffsetAfterNextTokenId(doc, offset, PHPTokenId.PHP_CURLY_OPEN);
    }

    private static int getOffsetAfterNextTokenId(BaseDocument doc, int offset, PHPTokenId tokenId) throws BadLocationException {
        int retval = offset;
        doc.readLock();
        try {
            TokenSequence<? extends PHPTokenId> ts = LexUtilities.getPHPTokenSequence(doc, retval);
            if (ts != null) {
                ts.move(retval);
                while (ts.moveNext()) {
                    Token t = ts.token();
                    if (t.id() == tokenId) {
                        ts.moveNext();
                        retval = ts.offset();
                        break;
                    }
                }
            }
        } finally {
            doc.readUnlock();
        }
        return retval;
    }

    /**
     * Methode utilisé pour générer le code de correctiondes methode NON static
     */
    /*private static PHPCompletionItem.MethodDeclarationItem createMethodDeclarationItem(final TypeScope typeScope, final MethodInvocation node) {
        final String methodName = CodeUtils.extractFunctionName(node.getMethod());
        final MethodElement method = MethodElementImpl.createMagicMethod(typeScope,
                methodName, 0, getParameters(node.getMethod().getParameters()));
        return typeScope.isInterface()
                ? PHPCompletionItem.MethodDeclarationItem.forIntroduceInterfaceHint(method, null)
                : PHPCompletionItem.MethodDeclarationItem.forIntroduceHint(method, null);
    }*/
}