/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author lazyman
 */
public class InitialDataImport {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);

    private static final String DOT_CLASS = InitialDataImport.class.getName() + ".";
    private static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "initialObjectsImport";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    private static final String OBJECTS_FILE = "objects.xml";

    @Autowired(required = true)
    private transient PrismContext prismContext;
    private ModelService model;
    private TaskManager taskManager;

    public void setModel(ModelService model) {
        Validate.notNull(model, "Model service must not be null.");
        this.model = model;
    }

    public void setTaskManager(TaskManager taskManager) {
        Validate.notNull(taskManager, "Task manager must not be null.");
        this.taskManager = taskManager;
    }

    public void init() {
        LOGGER.info("Starting initial object import.");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);

        try {
            PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();

            List<PrismObject<? extends Objectable>> objects = domProcessor.parseObjects(getResource(OBJECTS_FILE));
            for (PrismObject object : objects) {
                OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);

                boolean importObject = true;
                try {
                    model.getObject(object.getCompileTimeClass(), object.getOid(), null, task, result);
                    importObject = false;
                    result.recordSuccess();
                } catch (ObjectNotFoundException ex) {
                    importObject = true;
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't get object with oid {} from model", ex,
                            object.getOid());
                    result.recordWarning("Couldn't get object with oid '" + object.getOid() + "' from model",
                            ex);
                }

                if (!importObject) {
                    continue;
                }

                ObjectDelta delta = ObjectDelta.createAddDelta(object);
                model.executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
                result.recordSuccess();
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't import file {}", ex, OBJECTS_FILE);
            mainResult.recordFatalError("Couldn't import file '" + OBJECTS_FILE + "'", ex);
        }

        mainResult.recomputeStatus("Couldn't import objects.");
        LOGGER.info("Initial object import finished.");
        LOGGER.info("Initialization status:\n" + mainResult.dump());
    }

    private File getResource(String name) {
        URI path;
        try {
            path = InitialDataImport.class.getClassLoader().getResource(name).toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("parameter name = " + name, e);
        }
        return new File(path);
    }
}
