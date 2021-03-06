/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.registry.servlet.internal;

import org.apache.abdera.protocol.server.servlet.AbderaServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.wso2.carbon.registry.app.ResourceServlet;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.servlet.UDDIServlet;
import org.wso2.carbon.utils.CarbonUtils;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
         name = "org.wso2.carbon.registry.servlet", 
         immediate = true)
public class RegistryAtomServiceComponent {

    private static Log log = LogFactory.getLog(RegistryAtomServiceComponent.class);

    private RegistryService registryService = null;

    private HttpService httpService = null;

    private UDDIServlet juddiRegistryServlet = null;

    private boolean juddiRegistryServletRegistered = false;

    private HttpContext defaultHttpContext = null;

    @Activate
    protected void activate(ComponentContext context) {
        try {
            registerServlet();
            log.debug("******* Registry APP bundle is activated ******* ");
        } catch (Throwable e) {
            log.error("******* Failed to activate Registry APP bundle ******* ", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        httpService.unregister("/registry/atom");
        httpService.unregister("/registry/tags");
        httpService.unregister("/registry/resource");
        if (juddiRegistryServletRegistered) {
            httpService.unregister("/juddiv3");
            juddiRegistryServletRegistered = false;
        }
        log.debug("******* Registry APP bundle is deactivated ******* ");
    }

    private void registerServlet() throws Exception {
        if (registryService == null) {
            String msg = "Unable to Register Servlet. Registry Service Not Found.";
            log.error(msg);
            throw new Exception(msg);
        }
        if (!CarbonUtils.isRemoteRegistry()) {
            Dictionary servletParam = new Hashtable(2);
            servletParam.put("org.apache.abdera.protocol.server.Provider", "org.wso2.carbon.registry.app.RegistryProvider");
            httpService.registerServlet("/registry/atom", new AbderaServlet(), servletParam, defaultHttpContext);
            httpService.registerServlet("/registry/tags", new AbderaServlet(), servletParam, defaultHttpContext);
        }
        registerJUDDIServlet();
        httpService.registerServlet("/registry/resource", new ResourceServlet(), null, defaultHttpContext);
    }

    private void registerJUDDIServlet() {
        if (juddiRegistryServlet != null && httpService != null) {
            try {
                httpService.registerServlet("/juddiv3", juddiRegistryServlet, null, defaultHttpContext);
            } catch (Exception e) {
                log.error("Unable to register jUDDI servlet", e);
            }
            juddiRegistryServletRegistered = true;
        }
    }

    @Reference(
             name = "registry.service", 
             service = org.wso2.carbon.registry.core.service.RegistryService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {
        this.registryService = null;
    }

    @Reference(
             name = "http.service", 
             service = org.osgi.service.http.HttpService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetHttpService")
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
        this.defaultHttpContext = httpService.createDefaultHttpContext();
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    @Reference(
             name = "registry.uddi", 
             service = org.wso2.carbon.registry.core.servlet.UDDIServlet.class, 
             cardinality = ReferenceCardinality.OPTIONAL, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetJUDDIRegistryServlet")
    protected void setJUDDIRegistryServlet(UDDIServlet juddiRegistryServlet) {
        this.juddiRegistryServlet = juddiRegistryServlet;
        registerJUDDIServlet();
    }

    protected void unsetJUDDIRegistryServlet(UDDIServlet juddiRegistryServlet) {
        this.juddiRegistryServlet = null;
    }
}

