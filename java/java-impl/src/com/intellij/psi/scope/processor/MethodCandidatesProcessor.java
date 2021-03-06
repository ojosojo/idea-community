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
package com.intellij.psi.scope.processor;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.JavaResolveUtil;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.infos.MethodCandidateInfo;
import com.intellij.psi.scope.PsiConflictResolver;
import com.intellij.psi.scope.conflictResolvers.DuplicateConflictResolver;
import com.intellij.util.SmartList;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 31.01.2003
 * Time: 19:31:12
 * To change this template use Options | File Templates.
 */
public class MethodCandidatesProcessor extends MethodsProcessor{
  protected boolean myHasAccessibleStaticCorrectCandidate = false;

  public MethodCandidatesProcessor(PsiElement place, PsiConflictResolver[] resolvers, SmartList<CandidateInfo> container){
    super(resolvers, container, place);
  }

  public MethodCandidatesProcessor(PsiElement place){
    super(new PsiConflictResolver[]{DuplicateConflictResolver.INSTANCE}, new SmartList<CandidateInfo>(), place);
  }

  public void add(PsiElement element, PsiSubstitutor substitutor) {
    if (element instanceof PsiMethod){
      final PsiMethod method = (PsiMethod)element;
      addMethod(method, substitutor, isInStaticScope() && !method.hasModifierProperty(PsiModifier.STATIC));
    }
  }

  public void addMethod(final PsiMethod method, final PsiSubstitutor substitutor, final boolean staticProblem) {
    boolean isAccessible = JavaResolveUtil.isAccessible(method, method.getContainingClass(), method.getModifierList(),
                                                        myPlace, myAccessClass, myCurrentFileContext);
    myHasAccessibleStaticCorrectCandidate |= isAccessible && !staticProblem;

    if (isAccepted(method)) {
      add(createCandidateInfo(method, substitutor, staticProblem, isAccessible));
    }
  }

  protected MethodCandidateInfo createCandidateInfo(final PsiMethod method, final PsiSubstitutor substitutor,
                                                    final boolean staticProblem,
                                                    final boolean accessible) {
    return new MethodCandidateInfo(method, substitutor, !accessible, staticProblem, getArgumentList(), myCurrentFileContext,
                                getArgumentList().getExpressionTypes(), getTypeArguments());
  }

  protected boolean isAccepted(final PsiMethod candidate) {
    if (!isConstructor()) {
      return !candidate.isConstructor() && getName(ResolveState.initial()).equals(candidate.getName());
    }
    else {
      if (!candidate.isConstructor()) return false;
      if (myAccessClass == null) return true;
      if (myAccessClass instanceof PsiAnonymousClass) {
        return candidate.getContainingClass().equals(myAccessClass.getSuperClass());
      }
      return myAccessClass.equals(candidate.getContainingClass());
    }
  }

  public CandidateInfo[] getCandidates() {
    final JavaResolveResult[] resolveResult = getResult();
    CandidateInfo[] infos = new CandidateInfo[resolveResult.length];
    System.arraycopy(resolveResult, 0, infos, 0, resolveResult.length);
    return infos;
  }
}
