/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.api.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractObjectTypeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationRoleManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardLayoutType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FeedbackMessagesHookType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiExportSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceFeatureType;

/**
 * Compiled user profile. This class contains information about configuration and customization
 * of individual parts of user interface and user preferences. This class contains pre-processed
 * information in a form that is suitable to direct use by user interface code. The GUI should not
 * be required to do any complex processing on this.
 * 
 * This idea is to compile the profile just once, on login time. Therefore only the authentication
 * code (UserProfileCompiler) should modify this object. It should be considered read-only for all other
 * purposes.
 * 
 * Later it may be split to interface and implementation parts.
 * 
 * @since 4.0
 * @author Radovan Semancik
 */
@Experimental
public class CompiledUserProfile implements DebugDumpable, Serializable {
	private static final long serialVersionUID = 1L;

	private String defaultTimezone;
	private String preferredDataLanguage;
	private Boolean enableExperimentalFeatures;
	private List<RichHyperlinkType> additionalMenuLink = new ArrayList<>();
	private List<RichHyperlinkType> userDashboardLink = new ArrayList<>();
	private List<CompiledObjectCollectionView> objectCollectionViews = new ArrayList<>();
	private CompiledObjectCollectionView defaultObjectCollectionView = null;
	private DashboardLayoutType userDashboard;
	private GuiExportSettingsType defaultExportSettings;
	private ObjectFormsType objectForms;
	private GuiObjectDetailsSetType objectDetails;
	private FeedbackMessagesHookType feedbackMessagesHook;
	private AdminGuiConfigurationRoleManagementType roleManagement;
	private List<UserInterfaceFeatureType> features = new ArrayList<>();
	
	public String getDefaultTimezone() {
		return defaultTimezone;
	}
	
	public void setDefaultTimezone(String defaultTimezone) {
		this.defaultTimezone = defaultTimezone;
	}

	public String getPreferredDataLanguage() {
		return preferredDataLanguage;
	}

	public void setPreferredDataLanguage(String preferredDataLanguage) {
		this.preferredDataLanguage = preferredDataLanguage;
	}

	public Boolean isEnableExperimentalFeatures() {
		return enableExperimentalFeatures;
	}

	public void setEnableExperimentalFeatures(Boolean enableExperimentalFeatures) {
		this.enableExperimentalFeatures = enableExperimentalFeatures;
	}

	@NotNull
	public List<RichHyperlinkType> getAdditionalMenuLink() {
		return additionalMenuLink;
	}
	
	/**
	 * Very likely to change in the future (for "flexible dashboards" feature). 
	 */
	@Experimental
	@NotNull
	public List<RichHyperlinkType> getUserDashboardLink() {
		return userDashboardLink;
	}

	/**
	 * Very likely to change in the future (for "flexible dashboards" feature). 
	 */
	@Experimental
	public DashboardLayoutType getUserDashboard() {
		return userDashboard;
	}
	
	@Experimental
	public void setUserDashboard(DashboardLayoutType userDashboard) {
		this.userDashboard = userDashboard;
	}

	/**
	 * Compiled information about all configured object list views.
	 */
	@NotNull
	public List<CompiledObjectCollectionView> getObjectCollectionViews() {
		return objectCollectionViews;
	}
	
	/**
	 * Compiled information about object list view for a particular type.
	 * If viewName is null then it returns view definition for "all objects" view,
	 * e.g. "all users", "all roles".
	 */
	public CompiledObjectCollectionView findObjectCollectionView(@NotNull QName objectType, String viewName) {
		for (CompiledObjectCollectionView objectCollectionView : objectCollectionViews) {
			if (objectCollectionView.match(objectType, viewName)) {
				return objectCollectionView;
			}
		}
		return defaultObjectCollectionView;
	}
	
	/**
	 * Find all views that are applicable for a particular object type. Returns views for all collections
	 * and archetypes that are applicable for that type. Ideal to be used in constructing menus.
	 */
	@NotNull
	public List<CompiledObjectCollectionView> findAllApplicableObjectCollectionViews(@NotNull QName objectType) {
		List<CompiledObjectCollectionView> applicableViews = new ArrayList<>();
		for (CompiledObjectCollectionView objectCollectionView : objectCollectionViews) {
			if (objectCollectionView.match(objectType)) {
				applicableViews.add(objectCollectionView);
			}
		}
		return applicableViews;
	}

	/**
	 * Find all views that are applicable for a particular object type. Returns views for all collections
	 * and archetypes that are applicable for that type. Ideal to be used in costructing menus.
	 */
	@NotNull
	public <O extends ObjectType> List<CompiledObjectCollectionView> findAllApplicableObjectCollectionViews(Class<O> compileTimeClass) {
		return findAllApplicableObjectCollectionViews(ObjectTypes.getObjectType(compileTimeClass).getTypeQName());
	}

	/**
	 * Default list view setting should never be needed publicly. Always check setting for specific
	 * object type (and archetype).
	 */
	public CompiledObjectCollectionView getDefaultObjectCollectionView() {
		return defaultObjectCollectionView;
	}

