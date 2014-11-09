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

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.openide.cookies.EditorCookie;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Loïc Laverdant
 */
public abstract class RefactoringTask extends UserTask implements Runnable {

    private static final RequestProcessor RP = new RequestProcessor(RefactoringTask.class);
    private static final Logger LOG = Logger.getLogger(RefactoringTask.class.getName());
    RefactoringUIHolder uiHolder = RefactoringUIHolder.NONE;

    protected void fetchRefactoringUI(Source source, UserTask userTask) {
        Future<?> futureTask = RP.submit(new ParsingTask(source, userTask));
        boolean parsingInProgress = false;
        try {
            futureTask.get(2500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            LOG.log(Level.WARNING, null, ex);
        } catch (TimeoutException ex) {
            futureTask.cancel(true);
            parsingInProgress = true;
        }
        uiHolder.processUI(parsingInProgress);
    }

    private static final class ParsingTask implements Runnable, Cancellable {

        private final Source source;
        private final UserTask userTask;
        private volatile boolean cancelled;
        private volatile Future<Void> future;

        private ParsingTask(Source source, UserTask userTask) {
            this.source = source;
            this.userTask = userTask;
        }

        @Override
        public void run() {
            try {
                if (!cancelled) {
                    future = ParserManager.parseWhenScanFinished(Collections.singleton(source), userTask);
                }
            } catch (ParseException e) {
                LOG.log(Level.WARNING, null, e);
            }
        }

        @Override
        public boolean cancel() {
            cancelled = true;
            if (future != null) {
                future.cancel(true);
            }
            return true;
        }
    }

    abstract static class TextComponentTask extends RefactoringTask {

        private final JTextComponent textC;
        private final int caret;
        private final OffsetRange offsetRange;
        private final Document document;

        public TextComponentTask(final EditorCookie ec) {
            this.textC = ec.getOpenedPanes()[0];
            this.document = textC.getDocument();
            this.caret = textC.getCaretPosition();
            this.offsetRange = new OffsetRange(textC.getSelectionStart(), textC.getSelectionEnd());

            assert caret != -1;
        }

        @Override
        public void run() {
            fetchRefactoringUI(Source.create(document), this);
        }

        @Override
        public void run(ResultIterator resultIterator) throws Exception {
            Parser.Result parserResult = resultIterator.getParserResult();
            if (parserResult instanceof PHPParseResult) {
                Program root = getRoot((PHPParseResult) parserResult);
                if (root != null) {
                    uiHolder = createRefactoringUI((PHPParseResult) parserResult, caret, offsetRange);
                    return;
                }
            }
            // TODO How do I add some kind of error message?
            RefactoringTask.LOG.log(Level.FINE, "FAILURE - can't refactor uncompileable sources");
        }

        public static Program getRoot(ParserResult info) {
            return (info instanceof PHPParseResult) ? ((PHPParseResult) info).getProgram() : null;
        }

        protected abstract RefactoringUIHolder createRefactoringUI(final PHPParseResult info, final int offset, OffsetRange offsetRange);

    }

    @NbBundle.Messages({
        "ERR_ParsingInProgress=Can't refactor - parsing in progress.",
        "ERR_ElementNotInUsersFile=Can't refactor - element is on Include Path or in Signature File"
    })
    interface RefactoringUIHolder {

        RefactoringUIHolder NONE = new RefactoringUIHolder() {

            @Override
            public void processUI(boolean parsingInProgress) {
                if (parsingInProgress) {
                    JOptionPane.showMessageDialog(null, Bundle.ERR_ParsingInProgress());
                }
            }
        };

        RefactoringUIHolder NOT_USERS_FILE = new RefactoringUIHolder() {

            @Override
            public void processUI(boolean parsingInProgress) {
                JOptionPane.showMessageDialog(null, Bundle.ERR_ElementNotInUsersFile());
            }
        };

        void processUI(boolean parsingInProgress);
    }

}
