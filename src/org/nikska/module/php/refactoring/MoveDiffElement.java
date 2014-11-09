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
import java.lang.ref.WeakReference;
import org.netbeans.modules.csl.spi.support.ModificationResult;
import org.netbeans.modules.csl.spi.support.ModificationResult.Difference;
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation;
import org.openide.filesystems.FileObject;
import org.openide.text.PositionBounds;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * @author Loïc Laverdant
 */
public final class MoveDiffElement extends SimpleRefactoringElementImplementation {

    private final String newFileName;
    private WeakReference<String> newFileContent;
    private ModificationResult modification;
    private String displayText;
    private final Difference diff;
    private final FileObject parentFile;
    private PositionBounds bounds;

    public static MoveDiffElement create(
            ModificationResult.Difference diff,
            FileObject fileObject,
            ModificationResult modification) {
        return new MoveDiffElement(diff, new PositionBounds(diff.getStartPosition(), diff.getEndPosition()), fileObject, modification);
    }

    public MoveDiffElement(Difference diff, PositionBounds bounds, FileObject parentFile, ModificationResult modification) {
        this.newFileName = diff.getNewText();
        this.displayText = diff.getDescription();
        this.diff = diff;
        this.parentFile = parentFile;
        this.bounds = bounds;
        this.modification = modification;
    }

    @Override
    protected String getNewFileContent() {
        String result;
        if (newFileContent != null) {
            result = newFileContent.get();
            if (result != null) {
                return result;
            }
        }
        try {
            result = modification.getResultingSource(parentFile);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
        newFileContent = new WeakReference<>(result);
        return result;
    }

    @Override
    public void performChange() {
    }

    @Override
    public String getText() {
        return displayText;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public Lookup getLookup() {
        return Lookups.fixed(diff);
    }

    @Override
    public FileObject getParentFile() {
        return this.parentFile;
    }

    @Override
    public PositionBounds getPosition() {
        return bounds;
    }

}
