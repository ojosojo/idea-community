/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.util.importProject;

import com.intellij.ide.util.newProjectWizard.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.AbstractStepWithProgress;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Jul 18, 2007
 */
public class ModulesDetectionStep extends AbstractStepWithProgress<List<ModuleDescriptor>> {
  private final ProjectFromSourcesBuilder myBuilder;
  private final ModuleInsight myInsight;
  private final Icon myIcon;
  private final String myHelpId;
  private ModulesLayoutPanel myModulesLayoutPanel;

  public ModulesDetectionStep(ProjectFromSourcesBuilder builder, final ModuleInsight insight, Icon icon, @NonNls String helpId) {
    super("Stop module analysis?");
    myBuilder = builder;
    myInsight = insight;
    myIcon = icon;
    myHelpId = helpId;
  }

  public void updateDataModel() {
    myBuilder.setModules(myModulesLayoutPanel.getChosenEntries());
  }

  protected JComponent createResultsPanel() {
    myModulesLayoutPanel = new ModulesLayoutPanel(myInsight, new ModulesLayoutPanel.LibraryFilter() {
      public boolean isLibraryChosen(final LibraryDescriptor libDescriptor) {
        return myBuilder.isLibraryChosen(libDescriptor);
      }
    });
    return myModulesLayoutPanel;
  }

  protected String getProgressText() {
    return "Searching for modules. Please wait.";
  }

  int myPreviousStateHashCode = -1;
  protected boolean shouldRunProgress() {
    final int currentHash = calcStateHashCode();
    try {
      return currentHash != myPreviousStateHashCode;
    }
    finally {
      myPreviousStateHashCode = currentHash;
    }
  }

  private int calcStateHashCode() {
    final String contentEntryPath = myBuilder.getContentEntryPath();
    int hash = contentEntryPath != null? contentEntryPath.hashCode() : 1;
    for (Pair<String, String> pair : myBuilder.getSourcePaths()) {
      hash = 31 * hash + pair.getFirst().hashCode();
      hash = 31 * hash + pair.getSecond().hashCode();
    }
    final List<LibraryDescriptor> libs = myBuilder.getLibraries();
    for (LibraryDescriptor lib : libs) {
      final Collection<File> files = lib.getJars();
      for (File file : files) {
        hash = 31 * hash + file.hashCode();
      }
    }
    return hash;
  }

  protected List<ModuleDescriptor> calculate() {
    myInsight.scanModules();
    final List<ModuleDescriptor> suggestedModules = myInsight.getSuggestedModules();
    return suggestedModules != null? suggestedModules : Collections.<ModuleDescriptor>emptyList();
  }

  @Override
  public boolean validate() {
    final boolean validated = super.validate();
    if (!validated) {
      return false;
    }

    final List<ModuleDescriptor> modules = myModulesLayoutPanel.getChosenEntries();
    List<String> errors = new ArrayList<String>();
    for (ModuleDescriptor module : modules) {
      try {
        final String moduleFilePath = module.computeModuleFilePath();
        if (new File(moduleFilePath).exists()) {
          errors.add(IdeBundle.message("warning.message.the.module.file.0.already.exist.and.will.be.overwritten", moduleFilePath));
        }
      }
      catch (InvalidDataException e) {
        errors.add(e.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      final int answer = Messages.showYesNoDialog(getComponent(),
                                                  IdeBundle.message("warning.text.0.do.you.want.to.continue", StringUtil.join(errors, "\n")),
                                                  IdeBundle.message("title.file.already.exists"), Messages.getQuestionIcon());
      if (answer != 0) {
        return false;
      }
    }
    return true;
  }

  protected void onFinished(final List<ModuleDescriptor> moduleDescriptors, final boolean canceled) {
    myModulesLayoutPanel.rebuild();
  }

  @NonNls
  public String getHelpId() {
    return myHelpId;
  }

  public Icon getIcon() {
    return myIcon;
  }
}
