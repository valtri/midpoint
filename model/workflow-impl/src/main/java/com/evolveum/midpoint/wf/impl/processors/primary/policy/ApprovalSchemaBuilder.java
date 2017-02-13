/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchema;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ReferenceResolver;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.RelationResolver;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

import static java.util.Comparator.naturalOrder;

/**
 * @author mederly
 */
class ApprovalSchemaBuilder {

	class Result {
		@NotNull final ApprovalSchema schema;
		@NotNull final ApprovalSchemaType schemaType;
		@NotNull final SchemaAttachedPolicyRulesType attachedRules;

		public Result(@NotNull ApprovalSchemaType schemaType, @NotNull ApprovalSchema schema,
				@NotNull SchemaAttachedPolicyRulesType attachedRules) {
			this.schema = schema;
			this.schemaType = schemaType;
			this.attachedRules = attachedRules;
		}
	}

	private class Fragment {
		// object to which relations (approved, owner) are resolved
		// TODO test this thoroughly in presence of non-direct rules and merged schemas
		final PrismObject<?> target;
		@NotNull final ApprovalSchemaType schema;
		final EvaluatedPolicyRule policyRule;
		final ApprovalCompositionStrategyType compositionStrategy;

		private Fragment(ApprovalCompositionStrategyType compositionStrategy, PrismObject<?> target,
				@NotNull ApprovalSchemaType schema, EvaluatedPolicyRule policyRule) {
			this.compositionStrategy = compositionStrategy;
			this.target = target;
			this.schema = schema;
			this.policyRule = policyRule;
		}

		private boolean isMergeableWith(Fragment other) {
			return compositionStrategy != null && BooleanUtils.isTrue(compositionStrategy.isMergeable())
					&& other.compositionStrategy != null && BooleanUtils.isTrue(other.compositionStrategy.isMergeable())
					&& compositionStrategy.getOrder() != null && compositionStrategy.getOrder().equals(other.compositionStrategy.getOrder());
		}
	}

	private final List<Fragment> predefinedFragments = new ArrayList<>();
	private final List<Fragment> standardFragments = new ArrayList<>();

	@NotNull private final BasePrimaryChangeAspect primaryChangeAspect;

	ApprovalSchemaBuilder(@NotNull BasePrimaryChangeAspect primaryChangeAspect) {
		this.primaryChangeAspect = primaryChangeAspect;
	}

	// TODO target
	void add(ApprovalSchemaType schema, ApprovalCompositionStrategyType compositionStrategy, PrismObject<?> defaultTarget,
			EvaluatedPolicyRule policyRule) {
		Fragment fragment = new Fragment(compositionStrategy, defaultTarget, schema, policyRule);
		standardFragments.add(fragment);
	}

	// checks the existence of approvers beforehand, because we don't want to have an empty level
	boolean addPredefined(PrismObject<?> targetObject, @NotNull QName relationName, OperationResult result) {
		RelationResolver resolver = primaryChangeAspect.createRelationResolver(targetObject, result);
		List<ObjectReferenceType> approvers = resolver.getApprovers(Collections.singletonList(relationName));
		if (!approvers.isEmpty()) {
			ApprovalLevelType level = new ApprovalLevelType();
			level.getApproverRef().addAll(approvers);
			addPredefined(targetObject, level);
			return true;
		} else {
			return false;
		}
	}

	void addPredefined(PrismObject<?> targetObject, ApprovalLevelType level) {
		ApprovalSchemaType schema = new ApprovalSchemaType();
		schema.getLevel().add(level);
		addPredefined(targetObject, schema);
	}

	void addPredefined(PrismObject<?> targetObject, ApprovalSchemaType schema) {
		predefinedFragments.add(new Fragment(null, targetObject, schema, null));
	}

	Result buildSchema(ModelInvocationContext ctx, OperationResult result) throws SchemaException {
		sortFragments(predefinedFragments);
		sortFragments(standardFragments);
		List<Fragment> allFragments = new ArrayList<>();
		allFragments.addAll(predefinedFragments);
		allFragments.addAll(standardFragments);

		ApprovalSchemaType schemaType = new ApprovalSchemaType(ctx.prismContext);
		ApprovalSchemaImpl schema = new ApprovalSchemaImpl(ctx.prismContext);
		SchemaAttachedPolicyRulesType attachedRules = new SchemaAttachedPolicyRulesType();

		int i = 0;
		while(i < allFragments.size()) {
			List<Fragment> fragmentMergeGroup = getMergeGroup(allFragments, i);
			processFragmentGroup(fragmentMergeGroup, schemaType, schema, attachedRules, ctx, result);
			i += fragmentMergeGroup.size();
		}

		return new Result(schemaType, schema, attachedRules);
	}

