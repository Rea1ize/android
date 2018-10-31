/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.profilers.cpu.simpleperf;

import com.android.tools.profiler.protobuf3jarjar.ByteString;
import com.android.tools.profilers.cpu.TracePreProcessor;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SimpleperfSampleReporter implements TracePreProcessor {

  @VisibleForTesting
  static final ByteString FAILURE = ByteString.copyFromUtf8("Failure");

  private final String myHomePath;

  /**
   * Directory where the .so files are located. It's passed to simpleperf report-sample command following the --symdir flag.
   * Although this {@link Supplier} can't be null, it can return null and in this case we don't pass the --symdir flag at all.
   */
  @NotNull
  private Supplier<String> mySymbolsDir;

  public SimpleperfSampleReporter(@NotNull Supplier<String> symbolsDir) {
    this(PathManager.getHomePath(), symbolsDir);
  }

  @VisibleForTesting
  SimpleperfSampleReporter(@NotNull String homePath, @NotNull Supplier<String> symbolsDir) {
    myHomePath = homePath;
    mySymbolsDir = symbolsDir;
  }

  private static Logger getLogger() {
    return Logger.getInstance(SimpleperfSampleReporter.class);
  }

  /**
   * Receives a raw trace generated by running `simpleperf record` on an Android process, invokes `simpleperf report-sample`, and return
   * the output if conversion is made successfully. If there is a failure while converting the raw trace to the format supported by
   * {@link SimpleperfTraceParser}, return {@link #FAILURE}.
   */
  @Override
  public ByteString preProcessTrace(@NotNull ByteString trace) {
    try {
      File processedTraceFile = FileUtil.createTempFile(
        String.format("%s%ctrace-%d", FileUtil.getTempDirectory(), File.separatorChar, System.currentTimeMillis()), ".trace", true);

      String cmd = getReportSampleCommand(trace, processedTraceFile);
      Process reportSample = Runtime.getRuntime().exec(cmd);
      reportSample.waitFor();

      boolean reportSampleSuccess = reportSample.exitValue() == 0;
      if (!reportSampleSuccess) {
        getLogger().warn("simpleperf report-sample exited unsuccessfully.");
        return FAILURE;
      }

      ByteString processedTrace = ByteString.copyFrom(Files.readAllBytes(processedTraceFile.toPath()));
      processedTraceFile.delete();
      return processedTrace;
    }
    catch (IOException e) {
      getLogger().warn(String.format("I/O error when trying to execute simpleperf report-sample:\n%s", e.getMessage()));
      return FAILURE;
    }
    catch (InterruptedException e) {
      getLogger().warn(String.format("Failed to wait for simpleperf report-sample command to run:\n%s", e.getMessage()));
      return FAILURE;
    }
  }

  @VisibleForTesting
  String getReportSampleCommand(@NotNull ByteString trace, @NotNull File processedTrace) throws IOException {
    String symbolsDir = mySymbolsDir.get();
    String symDirFlag = symbolsDir == null ? "" : "--symdir " + symbolsDir;
    return String.format("%s report-sample --protobuf --show-callchain -i %s -o %s %s",
                         getSimpleperfBinaryPath(), tempFileFromByteString(trace).getAbsolutePath(),
                         processedTrace.getAbsolutePath(), symDirFlag);
  }

  @VisibleForTesting
  String getSimpleperfBinaryPath() {
    // First, try the release path. For instance:
    // $IDEA_PATH/plugins/android/resources/simpleperf/darwin-x86_64/simpleperf
    Path path =
      Paths.get(myHomePath, "plugins", "android", "resources", "simpleperf", getSimpleperfBinarySubdirectory(), getSimpleperfBinaryName());
    if (Files.notExists(path)) {
      // If the release path doesn't exist, it means we're building from sources, so use the prebuilts path. For example:
      // prebuilts/tools/windows/simpleperf/simpleperf.exe.
      // Note: prebuilts directory is $IDEA_PATH/../../prebuilts
      path =
        Paths.get(myHomePath, "..", "..", "prebuilts", "tools", getSimpleperfBinarySubdirectory(), "simpleperf", getSimpleperfBinaryName());
    }
    return path.toString();
  }

  private static File tempFileFromByteString(@NotNull ByteString bytes) throws IOException {
    File file = FileUtil.createTempFile(String.format("cpu_trace_%d", System.currentTimeMillis()), ".trace", true);
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(bytes.toByteArray());
    }
    return file;
  }

  private static String getSimpleperfBinarySubdirectory() {
    String os;
    if (SystemInfo.isLinux) {
      os = "linux-x86";
    }
    else if (SystemInfo.isMac) {
      os = "darwin-x86";
    }
    else if (SystemInfo.isWindows) {
      os = "windows";
    }
    else {
      throw new IllegalStateException("Unknown operating system");
    }

    String suffix64bit = SystemInfo.is64Bit ? (SystemInfo.isWindows ? "-x86_64" : "_64") : "";
    return String.format("%s%s", os, suffix64bit);
  }

  private static String getSimpleperfBinaryName() {
    return SystemInfo.isWindows ? "simpleperf.exe" : "simpleperf";
  }
}
