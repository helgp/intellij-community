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
package com.intellij.codeInspection.inheritance;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class ImplementedAtRuntimeConditionImpl extends ImplementedAtRuntimeCondition {
  @Override
  public boolean isImplementedAtRuntime(@NotNull PsiClass psiClass) {
    for (ImplicitSubclassProvider provider : ImplicitSubclassProvider.Companion.getEP_NAME().getExtensions()) {
      SubclassingInfo info = provider.getSubclassingInfo(psiClass);
      if (info != null && !info.isAbstract()) {
        return true;
      }
    }
    return false;
  }
}