	private List<Fragment> getMergeGroup(List<Fragment> fragments, int i) {
		int j = i+1;
		while (j < fragments.size() && fragments.get(i).isMergeableWith(fragments.get(j))) {
			j++;
		}
		return fragments.subList(i, j);
	}

	private void processFragmentGroup(List<Fragment> fragments, ApprovalSchemaType resultingSchemaType, ApprovalSchemaImpl resultingSchema,
			SchemaAttachedPolicyRulesType attachedRules, ModelInvocationContext ctx, OperationResult result)
			throws SchemaException {
		Fragment firstFragment = fragments.get(0);
		List<ApprovalLevelType> fragmentLevels = cloneAndMergeLevels(fragments);
		if (fragmentLevels.isEmpty()) {
			return;		// probably shouldn't occur
		}
		fragmentLevels.sort(Comparator.comparing(ApprovalLevelType::getOrder, Comparator.nullsLast(naturalOrder())));
		RelationResolver relationResolver = primaryChangeAspect.createRelationResolver(firstFragment.target, result);
		ReferenceResolver referenceResolver = primaryChangeAspect.createReferenceResolver(ctx.modelContext, ctx.taskFromModel, result);
		int from = resultingSchemaType.getLevel().size() + 1;
		int i = from;
		for (ApprovalLevelType level : fragmentLevels) {
			level.setOrder(i++);
			resultingSchemaType.getLevel().add(level);
			resultingSchema.addLevel(level, relationResolver, referenceResolver);
		}
		if (firstFragment.policyRule != null) {
			SchemaAttachedPolicyRuleType attachedRule = new SchemaAttachedPolicyRuleType();
			attachedRule.setLevelMin(from);
			attachedRule.setLevelMax(i - 1);
			attachedRule.setRule(firstFragment.policyRule.toEvaluatedPolicyRuleType());
			attachedRules.getEntry().add(attachedRule);
		}
	}

	private List<ApprovalLevelType> cloneAndMergeLevels(List<Fragment> fragments) throws SchemaException {
		if (fragments.size() == 1) {
			return (List<ApprovalLevelType>) CloneUtil.cloneCollectionMembers(fragments.get(0).schema.getLevel());
		}
		PrismContext prismContext = primaryChangeAspect.getChangeProcessor().getPrismContext();
		ApprovalLevelType resultingLevel = new ApprovalLevelType(prismContext);
		for (Fragment fragment : fragments) {
			mergeLevelFromFragment(resultingLevel, fragment);
		}
		return Collections.singletonList(resultingLevel);
	}

	private void mergeLevelFromFragment(ApprovalLevelType resultingLevel, Fragment fragment) throws SchemaException {
		if (fragment.schema.getLevel().size() != 1) {
			throw new IllegalStateException("Couldn't merge approval schema fragment with level count of not 1: " + fragment.schema);
		}
		ApprovalLevelType levelToMerge = fragment.schema.getLevel().get(0);
		List<QName> overwriteItems = fragment.compositionStrategy.getMergeOverwriting();
		resultingLevel.asPrismContainerValue().mergeContent(levelToMerge.asPrismContainerValue(), overwriteItems);
	}

	private void sortFragments(List<Fragment> fragments) {
		fragments.forEach(f -> {
			if (f.compositionStrategy != null && BooleanUtils.isTrue(f.compositionStrategy.isMergeable())
					&& f.compositionStrategy.getOrder() == null) {
				throw new IllegalStateException("Mergeable composition strategy with no order: "
						+ f.compositionStrategy + " in " + f.policyRule);
			}
		});

		// relying on the fact that the sort algorithm is stable
		fragments.sort((f1, f2) -> {
			ApprovalCompositionStrategyType s1 = f1.compositionStrategy;
			ApprovalCompositionStrategyType s2 = f2.compositionStrategy;
			Integer o1 = s1 != null ? s1.getOrder() : null;
			Integer o2 = s2 != null ? s2.getOrder() : null;
			if (o1 == null || o2 == null) {
				return MiscUtil.compareNullLast(o1, o2);
			}
			// non-mergeable first
			boolean m1 = BooleanUtils.isTrue(s1.isMergeable());
			boolean m2 = BooleanUtils.isTrue(s2.isMergeable());
			if (!m1 && !m2) {
				return 0;
			} else if (m1 && !m2) {
				return 1;
			} else if (!m1) {
				return -1;
			}
			return Comparator.nullsLast(Comparator.<Integer>naturalOrder())
					.compare(s1.getMergeOrder(), s2.getMergeOrder());
		});
	}

}