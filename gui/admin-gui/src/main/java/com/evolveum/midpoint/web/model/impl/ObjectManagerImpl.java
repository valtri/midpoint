/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.dto.ObjectDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class ObjectManagerImpl<C extends ObjectType, T extends ObjectDto<C>> implements
        ObjectManager<T>, Serializable {

    private static final long serialVersionUID = -7853884441389039036L;
    private static final Trace LOGGER = TraceManager.getTrace(ObjectManagerImpl.class);
    @Autowired(required = true)
    private transient ModelService model;
    @Autowired(required = true)
    private transient TaskManager taskManager;
    @Autowired(required = true)
    private PrismContext prismContext;

    protected ModelService getModel() {
        return model;
    }

    protected TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public Collection<T> list() {
        return list(null);
    }

    protected <O extends ObjectType> PrismObject<O> get(Class<O> objectClass, String oid,
            Collection<PropertyPath> resolve) throws ObjectNotFoundException {
        Validate.notEmpty(oid, "Object oid must not be null or empty.");
        Validate.notNull(objectClass, "Object class must not be null.");

        LOGGER.debug("Get object with oid {}.", new Object[]{oid});
        OperationResult result = new OperationResult(GET);

        PrismObject<O> objectType = null;
        try {
            objectType = getModel().getObject(objectClass, oid, resolve, null, result);
            result.recordSuccess();
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (SystemException ex) {
            throw ex;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get object {} from model", ex, oid);
            result.recordFatalError(ex);
            throw new SystemException("Couldn't get object with oid '" + oid + "'.", ex);
        } finally {
            result.computeStatus();
        }

        ControllerUtil.printResults(LOGGER, result, null);

        return objectType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(String oid, Collection<PropertyPath> resolve) {
        Validate.notEmpty(oid, "Object oid must not be null or empty.");
        LOGGER.debug("Get object with oid {}.", new Object[]{oid});
        OperationResult result = new OperationResult(GET);

        T object = null;
        try {
            PrismObject prismObject = get(getSupportedObjectClass(), oid, resolve);
            Objectable objectType = prismObject.asObjectable();
            isObjectTypeSupported(objectType);
            object = createObject((C) objectType);
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get object {} from model", ex, oid);
            result.recordFatalError(ex);
        } finally {
            result.computeStatus();
        }

        ControllerUtil.printResults(LOGGER, result, null);

        return object;
    }

    @Override
    public void delete(String oid) {
        Validate.notEmpty(oid, "Object oid must not be null or empty.");
        LOGGER.debug("Deleting object '" + oid + "'.");

        Task task = taskManager.createTaskInstance(DELETE);
        OperationResult result = task.getResult();
        try {
            getModel().deleteObject(getSupportedObjectClass(), oid, task, result);
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete object {} from model", ex, oid);
            result.recordFatalError(ex);
        } finally {
            result.computeStatus();
        }

        ControllerUtil.printResults(LOGGER, result, null);
    }

    @Override
    public String add(T object) {
        Validate.notNull(object, "Object must not be null.");
        Validate.notNull(object.getXmlObject(), "Xml object type in object must not be null.");
        LOGGER.debug("Adding object '" + object.getName() + "'.");

        OperationResult result = new OperationResult(ADD);
        Task task = taskManager.createTaskInstance();
        // TODO: task initialization
        String oid = null;
        try {
            SecurityUtils security = new SecurityUtils();
            PrincipalUser principal = security.getPrincipalUser();
            task.setOwner(principal.getUser().asPrismObject());
            oid = getModel().addObject(object.getXmlObject().asPrismObject(), task, result);
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't add object {} to model", ex, object.getName());
            result.recordFatalError(ex);
        } finally {
            result.computeStatus();
        }

        ControllerUtil.printResults(LOGGER, result, null);

        return oid;
    }

    protected <O extends ObjectType> Collection<O> list(PagingType paging, Class<O> type) {
        Validate.notNull(type, "Class object must not be null.");

        Collection<O> collection = new ArrayList<O>();
        OperationResult result = new OperationResult(LIST);
        try {
            List<PrismObject<O>> objectList = getModel().listObjects(type, paging, null, result);
            LOGGER.debug("Found {} objects of type {}.", new Object[]{objectList.size(), type});
            for (PrismObject<O> object : objectList) {
                collection.add(object.asObjectable());
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't list {} objects from model", ex, type);
            result.recordFatalError(ex);
        } finally {
            result.computeStatus();
        }

        ControllerUtil.printResults(LOGGER, result, null);
        return collection;
    }

    @SuppressWarnings("unchecked")
    protected Collection<T> list(PagingType paging, ObjectTypes type) {
        Validate.notNull(type, "Object type must not be null.");
        LOGGER.debug("Listing '" + type.getObjectTypeUri() + "' objects.");

        Collection<T> collection = new ArrayList<T>();
        Collection<ObjectType> objects = (Collection<ObjectType>) list(paging, type.getClassDefinition());
        for (ObjectType objectType : objects) {
            isObjectTypeSupported(objectType);
            collection.add(createObject((C) objectType));
        }

        return collection;
    }

    @Override
    public T create() {
        return createObject(null);
    }

    private void isObjectTypeSupported(Objectable object) {
        isObjectTypeSupported((ObjectType) object);
    }

    private void isObjectTypeSupported(ObjectType object) {
        Class<? extends ObjectType> type = getSupportedObjectClass();
        Validate.notNull(type, "Supported object class must not be null.");

        if (!type.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Object type '" + object.getClass()
                    + "' is not supported, supported class is '" + type + "'.");
        }
    }

    protected abstract Class<? extends ObjectType> getSupportedObjectClass();

    protected abstract T createObject(C objectType);
}
