/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.nikska.module.php.refactoring;

import java.util.Collection;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/**
 *
 * @author Radek Matous
 */
@org.openide.util.lookup.ServiceProvider(service = org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider.class, position = 400)
public class RefactoringActionsProvider extends ActionsImplementationProvider {

    private boolean isFromEditor(Lookup lookup) {
        //TODO: is from editor? review, improve
        EditorCookie ec = lookup.lookup(EditorCookie.class);
        return ec != null && ec.getOpenedPanes() != null;
    }

    @Override
    public boolean canMove(Lookup lookup) {
        return canMoveElement(lookup);
    }

    private boolean canMoveElement(Lookup lookup) {
        return true;//canFindUsages(lookup);
    }

    @Override
    public void doMove(final Lookup lookup) {
        EditorCookie ec = lookup.lookup(EditorCookie.class);
        if (isFromEditor(lookup)) {
            moveElement(ec);
        }
    }

    private void moveElement(EditorCookie ec) {
        new RefactoringTask.TextComponentTask(ec) {

            @Override
            protected RefactoringUIHolder createRefactoringUI(final PHPParseResult info, final int offset, OffsetRange offsetRange) {
                RefactoringUIHolder result = RefactoringUIHolder.NONE;
                
                MoveSupport ctx = MoveSupport.getInstance(info, offset, offsetRange);
                if (ctx != null) {
                    result = new RefactoringUIHolderImpl(new PhpMovefactoringUI(ctx));
                }
                return result;
            }
        }.run();
    }

    static final class RefactoringUIHolderImpl implements RefactoringTask.RefactoringUIHolder {
        private final RefactoringUI refactoringUI;

        public RefactoringUIHolderImpl(RefactoringUI refactoringUI) {
            assert refactoringUI != null;
            this.refactoringUI = refactoringUI;
        }

        @Override
        public void processUI(boolean parsingInProgress) {
            UI.openRefactoringUI(refactoringUI, TopComponent.getRegistry().getActivated());
        }

    }
}
