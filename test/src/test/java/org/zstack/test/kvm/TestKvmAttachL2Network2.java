package org.zstack.test.kvm;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.identity.SessionInventory;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestKvmAttachL2Network2 {
    static CLogger logger = Utils.getLogger(TestKvmAttachL2Network2.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static CloudBus bus;
    static DatabaseFacade dbf;
    static KVMHostFactory kvmFactory;
    static SessionInventory session;
    static KVMSimulatorConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmAttachL2Network2.xml", con);
        deployer.addSpringConfig("Kvm.xml");
        deployer.addSpringConfig("KVMSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        kvmFactory = loader.getComponent(KVMHostFactory.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    
    @Test
    public void test() throws ApiSenderException {
        ClusterInventory cinv = deployer.clusters.get("TestCluster");
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("KVM-1");
        msg.setClusterUuid(cinv.getUuid());
        msg.setManagementIp("localhost");
        msg.setUsername("admin");
        msg.setPassword("password");
        msg.setSession(session);
        ApiSender sender = api.getApiSender();
        sender.send(msg, APIAddHostEvent.class);
    }
}
