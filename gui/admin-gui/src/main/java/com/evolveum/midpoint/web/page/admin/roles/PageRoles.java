/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/roles", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_URL,
                label = "PageRoles.auth.roles.label",
                description = "PageRoles.auth.roles.description")})
public class PageRoles extends PageAdminObjectList<RoleType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageRoles.class);
    private static final String DOT_CLASS = PageRoles.class.getName() + ".";

    private static final String OPERATION_SEARCH_MEMBERS = DOT_CLASS + "searchMembers";

    private IModel<Search> searchModel;

    public PageRoles() {
        super();
    }

	private final FocusListInlineMenuHelper<RoleType> listInlineMenuHelper = new FocusListInlineMenuHelper<RoleType>(RoleType.class, this, this){
        private static final long serialVersionUID = 1L;

        protected boolean isShowConfirmationDialog(ColumnMenuAction action){
            return PageRoles.this.isShowConfirmationDialog(action);
        }

        protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
            return PageRoles.this.getConfirmationMessageModel(action, actionName);
        }

    };

    @Override
    protected List<InlineMenuItem> createRowActions() {
        return listInlineMenuHelper.createRowActions();
    }

    @Override
    protected List<IColumn<SelectableBean<RoleType>, String>> initColumns() {
        return ColumnUtils.getDefaultRoleColumns();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, RoleType object) {
        PageRoles.this.roleDetailsPerformed(target, object.getOid());
    }

    @Override
    protected void newObjectActionPerformed(AjaxRequestTarget target) {
        navigateToNext(PageRole.class);
    }

    @Override
    protected Class<RoleType> getType(){
        return RoleType.class;
    }

    private void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageRole.class, parameters);
    }

    private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
    	return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);

    }

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                getObjectListPanel().getSelectedObjectsCount() > 0;
    }
}
