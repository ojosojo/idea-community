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
package com.intellij.execution.testframework.sm.runner.ui;

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.TestFrameworkRunningModel;
import com.intellij.execution.testframework.sm.runner.ProxyFilters;
import com.intellij.execution.testframework.actions.ScrollToTestSourceAction;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.SMRunnerUtil;
import com.intellij.util.OpenSourceUtil;
import com.intellij.openapi.application.ModalityState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerUIActionsHandler implements TestResultsViewer.EventsListener {
  private final TestConsoleProperties myConsoleProperties;

  public SMTRunnerUIActionsHandler(final TestConsoleProperties consoleProperties) {
    myConsoleProperties = consoleProperties;
  }

  public void onTestNodeAdded(final TestResultsViewer sender, final SMTestProxy test) {
    if (TestConsoleProperties.TRACK_RUNNING_TEST.value(myConsoleProperties)) {
      sender.selectAndNotify(test);
    }
  }

  public void onTestingStarted(TestResultsViewer sender) {
    // Do nothing
  }

  public void onTestingFinished(final TestResultsViewer sender) {
    // select first defect at the end (my be TRACK_RUNNING_TEST was enabled and affects on the fly selection)
    final SMTestProxy testsRootNode = sender.getTestsRootNode();
    if (TestConsoleProperties.SELECT_FIRST_DEFECT.value(myConsoleProperties)) {
      final AbstractTestProxy firstDefect;

      // defects priority:
      // ERROR -> FAILURE -> GENERAL DEFECTIVE NODE
      final List<SMTestProxy> allTests = testsRootNode.getAllTests();
      final AbstractTestProxy firstError = ProxyFilters.ERROR_LEAF.detectIn(allTests);
      if (firstError != null) {
       firstDefect = firstError;
      } else {
        final AbstractTestProxy firstFailure = ProxyFilters.FAILURE_LEAF.detectIn(allTests);
        if (firstFailure != null) {
         firstDefect = firstFailure;
        } else {
          firstDefect = null;
        }
      }

      // select if detected
      if (firstDefect != null) {
        sender.selectAndNotify(firstDefect);
      }
    }
  }

  public void onSelected(@Nullable final SMTestProxy selectedTestProxy,
                         @NotNull final TestResultsViewer viewer,
                         @NotNull final TestFrameworkRunningModel model) {
    //TODO: tests o "onSelected"
    SMRunnerUtil.runInEventDispatchThread(new Runnable() {
      public void run() {
        if (ScrollToTestSourceAction.isScrollEnabled(model)) {
          OpenSourceUtil.openSourcesFrom(model.getTreeView(), false);
        }
      }
    }, ModalityState.NON_MODAL);
  }
}
