/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection

import com.intellij.JavaTestUtil
import com.intellij.codeInspection.reflectiveAccess.JavaReflectionMemberAccessInspection
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

/**
 * @author Pavel.Dolgov
 */
class JavaReflectionMemberAccessTest : LightCodeInsightFixtureTestCase() {

  private val inspection = JavaReflectionMemberAccessInspection()

  override fun setUp() {
    super.setUp()
    LanguageLevelProjectExtension.getInstance(project).languageLevel = LanguageLevel.JDK_1_5
    myFixture.enableInspections(inspection)
  }

  override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_1_5

  override fun getBasePath() = JavaTestUtil.getRelativeJavaTestDataPath() + "/inspection/javaReflectionMemberAccess"

  fun testFields() = doTest()
  fun testMethods() = doTest()
  fun testConstructors() = doTest()

  fun testFieldExists() = doTest(true)
  fun testMethodExists() = doTest(true)
  fun testConstructorExists() = doTest(true)


  private fun doTest(checkExists: Boolean = false) {
    inspection.checkMemberExistsInNonFinalClasses = checkExists
    myFixture.testHighlighting("${getTestName(false)}.java")
  }
}