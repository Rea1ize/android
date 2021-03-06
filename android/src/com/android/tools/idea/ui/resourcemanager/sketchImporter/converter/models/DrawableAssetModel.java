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
package com.android.tools.idea.ui.resourcemanager.sketchImporter.converter.models;

import com.google.common.collect.ImmutableList;
import java.awt.Rectangle;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * {@link AssetModel} corresponding to a Vector Drawable imported from Sketch into Android Studio.
 */
public class DrawableAssetModel implements AssetModel {
  private final Rectangle.Double myArtboardDimension;
  private final Rectangle.Double myViewportDimension;
  private ImmutableList<ShapeModel> myShapeModels;
  private boolean myExportable;
  private String myName;
  private Origin myOrigin;

  public DrawableAssetModel(@NotNull ImmutableList<ShapeModel> shapeModels,
                            boolean exportable,
                            @NotNull String name,
                            @NotNull Rectangle.Double artboardDimension,
                            @NotNull Rectangle.Double viewportDimension,
                            @NotNull Origin origin) {
    myShapeModels = shapeModels;
    myExportable = exportable;
    myName = name.replaceAll("[ :\\\\/*\"?|<>%.']", "_").toLowerCase(Locale.ENGLISH);  // TODO use a different sanitizer
    myArtboardDimension = artboardDimension;
    myViewportDimension = viewportDimension;
    myOrigin = origin;
  }


  @NotNull
  public ImmutableList<ShapeModel> getShapeModels() {
    return myShapeModels;
  }

  public double getArtboardWidth() {
    return myArtboardDimension.getWidth();
  }

  public double getArtboardHeight() {
    return myArtboardDimension.getHeight();
  }

  public double getViewportWidth() {
    return myViewportDimension.getWidth();
  }

  public double getViewportHeight() {
    return myViewportDimension.getHeight();
  }

  @Override
  public boolean isExportable() {
    return myExportable;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  public void setName(@NotNull String name) {
    myName = name;
  }

  @Override
  @NotNull
  public Origin getOrigin() {
    return myOrigin;
  }
}
