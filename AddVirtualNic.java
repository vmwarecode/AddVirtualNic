/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.host;

import com.vmware.common.annotations.Action;
import com.vmware.common.annotations.Option;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * <pre>
 * AddVirtualNic
 *
 * This sample is used to add a Virtual Nic to a PortGroup
 *
 * <b>Parameters:</b>
 * url              [required] : url of the web service
 * username         [required] : username for the authentication
 * password         [required] : password for the authentication
 * portgroupname    [required] : Name of the port group
 * ipaddress        [optional] : ipaddress for the nic, if not set DHCP
 *                               will be in affect for the nic
 * hostname         [optional] : Name of the host
 * datacentername   [optional] : Name of the datacenter
 *
 * <b>Command Line:</b>
 * Add VirtualNic to a PortGroup on a Virtual Switch
 * run.bat com.vmware.host.AddVirtualNic --url [webserviceurl]
 * --username [username] --password  [password]
 * --hostname [hostname]  --datacentername [mydatacenter]
 * --portgroupname [myportgroup] --ipaddress [AAA.AAA.AAA.AAA]
 *
 * Add VirtualNic to a PortGroup on a Virtual Switch without hostname
 * run.bat com.vmware.host.AddVirtualNic --url [webserviceurl]
 * --username [username] --password  [password]
 * --datacentername [mydatacenter]
 * --portgroupname [myportgroup] --ipaddress [AAA.AAA.AAA.AAA]
 *
 * Add VirtualNic to a PortGroup on a Virtual Switch without datacentername
 * run.bat com.vmware.host.AddVirtualNic --url [webserviceurl]
 * --username [username] --password  [password]
 * --portgroupname [myportgroup] --ipaddress [AAA.AAA.AAA.AAA]
 * </pre>
 */
@Sample(name = "add-virtual-nic", description = "This sample is used to add a Virtual Nic to a PortGroup")
public class AddVirtualNic extends ConnectedVimServiceBase {
    private ManagedObjectReference rootFolder;
    private ManagedObjectReference propCollectorRef;

    String datacentername;
    String hostname;
    String portgroupname;
    String ipaddress;

    @Option(name = "portgroupname", required = true, description = "Name of the port group")
    public void setPortgroupname(String portgroupname) {
        this.portgroupname = portgroupname;
    }

    @Option(
            name = "ipaddress",
            required = false,
            description = "ipaddress for the nic, if not set DHCP will be in affect for the nic"
    )
    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    @Option(name = "hostname", required = false, description = "Name of the host")
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Option(name = "datacentername", required = false, description = "Name of the datacenter")
    public void setDatacentername(String datacentername) {
        this.datacentername = datacentername;
    }

    void init() {
        propCollectorRef = serviceContent.getPropertyCollector();
        rootFolder = serviceContent.getRootFolder();
    }

    HostVirtualNicSpec createVirtualNicSpecification() {
        HostIpConfig hipconfig = new HostIpConfig();
        if (ipaddress != null && !ipaddress.isEmpty()) {
            hipconfig.setDhcp(Boolean.FALSE);
            hipconfig.setIpAddress(ipaddress);
            hipconfig.setSubnetMask("255.255.255.0");
        } else {
            hipconfig.setDhcp(Boolean.TRUE);
        }
        HostVirtualNicSpec hvnicspec = new HostVirtualNicSpec();
        hvnicspec.setIp(hipconfig);
        return hvnicspec;
    }

    void addVirtualNIC() throws HostConfigFaultFaultMsg, AlreadyExistsFaultMsg, InvalidStateFaultMsg, InvalidPropertyFaultMsg, InvocationTargetException, NoSuchMethodException, IllegalAccessException, RuntimeFaultFaultMsg {
        ManagedObjectReference dcmor;
        ManagedObjectReference hostfoldermor;
        ManagedObjectReference hostmor = null;

        if (((datacentername != null) && (hostname != null))
                || ((datacentername != null) && (hostname == null))) {
            Map<String, ManagedObjectReference> dcResults = getMOREFs.inFolderByType(serviceContent
                    .getRootFolder(), "Datacenter", new RetrieveOptions());
            dcmor = dcResults.get(datacentername);
            if (dcmor == null) {
                System.out.println("Datacenter not found");
                return;
            }
            hostfoldermor = (ManagedObjectReference) getMOREFs.entityProps(dcmor,
                    new String[] { "hostFolder" }).get("hostFolder");
            Map<String, ManagedObjectReference> hostResults = getMOREFs.inFolderByType(
                    hostfoldermor, "HostSystem", new RetrieveOptions());
            hostmor = hostResults.get(hostname);

        } else if ((datacentername == null) && (hostname != null)) {
            Map<String, ManagedObjectReference> hostResults = getMOREFs.inFolderByType(
                    serviceContent.getRootFolder(), "HostSystem", new RetrieveOptions());
            hostmor = hostResults.get(hostname);

        }
        if (hostmor != null) {
            HostConfigManager configMgr = (HostConfigManager) getMOREFs.entityProps(hostmor,
                    new String[] { "configManager" }).get("configManager");
            ManagedObjectReference nwSystem = configMgr.getNetworkSystem();
            HostPortGroupSpec portgrp = new HostPortGroupSpec();
            portgrp.setName(portgroupname);

            HostVirtualNicSpec vNicSpec = createVirtualNicSpecification();
            String nic = vimPort.addVirtualNic(nwSystem, portgroupname, vNicSpec);

            System.out.println("Successful in creating nic : " + nic
                    + " with PortGroup :" + portgroupname);
        } else {
            System.out.println("Host not found");
        }
    }

    @Action
    public void run() throws RuntimeFaultFaultMsg, AlreadyExistsFaultMsg, InvalidStateFaultMsg, InvocationTargetException, InvalidPropertyFaultMsg, NoSuchMethodException, IllegalAccessException, HostConfigFaultFaultMsg {
        init();
        addVirtualNIC();
    }
}
