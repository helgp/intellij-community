package com.intellij.compiler.options;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ex.MultiLineLabel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.ui.ItemRemovable;
import com.intellij.util.ui.Table;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 5, 2009
 */
public class AnnotationProcessorsConfigurable implements SearchableConfigurable {
  private ProcessedModulesTable myModulesTable;
  private final Project myProject;
  private JRadioButton myRbClasspath;
  private JRadioButton myRbProcessorsPath;
  private TextFieldWithBrowseButton myProcessorPathField;
  private ProcessorTableModel myProcessorsModel;
  private JCheckBox myCbEnableProcessing;
  private JButton myRemoveButton;
  private Table myProcessorTable;
  private JButton myAddButton;

  public AnnotationProcessorsConfigurable(final Project project) {
    myProject = project;
  }

  public String getDisplayName() {
    return "Annotation Processors";
  }

  public Icon getIcon() {
    return null;
  }

  public String getHelpTopic() {
    return "reference.projectsettings.compiler.annotationProcessors";
  }

  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  public Runnable enableSearch(String option) {
    return null;
  }

  public JComponent createComponent() {
    final JPanel mainPanel = new JPanel(new GridBagLayout());

    myCbEnableProcessing = new JCheckBox("Enable annotation processing");

    myRbClasspath = new JRadioButton("Obtain processors from project classpath");
    myRbProcessorsPath = new JRadioButton("Processor path:");
    ButtonGroup group = new ButtonGroup();
    group.add(myRbClasspath);
    group.add(myRbProcessorsPath);

    myProcessorPathField = new TextFieldWithBrowseButton(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final VirtualFile[] files =
          FileChooser.chooseFiles(myProcessorPathField, FileChooserDescriptorFactory.createAllButJarContentsDescriptor());
        if (files.length > 0) {
          final StringBuilder builder = new StringBuilder();
          for (VirtualFile file : files) {
            if (builder.length() > 0) {
              builder.append(File.pathSeparator);
            }
            builder.append(FileUtil.toSystemDependentName(file.getPath()));
          }
          myProcessorPathField.setText(builder.toString());
        }
      }
    });

    final JPanel processorTablePanel = new JPanel(new BorderLayout());
    myProcessorsModel = new ProcessorTableModel();
    processorTablePanel.setBorder(IdeBorderFactory.createTitledBorder("Annotation Processors", false, false, true));
    myProcessorTable = new Table(myProcessorsModel);
    myProcessorTable.getEmptyText().setText("No processors configured");

    processorTablePanel.add(ScrollPaneFactory.createScrollPane(myProcessorTable), BorderLayout.CENTER);
    final JPanel buttons = new JPanel(new GridBagLayout());
    myAddButton = new JButton("Add");
    buttons.add(myAddButton, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    myRemoveButton = new JButton("Remove");
    buttons.add(myRemoveButton, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
    processorTablePanel.add(buttons, BorderLayout.EAST);
    processorTablePanel.setPreferredSize(new Dimension(processorTablePanel.getPreferredSize().width, 50));

    myModulesTable = new ProcessedModulesTable(myProject);
    myModulesTable.setBorder(IdeBorderFactory.createTitledBorder("Processed Modules", false, false, true));
    final JLabel noteMessage = new MultiLineLabel("Source files generated by annotation processors will be stored under the project output directory.\n" +
                                                  "To override this behaviour for certain modules you may specify the directory name in the table below.\n" +
                                                  "If specified, the directory will be created under corresponding module's content root.");
    
    final JLabel warning = new MultiLineLabel("WARNING!\n" +
                                              "All source files located in the generated sources output directory WILL BE EXCLUDED from annotation processing.\n" +
                                              "If option 'Clear output directory on rebuild' is enabled,\n" +
                                              "the entire contents of directories specified in the table below WILL BE CLEARED on rebuild.");
    warning.setFont(warning.getFont().deriveFont(Font.BOLD));

    mainPanel.add(myCbEnableProcessing, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    mainPanel.add(myRbClasspath, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    mainPanel.add(myRbProcessorsPath, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    mainPanel.add(myProcessorPathField, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
    mainPanel.add(processorTablePanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(10, 0, 0, 0), 0, 0));
    mainPanel.add(noteMessage, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));
    mainPanel.add(warning, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));
    mainPanel.add(myModulesTable, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(10, 0, 0, 0), 0, 0));


    myRbClasspath.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        updateEnabledState();
      }
    });

    myProcessorTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          updateEnabledState();
        }
      }
    });
    myAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final TableCellEditor cellEditor = myProcessorTable.getCellEditor();
        if (cellEditor != null) {
          cellEditor.stopCellEditing();
        }
        final ProcessorTableModel model = (ProcessorTableModel)myProcessorTable.getModel();
        final int inserdedIndex = model.addRow();
        TableUtil.editCellAt(myProcessorTable, inserdedIndex, ProcessorTableRow.NAME_COLUMN);
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TableUtil.removeSelectedItems(myProcessorTable);
      }
    });

    myCbEnableProcessing.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        updateEnabledState();
      }
    });

    updateEnabledState();
    
    return mainPanel;
  }

  private void updateEnabledState() {
    final boolean enabled = myCbEnableProcessing.isSelected();
    final boolean useProcessorpath = !myRbClasspath.isSelected();
    myRbClasspath.setEnabled(enabled);
    myRbProcessorsPath.setEnabled(enabled);
    myProcessorPathField.setEnabled(enabled && useProcessorpath);
    myRemoveButton.setEnabled(enabled && myProcessorTable.getSelectedRow() >= 0);
    myAddButton.setEnabled(enabled);
    myProcessorTable.setEnabled(enabled);
    final JTableHeader header = myProcessorTable.getTableHeader();
    if (header != null) {
      header.repaint();
    }
    myModulesTable.setEnabled(enabled);
  }

  public boolean isModified() {
    final CompilerConfiguration config = CompilerConfiguration.getInstance(myProject);
    if (config.isAnnotationProcessorsEnabled() != myCbEnableProcessing.isSelected()) {
      return true;
    }
    if (config.isObtainProcessorsFromClasspath() != myRbClasspath.isSelected()) {
      return true;
    }
    if (!FileUtil.pathsEqual(config.getProcessorPath(), FileUtil.toSystemIndependentName(myProcessorPathField.getText().trim()))) {
      return true;
    }

    final Map<String, String> map = myProcessorsModel.exportToMap();
    if (!map.equals(config.getAnnotationProcessorsMap())) {
      return true;
    }

    if (!getMarkedModules().equals(config.getAnotationProcessedModules())) {
      return true;
    }

    return false;
  }

  public void apply() throws ConfigurationException {
    final CompilerConfiguration config = CompilerConfiguration.getInstance(myProject);
    config.setAnnotationProcessorsEnabled(myCbEnableProcessing.isSelected());

    config.setObtainProcessorsFromClasspath(myRbClasspath.isSelected());
    config.setProcessorsPath(FileUtil.toSystemIndependentName(myProcessorPathField.getText().trim()));

    config.setAnnotationProcessorsMap(myProcessorsModel.exportToMap());

    config.setAnotationProcessedModules(getMarkedModules());
  }

  private Map<Module, String> getMarkedModules() {
    final Map<Module, String> result = new HashMap<Module, String>();
    for (Pair<Module, String> pair : myModulesTable.getAllModules()) {
      result.put(pair.getFirst(), pair.getSecond());
    }
    return result;
  }

  public void reset() {
    final CompilerConfiguration config = CompilerConfiguration.getInstance(myProject);
    myCbEnableProcessing.setSelected(config.isAnnotationProcessorsEnabled());

    final boolean obtainFromClasspath = config.isObtainProcessorsFromClasspath();
    if (obtainFromClasspath) {
      myRbClasspath.setSelected(true);
    }
    else {
      myRbProcessorsPath.setSelected(true);
    }

    myProcessorPathField.setText(FileUtil.toSystemDependentName(config.getProcessorPath()));

    myProcessorsModel.setProcessorMap(config.getAnnotationProcessorsMap());
    
    myModulesTable.removeAllElements();
    for (final Module module : ModuleManager.getInstance(myProject).getModules()) {
      if (config.isAnnotationProcessingEnabled(module)) {
        myModulesTable.addModule(module, config.getGeneratedSourceDirName(module));
      }
    }
    myModulesTable.sort(new Comparator<Module>() {
      public int compare(Module o1, Module o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
      }
    });
  }

  public void disposeUIResources() {
  }

  private static class ProcessorTableModel extends AbstractTableModel implements ItemRemovable{
    private final java.util.List<ProcessorTableRow> myRows = new ArrayList<ProcessorTableRow>();

    public String getColumnName(int column) {
      switch (column) {
        case ProcessorTableRow.NAME_COLUMN: return "Processor FQ Name";
        case ProcessorTableRow.OPTIONS_COLUMN : return "Processor Run Options (space-separated \"key=value\" pairs)";
      }
      return super.getColumnName(column);
    }

    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    public int getRowCount() {
      return myRows.size();
    }

    public int getColumnCount() {
      return 2;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == ProcessorTableRow.NAME_COLUMN || columnIndex == ProcessorTableRow.OPTIONS_COLUMN;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      final ProcessorTableRow row = myRows.get(rowIndex);
      switch (columnIndex) {
        case ProcessorTableRow.NAME_COLUMN: return row.name;
        case ProcessorTableRow.OPTIONS_COLUMN : return row.options;
      }
      return null;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (aValue != null) {
        final ProcessorTableRow row = myRows.get(rowIndex);
        switch (columnIndex) {
          case ProcessorTableRow.NAME_COLUMN:
            row.name = (String)aValue;
            break;
          case ProcessorTableRow.OPTIONS_COLUMN:
            row.options = (String)aValue;
            break;
        }
      }
    }

    public void removeRow(int idx) {
      myRows.remove(idx);
      fireTableRowsDeleted(idx, idx);
    }

    public int addRow() {
      myRows.add(new ProcessorTableRow());
      final int inserted = myRows.size() - 1;
      fireTableRowsInserted(inserted, inserted);
      return inserted;
    }

    public void setProcessorMap(Map<String, String> processorMap) {
      clear();
      if (processorMap.size() > 0) {
        for (Map.Entry<String, String> entry : processorMap.entrySet()) {
          myRows.add(new ProcessorTableRow(entry.getKey(), entry.getValue()));
        }
        Collections.sort(myRows, new Comparator<ProcessorTableRow>() {
          public int compare(ProcessorTableRow o1, ProcessorTableRow o2) {
            return o1.name.compareToIgnoreCase(o2.name);
          }
        });
        fireTableRowsInserted(0, processorMap.size()-1);
      }
    }

    public void clear() {
      final int count = myRows.size();
      if (count > 0) {
        myRows.clear();
        fireTableRowsDeleted(0, count-1);
      }
    }

    public Map<String, String> exportToMap() {
      final Map<String, String> map = new HashMap<String, String>();
      for (ProcessorTableRow row : myRows) {
        if (row.name != null) {
          final String name = row.name.trim();
          if (name.length() > 0 && !map.containsKey(name)) {
            map.put(name, row.options);
          }
        }
      }
      return map;
    }
  }

  private static final class ProcessorTableRow {
    public static final int NAME_COLUMN = 0;
    public static final int OPTIONS_COLUMN = 1;

    public String name = "";
    public String options = "";

    public ProcessorTableRow() {
    }

    public ProcessorTableRow(String name, String options) {
      this.name = name != null? name : "";
      this.options = options != null? options : "";
    }
  }
}
