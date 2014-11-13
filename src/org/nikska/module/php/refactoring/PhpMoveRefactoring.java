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

import java.util.List;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Loïc Laverdant
 */
public final class PhpMoveRefactoring extends AbstractRefactoring {

    private String newName;
    private String modifier;
    private String newType;
    private ParserResult parserResult;
    private boolean generatePhpDoc;

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public PhpMoveRefactoring(Lookup lkp) {
        super(lkp);
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewType(String newType) {
        this.newType = newType;
    }

    public String getNewType() {
        return this.newType;
    }

    public PHPParseResult getParserResult() {
        return (PHPParseResult) this.parserResult;
    }
    
    public void setParserResult(PHPParseResult parserResult) {
        this.parserResult = parserResult;
    }

    /**
     * @todo Attention pottentiellement plusieurs classe peuvent etre défini dans un fichier
     */
    public ClassDeclaration getClassDeclaration() {
        Model model = getParserResult().getModel();
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

    public FileObject getResultFileObject() {
        if (parserResult != null) {
            return parserResult.getSnapshot().getSource().getFileObject();
        }
        return null;
    }
    
    public boolean isGeneratePhpDoc() {
        return generatePhpDoc;
    }

    void setGeneratePhpDoc(boolean generatePhpDoc) {
        this.generatePhpDoc = generatePhpDoc;
    }

}
