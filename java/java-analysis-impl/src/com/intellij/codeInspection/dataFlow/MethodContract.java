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
package com.intellij.codeInspection.dataFlow;

import com.intellij.codeInspection.dataFlow.value.DfaConstValue;
import com.intellij.codeInspection.dataFlow.value.DfaRelationValue.RelationType;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A method contract which states that method will have a concrete return value
 * if arguments fulfill some constraint.
 *
 * @author Tagir Valeev
 */
public abstract class MethodContract {
  // package private to avoid uncontrolled implementations
  MethodContract() {

  }

  /**
   * @return a value the method will return if the contract conditions fulfill
   */
  public abstract ValueConstraint getReturnValue();

  /**
   * @return true if this contract result does not depend on arguments
   */
  boolean isTrivial() {
    return false;
  }

  abstract String getArgumentsPresentation();

  abstract List<DfaValue> getConditions(DfaValueFactory factory, DfaValue qualifier, DfaValue[] arguments);

  @Override
  public String toString() {
    return getArgumentsPresentation() + " -> " + getReturnValue();
  }

  public enum ValueConstraint {
    ANY_VALUE("_"), NULL_VALUE("null"), NOT_NULL_VALUE("!null"), TRUE_VALUE("true"), FALSE_VALUE("false"), THROW_EXCEPTION("fail");
    private final String myPresentableName;

    ValueConstraint(String presentableName) {
      myPresentableName = presentableName;
    }

    @Nullable
    DfaConstValue getComparisonValue(DfaValueFactory factory) {
      if (this == NULL_VALUE || this == NOT_NULL_VALUE) return factory.getConstFactory().getNull();
      if (this == TRUE_VALUE || this == FALSE_VALUE) return factory.getConstFactory().getTrue();
      return null;
    }

    boolean shouldUseNonEqComparison() {
      return this == NOT_NULL_VALUE || this == FALSE_VALUE;
    }

    /**
     * Returns a condition value which should be applied to memory state to satisfy this constraint
     *
     * @param factory factory to create new values
     * @param argValue argument value to test
     * @return a condition
     */
    public DfaValue getCondition(DfaValueFactory factory, DfaValue argValue) {
      if (this == THROW_EXCEPTION || this == ANY_VALUE) {
        return factory.getBoolean(true);
      }
      DfaConstValue expectedValue = Objects.requireNonNull(getComparisonValue(factory));

      return factory.createCondition(argValue, RelationType.equivalence(!shouldUseNonEqComparison()), expectedValue);
    }

    @Override
    public String toString() {
      return myPresentableName;
    }
  }
}
