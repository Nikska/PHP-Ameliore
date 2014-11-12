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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.model.TypeAssignments;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 *
 * @author Loïc Laverdant
 */
public class PhpMoveRefactoringTool {

    public static String reformatNewText(FileObject file, int offsetBegin, int length, String newText) {

        try {
            DataObject od = DataObject.find(file);
            EditorCookie ec = od.getLookup().lookup(EditorCookie.class);
            if (ec != null) {
                BaseDocument bdoc = (BaseDocument) ec.openDocument();
                String mimeType = (String) bdoc.getProperty("mimeType"); //NOI18N
                BaseDocument newDoc = new BaseDocument(false, mimeType);
                newDoc.insertString(0, bdoc.getText(0, bdoc.getLength()), null);

                if (length > 0) {
                    newDoc.remove(offsetBegin, length);
                }
                newDoc.insertString(offsetBegin, newText, null);
                int reformatLenght = Utilities.reformat(newDoc, offsetBegin, offsetBegin + newText.length());
                String reformatedText = newDoc.getText(offsetBegin, reformatLenght);
                return reformatedText;
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException | BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }

        return newText;
    }

    public static String getReturnsAssignment(Set<PhpElement> returnAssignments) {
        String returns = "";
        boolean hasReturns = false;
        int countReturn = 0;

        for (PhpElement element : returnAssignments) {
            if (hasReturns) {
                returns += ", ";
            }
            returns += element.getName();
            hasReturns = true;
            countReturn++;
        }

        if (countReturn > 1) {
            returns = "list(" + returns + ")";
        }

        if (hasReturns) {
            return returns + " = ";
        }

        return "";
    }

    public static String getUsageNewDeclaration(PhpMoveRefactoring refactoring, Set<PhpElement> returnAssignments, Set<PhpElement> parameters) {
        String newDeclaration = PhpMoveRefactoringTool.getReturnsAssignment(returnAssignments);

        switch (refactoring.getNewType()) {
            case MoveSupport.TYPE_NEW_FILE:
                return "include('" + refactoring.getResultFileObject().getPath() + "');";
            case MoveSupport.TYPE_METHOD:
                newDeclaration += "$this->";
                break;
        }

        newDeclaration += refactoring.getNewName() + "(" + PhpMoveRefactoringTool.getParameters(parameters) + ");";
        return newDeclaration;
    }

    public static String getPhpDoc(PhpMoveRefactoring refactoring, Set<PhpElement> parameters, Set<PhpElement> returns) {
        String phpDoc = "/**\n";
        phpDoc += "* Refactored "+ refactoring.getNewName() + "\n";
        phpDoc += "*\n";

        for (PhpElement parameter : parameters) {
            phpDoc += "* @param " + findTypeName(parameter) + " " + parameter.getName() + "\n";
        }

        for (PhpElement returnElement : returns) {
            phpDoc += "* @return " + findTypeName(returnElement) + "\n";
        }

        phpDoc += "*/\n";
        return phpDoc;
    }

    public static String findTypeName(PhpElement parameter) {
        String typeName = "mixed";

        if (parameter instanceof TypeAssignments) {
            TypeAssignments varName = (TypeAssignments) parameter;
            Collection<? extends String> types = varName.getTypeNames(parameter.getOffset());
            for (String type : types) {
                return type;
            }
        }

        return typeName;
    }

    public static String getStartNewDeclaration(PhpMoveRefactoring refactoring, Set<PhpElement> parameters) {
        String newDeclaration = "";

        if (refactoring.getNewType().equals(MoveSupport.TYPE_METHOD)
                && !refactoring.getModifier().isEmpty()) {
            newDeclaration = refactoring.getModifier() + " ";
        }

        newDeclaration += "function " + refactoring.getNewName() + "(" + PhpMoveRefactoringTool.getParameters(parameters) + ") {\n";
        return newDeclaration;
    }

    public static String getEndNewDeclaration() {
        String newDeclaration = "\n}";
        return newDeclaration;
    }

    public static String getReturnDeclaration(Set<PhpElement> phpElements) {
        String returns = "\n" + PhpMoveRefactoringTool.getReturns(phpElements) + "\n";
        return returns;
    }

    private static String getReturns(Set<PhpElement> phpElements) {
        String returns = "";
        boolean hasReturns = false;
        int countReturn = 0;

        for (PhpElement element : phpElements) {
            if (hasReturns) {
                returns += ", ";
            }
            returns += element.getName();
            hasReturns = true;
            countReturn++;
        }

        if (countReturn > 1) {
            returns = "array(" + returns + ")";
        }

        if (hasReturns) {
            return "return " + returns + ";";
        }

        return "";
    }

    private static String getParameters(Set<PhpElement> parameters) {
        String returnParameters = "";
        boolean hasParameter = false;
        for (PhpElement element : parameters) {
            if (hasParameter) {
                returnParameters += ", ";
            }
            returnParameters += element.getName();
            hasParameter = true;
        }
        return returnParameters;
    }

}
