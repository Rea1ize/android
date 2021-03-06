/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.testing;

import static com.android.utils.TraceUtils.getCurrentStack;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.android.builder.model.SyncIssue;
import com.android.tools.idea.gradle.project.sync.GradleSyncListener;
import com.android.tools.idea.gradle.project.sync.issues.SyncIssues;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestGradleSyncListener implements GradleSyncListener {
  @NotNull private final CountDownLatch myLatch;

  boolean syncSkipped;
  boolean success;
  boolean hasErrors;
  @Nullable String failureMessage;

  TestGradleSyncListener() {
    myLatch = new CountDownLatch(1);
  }

  @Override
  public void syncSkipped(@NotNull Project project) {
    syncSucceeded(project);
    syncSkipped = true;
  }

  @Override
  public void syncSucceeded(@NotNull Project project) {
    success = true;
    myLatch.countDown();
  }

  @Override
  public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
    success = false;
    failureMessage = !errorMessage.isEmpty() ? errorMessage : "No errorMessage at:\n" + getCurrentStack();
    myLatch.countDown();
  }

  void await() throws InterruptedException {
    myLatch.await(5, MINUTES);
  }

  public boolean isSyncSkipped() {
    return syncSkipped;
  }

  public boolean isSyncFinished() {
    return success || failureMessage != null;
  }

  public void collectErrors(Project project) {
    SyncIssues.seal(project);
    String errors =
      SyncIssues.byModule(project).values().stream()
        .flatMap(it -> it.stream()).filter(it -> it.getSeverity() >= SyncIssue.SEVERITY_ERROR)
        .map(it -> it.getMessage())
        .collect(Collectors.joining("\n"));
    hasErrors = !errors.isEmpty();
    if (success && hasErrors) {
      failureMessage = failureMessage != null ? failureMessage + "\n" + errors : errors;
    }
  }
}