	public void setDefaultObjectCollectionView(CompiledObjectCollectionView defaultObjectCollectionView) {
		this.defaultObjectCollectionView = defaultObjectCollectionView;
	}

	public GuiExportSettingsType getDefaultExportSettings() {
		return defaultExportSettings;
	}

	public void setDefaultExportSettings(GuiExportSettingsType defaultExportSettings) {
		this.defaultExportSettings = defaultExportSettings;
	}
	
	/**
	 * May change in the future.
	 */
	@Experimental
	public ObjectFormsType getObjectForms() {
		return objectForms;
	}

	@Experimental
	public void setObjectForms(ObjectFormsType objectForms) {
		this.objectForms = objectForms;
	}
	
	/**
	 * May change in the future.
	 */
	@Experimental
	public GuiObjectDetailsSetType getObjectDetails() {
		return objectDetails;
	}

	@Experimental
	public void setObjectDetails(GuiObjectDetailsSetType objectDetails) {
		this.objectDetails = objectDetails;
	}
	

	public <O extends ObjectType> GuiObjectDetailsPageType findObjectDetailsConfiguration(Class<O> compileTimeClass) {
		if (objectDetails == null) {
			return null;
		}
		return findObjectConfiguration(objectDetails.getObjectDetailsPage(), compileTimeClass);
	}

	private <T extends AbstractObjectTypeConfigurationType, O extends ObjectType> T findObjectConfiguration(
			List<T> list, Class<O> type) {
		if (list == null) {
			return null;
		}
		QName typeQName = ObjectTypes.getObjectType(type).getTypeQName();
		for (T item: list) {
			if (QNameUtil.match(item.getType(), typeQName)) {
				return item;
			}
		}
		for (T item: list) {
			if (item.getType() == null) {
				return item;
			}
		}
		return null;
	}

	
	public FeedbackMessagesHookType getFeedbackMessagesHook() {
		return feedbackMessagesHook;
	}

	public void setFeedbackMessagesHook(FeedbackMessagesHookType feedbackMessagesHook) {
		this.feedbackMessagesHook = feedbackMessagesHook;
	}

	public AdminGuiConfigurationRoleManagementType getRoleManagement() {
		return roleManagement;
	}

	public void setRoleManagement(AdminGuiConfigurationRoleManagementType roleManagement) {
		this.roleManagement = roleManagement;
	}

	public List<UserInterfaceFeatureType> getFeatures() {
		return features;
	}
	
	public UserInterfaceFeatureType findFeature(String identifier) {
		return findFeature(features, identifier); 
	}
	
	public static <T extends UserInterfaceFeatureType> T findFeature(List<T> features, String identifier) {
		for (T feature: features) {
			if (feature.getIdentifier().equals(identifier)) {
				return feature;
			}
		}
		return null;
	}
	
	public UserInterfaceElementVisibilityType getFeatureVisibility(String identifier) {
		UserInterfaceFeatureType feature = findFeature(identifier);
		if (feature == null) {
			return UserInterfaceElementVisibilityType.AUTOMATIC;
		}
		UserInterfaceElementVisibilityType visibility = feature.getVisibility();
		if (visibility == null) {
			return UserInterfaceElementVisibilityType.AUTOMATIC;
		}
		return visibility;
	}
	
	public boolean isFeatureVisible(String identifier) {
		return isFeatureVisible(identifier, null);
	}

	public boolean isFeatureVisible(String identifier, BooleanSupplier automaticPredicate) {
		UserInterfaceElementVisibilityType visibility = getFeatureVisibility(identifier);
		return isVisible(visibility, automaticPredicate);
	}
	
	public static boolean isVisible(UserInterfaceElementVisibilityType visibility, BooleanSupplier automaticPredicate) {
		if (visibility == UserInterfaceElementVisibilityType.HIDDEN) {
			return false;
		}
		if (visibility == UserInterfaceElementVisibilityType.VISIBLE) {
			return true;
		}
		if (visibility == UserInterfaceElementVisibilityType.AUTOMATIC) {
			if (automaticPredicate == null) {
				return true;
			} else {
				return automaticPredicate.getAsBoolean();
			}
		}
		return false;
	}
	
	@Experimental
	public DashboardWidgetType findUserDashboardWidget(String widgetIdentifier) {
		if (userDashboard == null) {
			return null;
		}
		return findFeature(userDashboard.getWidget(), widgetIdentifier);
	}
	
	// TODO: later: information about menu structure

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CompiledUserProfile.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "defaultTimezone", defaultTimezone, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "preferredDataLanguage", preferredDataLanguage, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "enableExperimentalFeatures", enableExperimentalFeatures, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "additionalMenuLink", additionalMenuLink, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "userDashboardLink", userDashboardLink, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "objectCollectionViews", objectCollectionViews, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "defaultObjectCollectionView", defaultObjectCollectionView, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "userDashboard", userDashboard, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "defaultExportSettings", defaultExportSettings, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "objectForms", objectForms, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "objectDetails", objectDetails, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "feedbackMessagesHook", feedbackMessagesHook, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "roleManagement", roleManagement, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "features", features, indent + 1);
		return sb.toString();
	}
}
