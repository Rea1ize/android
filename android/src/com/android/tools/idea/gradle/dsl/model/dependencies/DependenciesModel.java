/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.gradle.dsl.model.dependencies;

import com.android.tools.idea.gradle.dsl.dependencies.ExternalDependencySpec;
import com.android.tools.idea.gradle.dsl.parser.dependencies.DependenciesDslElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslElementList;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslLiteral;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DependenciesModel {
  @NotNull private DependenciesDslElement myDslElement;

  public DependenciesModel(@NotNull DependenciesDslElement dslElement) {
    myDslElement = dslElement;
  }

  @NotNull
  public List<ArtifactDependencyModel> artifactDependencies(@NotNull String configurationName) {
    List<ArtifactDependencyModel> dependencies = Lists.newArrayList();
    GradleDslElementList list = myDslElement.getProperty(configurationName, GradleDslElementList.class);
    if (list != null) {
      for (GradleDslElement element : list.getElements(GradleDslElement.class)) {
        dependencies.add(ArtifactDependencyModel.create(element));
      }
    }
    return dependencies;
  }

  @NotNull
  public DependenciesModel addArtifactDependency(@NotNull String configurationName, @NotNull String compactNotation) {
    ExternalDependencySpec spec = ExternalDependencySpec.create(compactNotation);
    if (spec == null) {
      throw new IllegalArgumentException("'" + compactNotation + "' is not a valid dependency specification");
    }
    GradleDslElementList list = myDslElement.getProperty(configurationName, GradleDslElementList.class);
    if (list == null) {
      list = new GradleDslElementList(myDslElement, configurationName);
      myDslElement.setNewElement(configurationName, list);
    }
    GradleDslLiteral literal = new GradleDslLiteral(list, configurationName);
    literal.setValue(compactNotation);
    list.addNewElement(literal);
    return this;
  }

  public DependenciesModel remove(@NotNull DependencyModel model) {
    GradleDslElementList gradleDslElementList = myDslElement.getProperty(model.getConfigurationName(), GradleDslElementList.class);
    if (gradleDslElementList != null) {
      gradleDslElementList.removeElement(model.getDslElement());
    }
    return this;
  }
}
