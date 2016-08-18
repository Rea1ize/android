/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync;

import com.android.tools.idea.gradle.GradleSyncState;
import com.android.tools.idea.gradle.project.GradleProjectSyncData;
import com.android.tools.idea.gradle.project.GradleSyncListener;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;
import org.gradle.tooling.ProjectConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import static com.android.tools.idea.gradle.util.GradleUtil.getOrCreateGradleExecutionSettings;
import static com.android.tools.idea.gradle.util.Projects.getBaseDirPath;
import static com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded;

public class GradleSync {
  @NotNull private final Project myProject;
  @NotNull private final GradleExecutionHelper myHelper = new GradleExecutionHelper();

  public static GradleSync getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleSync.class);
  }

  public GradleSync(@NotNull Project project) {
    myProject = project;
  }

  public void sync(@NotNull ProgressExecutionMode mode, @Nullable GradleSyncListener syncListener) {
    String title = String.format("Syncing project '%1$s' with Gradle", myProject.getName());
    Task task;
    switch (mode) {
      case MODAL_SYNC:
        task = new Task.Modal(myProject, title, true) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            sync(indicator, syncListener);
          }
        };
        break;
      case IN_BACKGROUND_ASYNC:
        task = new Task.Backgroundable(myProject, title, true) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            sync(indicator, syncListener);
          }
        };
        break;
      default:
        throw new IllegalArgumentException(mode + " is not a supported execution mode");
    }
    invokeAndWaitIfNeeded((Runnable)task::queue);
  }

  private void sync(@NotNull ProgressIndicator indicator, @Nullable GradleSyncListener syncListener) {
    if (myProject.isDisposed()) {
      return;
    }

    // TODO: Handle sync cancellation.

    if (GradleSyncState.getInstance(myProject).isSyncInProgress()) {
      handleSyncFailure("Another 'Gradle Sync' task is currently running", syncListener);
      return;
    }

    GradleExecutionSettings executionSettings = getOrCreateGradleExecutionSettings(myProject, false);
    Function<ProjectConnection, Void> syncFunction = projectConnection -> {
      // TODO perform sync here.
      return null;
    };

    myHelper.execute(getBaseDirPath(myProject).getPath(), executionSettings, syncFunction);
  }

  // Made 'public' to avoid duplication with ProjectSetUpTask#onFailure.
  // TODO: make 'private' once the new Gradle sync is the default one.
  public void handleSyncFailure(@NotNull String errorMessage, @Nullable GradleSyncListener syncListener) {
    String newMessage = ExternalSystemBundle.message("error.resolve.with.reason", errorMessage);
    Logger.getInstance(GradleSync.class).info(newMessage);

    // Remove cache data to force a sync next time the project is open. This is necessary when checking MD5s is not enough. For example,
    // when sync failed because the SDK being used by the project was accidentally removed in the SDK Manager. The state of the project did
    // not change, and if we don't force a sync, the project will use the cached state and it would look like there are no errors.
    GradleProjectSyncData.removeFrom(myProject);
    GradleSyncState.getInstance(myProject).syncFailed(newMessage);

    if (syncListener != null) {
      syncListener.syncFailed(myProject, newMessage);
    }
  }
}
