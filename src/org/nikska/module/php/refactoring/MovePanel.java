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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * Move refactoring parameters panel
 *
 * @author Loïc Laverdant
 */
public class MovePanel extends JPanel implements CustomRefactoringPanel {

    //private final transient String oldName;
    private final transient ChangeListener parent;
    private boolean initialized;
    private final FileObject file;
    private ParserResult parserResult;

    /**
     * Creates new form RenamePanelName
     */
    public MovePanel(MoveSupport support, ChangeListener parent, String name) {
        setName(name);
        this.file = support.getSourceFileObject();
        this.parent = parent;
        initComponents();
        fileNameTextField.setText(file.getPath());

        newTypeComboBox.addItem(MoveSupport.TYPE_FUNCTION);
        newTypeComboBox.addItem(MoveSupport.TYPE_NEW_FILE);
        newTypeComboBox.setSelectedIndex(0);
        
        if (support.isInMethod()) {
            newTypeComboBox.addItem(MoveSupport.TYPE_METHOD);
            newTypeComboBox.setSelectedIndex(newTypeComboBox.getItemCount() - 1);
        }
        
        parserResult = support.getParseResult();
        newNameTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void changedUpdate(DocumentEvent event) {
                MovePanel.this.parent.stateChanged(null);
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                MovePanel.this.parent.stateChanged(null);
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                MovePanel.this.parent.stateChanged(null);
            }
        });
        
        configureForm();
    }
    
    private void configureForm() {
        
        //Desactivé pour l'instant
        classNameLabel.setVisible(false);
        classNameTextField.setVisible(false);
        
        String selectedItem = (String) newTypeComboBox.getSelectedItem();
        if (MoveSupport.TYPE_METHOD.equals(selectedItem)) {
            configureFormForMethod();
        } else if (MoveSupport.TYPE_FUNCTION.equals(selectedItem)) {
            configureFormForFunction();
        }
        else {
            configureFormForElse();
        }
    }
    
    private void configureFormForMethod() {
        modifierLabel.setVisible(true);
        modifierComboBox.setVisible(true);
        newNameLabel.setVisible(true);
        newNameTextField.setVisible(true);
        newNameLabel.setText(NbBundle.getMessage(MovePanel.class, "LBL_NewMethod"));
        newNameTextField.setText(NbBundle.getMessage(MovePanel.class, "LBL_NewMethodValue"));
        newFileCheckBox.setVisible(false);
        generatePhpDocCheckBox.setVisible(true);
    }
    
    private void configureFormForFunction() {
        modifierLabel.setVisible(false);
        modifierComboBox.setVisible(false);
        newNameLabel.setVisible(true);
        newNameTextField.setVisible(true);
        newNameLabel.setText(NbBundle.getMessage(MovePanel.class, "LBL_NewFunction"));
        newNameTextField.setText(NbBundle.getMessage(MovePanel.class, "LBL_NewFunctionValue"));
        newFileCheckBox.setVisible(true);
        generatePhpDocCheckBox.setVisible(true);
    }
    
    private void configureFormForElse() {
        modifierLabel.setVisible(false);
        modifierComboBox.setVisible(false);
        newNameLabel.setVisible(false);
        newNameTextField.setVisible(false);
        newFileCheckBox.setVisible(true);
        generatePhpDocCheckBox.setVisible(false);
    }
            

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    public String getNameValue() {
        return newNameTextField.getText();
    }

    public String getNewType() {
        return (String) newTypeComboBox.getSelectedItem();
    }

    public String getModifier() {
        return (String) modifierComboBox.getSelectedItem();
    }

    public ParserResult getParserResult() {
        return parserResult;
    }
    
    private void parseChoosedFile(FileObject fileObject) throws DataObjectNotFoundException, IOException, ParseException {
        DataObject od = DataObject.find(fileObject);
        EditorCookie ec = od.getLookup().lookup(EditorCookie.class);
        if (ec != null) {
            BaseDocument bdoc = (BaseDocument) ec.openDocument();
            String selectedType = (String) newTypeComboBox.getSelectedItem();
            if (newFileCheckBox.isSelected() && !MoveSupport.TYPE_METHOD.equals(selectedType)) {
                try {
                    bdoc.insertString(0, "<?php\n", null);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            ParserManager.parseWhenScanFinished(Collections.singleton(Source.create(bdoc)), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    ParserResult info = (ParserResult) resultIterator.getParserResult();
                    if (info != null) {
                        parserResult = info;
                        parent.stateChanged(null);
                    }
                }
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        label = new javax.swing.JLabel();
        newTypeComboBox = new javax.swing.JComboBox();
        newNameLabel = new javax.swing.JLabel();
        newNameTextField = new javax.swing.JTextField();
        classNameLabel = new javax.swing.JLabel();
        classNameTextField = new javax.swing.JTextField();
        modifierLabel = new javax.swing.JLabel();
        modifierComboBox = new javax.swing.JComboBox();
        fileNameBrowseButton = new javax.swing.JButton();
        fileNameTextField = new javax.swing.JTextField();
        newFileCheckBox = new javax.swing.JCheckBox();
        generatePhpDocCheckBox = new javax.swing.JCheckBox();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 11, 11));

        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        org.openide.awt.Mnemonics.setLocalizedText(label, org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_NewType")); // NOI18N

        newTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newTypeComboBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(newNameLabel, org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_NewName")); // NOI18N

        newNameTextField.setText("methode");

        org.openide.awt.Mnemonics.setLocalizedText(classNameLabel, org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_ClassName")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(modifierLabel, org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_Modifier")); // NOI18N

        modifierComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "public", "protected", "private" }));

        org.openide.awt.Mnemonics.setLocalizedText(fileNameBrowseButton, org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_Browse")); // NOI18N
        fileNameBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNameBrowseButtonActionPerformed(evt);
            }
        });

        fileNameTextField.setText(org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_Browse_text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(newFileCheckBox, "New File");
        newFileCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newFileCheckBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(generatePhpDocCheckBox, org.openide.util.NbBundle.getMessage(MovePanel.class, "LBL_GeneratePhpDoc")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(modifierLabel)
                            .addComponent(classNameLabel)
                            .addComponent(label)
                            .addComponent(newNameLabel))
                        .addGap(67, 67, 67))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fileNameTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(newTypeComboBox, 0, 180, Short.MAX_VALUE)
                        .addComponent(classNameTextField))
                    .addComponent(newNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(modifierComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fileNameBrowseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(newFileCheckBox)))
                .addContainerGap(14, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(generatePhpDocCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(label)
                    .addComponent(newTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(classNameLabel)
                    .addComponent(classNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(modifierLabel)
                    .addComponent(modifierComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newNameLabel)
                    .addComponent(newNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(generatePhpDocCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fileNameBrowseButton)
                    .addComponent(newFileCheckBox))
                .addContainerGap(81, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fileNameBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNameBrowseButtonActionPerformed
        FileChooserBuilder fileBuilder = new FileChooserBuilder(MovePanel.class.getName())
                .setFilesOnly(true)
                .setTitle(NbBundle.getMessage(MovePanel.class, "LBL_Browser"))
                .setDefaultWorkingDirectory(new File(fileNameTextField.getText()));
        File fileChoose;
        if (!newFileCheckBox.isSelected()) {
            fileChoose = fileBuilder.showOpenDialog();
        }
        else {
            fileChoose = fileBuilder.showSaveDialog();
            if (fileChoose != null) {
                try {
                    fileChoose.createNewFile();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        if (fileChoose != null) {
            fileNameTextField.setText(fileChoose.getAbsolutePath());
            FileObject fileObject = FileUtil.toFileObject(fileChoose);
            
            try {
                parseChoosedFile(fileObject);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException | ParseException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }//GEN-LAST:event_fileNameBrowseButtonActionPerformed

    private void newTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newTypeComboBoxActionPerformed
        
        configureForm();
    }//GEN-LAST:event_newTypeComboBoxActionPerformed

    private void newFileCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newFileCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newFileCheckBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel classNameLabel;
    private javax.swing.JTextField classNameTextField;
    private javax.swing.JButton fileNameBrowseButton;
    private javax.swing.JTextField fileNameTextField;
    private javax.swing.JCheckBox generatePhpDocCheckBox;
    private javax.swing.JLabel label;
    private javax.swing.JComboBox modifierComboBox;
    private javax.swing.JLabel modifierLabel;
    private javax.swing.JCheckBox newFileCheckBox;
    private javax.swing.JLabel newNameLabel;
    private javax.swing.JTextField newNameTextField;
    private javax.swing.JComboBox newTypeComboBox;
    // End of variables declaration//GEN-END:variables

    public boolean getGeneratePhpDoc() {
        return generatePhpDocCheckBox.isSelected();
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
